package com.meta4projects.fldfloatingdictionary.others

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager2.widget.ViewPager2
import com.meta4projects.fldfloatingdictionary.R
import com.meta4projects.fldfloatingdictionary.adapters.EntryAdapter
import com.meta4projects.fldfloatingdictionary.room.database.DictionaryDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Window(private val context: Context) : OnTouchListener {

    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private lateinit var entryWords: ArrayList<String>
    private lateinit var viewPager: ViewPager2
    private lateinit var rootView: View
    private var entryAdapter: EntryAdapter? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var dragView: ImageView? = null
    private lateinit var layoutContainer: ConstraintLayout
    private lateinit var editText: AutoCompleteTextView
    private var wasInFocus = true
    private var pointerStartX = 0
    private var pointerStartY = 0
    private var initialX = 0
    private var initialY = 0

    init {
        setEntries()
    }

    private fun setEntries() {
        CoroutineScope(Dispatchers.IO + Util.coroutineExceptionHandler).launch {
            entryWords = DictionaryDatabase.getINSTANCE(context).entryWordsDao().getRandomWords() as ArrayList<String>
            withContext(Dispatchers.Main + Util.coroutineExceptionHandler) {
                setLayoutParams()
                initialiseWindow()
            }
        }
    }

    private fun setLayoutParams() {
        layoutParams = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT)
        layoutParams!!.gravity = Gravity.TOP or Gravity.END
        layoutParams!!.height = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams!!.width = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams!!.x = 0
        layoutParams!!.y = 100
    }

    @SuppressLint("InflateParams")
    private fun initialiseWindow() {
        val layoutInflater = LayoutInflater.from(context)
        rootView = if (Util.isNightMode) layoutInflater.inflate(R.layout.layout_floating_dictionary_night, null) else layoutInflater.inflate(R.layout.layout_floating_dictionary, null)
        dragView = rootView.findViewById(R.id.drag_view)
        layoutContainer = rootView.findViewById(R.id.container_layout)
        rootView.setOnTouchListener(this)
        val display = windowManager.defaultDisplay
        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)
        val deviceWidth = displayMetrics.widthPixels
        val deviceHeight = displayMetrics.heightPixels
        val height = (deviceHeight * 0.5).toInt()
        val layoutParamsContainer = layoutContainer.layoutParams
        layoutParamsContainer.width = deviceWidth
        layoutParamsContainer.height = height
        layoutContainer.layoutParams = layoutParamsContainer
        editText = rootView.findViewById(R.id.search_input_view)
        val adapter = ArrayAdapter<String?>(context, if (Util.isNightMode) R.layout.layout_suggestion_night else R.layout.layout_suggestion)
        editText.setAdapter(adapter)
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                replaceSearchEntryWords(s.toString().trim(), adapter)
            }

            override fun afterTextChanged(s: Editable) {}
        })
        editText.setOnClickListener { v: View? ->
            layoutParams!!.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            layoutParams!!.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            update()
            wasInFocus = true
            Util.showSoftKeyboard(context, v)
        }
        editText.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = editText.text.toString().trim()
                if (query.isNotEmpty()) {
                    setWord(query)
                    editText.setText(query)
                    editText.dismissDropDown()
                    Util.hideKeyboard(context, editText)
                    return@setOnEditorActionListener true
                }
            }
            false
        }
        editText.onItemClickListener = OnItemClickListener { parent: AdapterView<*>, _: View?, position: Int, _: Long ->
            val word = parent.getItemAtPosition(position).toString()
            setWord(word)
            Util.hideKeyboard(context, editText)
        }
        val speaker = rootView.findViewById<ImageView>(R.id.speaker)
        speaker.setOnClickListener {
            val word = entryWords[viewPager.currentItem]
            Util.textToSpeech?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "speech_id")
        }
        setViewPager()
        open()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setViewPager() {
        viewPager = rootView.findViewById(R.id.viewpager)
        entryAdapter = EntryAdapter(entryWords, context)
        viewPager.adapter = entryAdapter
        entryAdapter!!.setLinkClickListener { word: String? -> setWord(word) }
        entryAdapter!!.notifyDataSetChanged()
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val firstPosition = 0
                val lastPosition = entryWords.size - 1
                if (position == firstPosition || position == lastPosition) {
                    val currentWord = entryWords[viewPager.currentItem]

                    CoroutineScope(Dispatchers.IO + Util.coroutineExceptionHandler).launch {
                        val currentWords = DictionaryDatabase.getINSTANCE(context).entryWordsDao().getEntryWords(currentWord)
                        withContext(Dispatchers.Main + Util.coroutineExceptionHandler) {
                            entryWords.clear()
                            entryWords.addAll(currentWords)
                            viewPager.setCurrentItem(entryWords.indexOf(currentWord), false)
                            entryAdapter!!.notifyDataSetChanged()
                        }
                    }
                }
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setWord(word: String?) {
        CoroutineScope(Dispatchers.IO + Util.coroutineExceptionHandler).launch {
            val words = DictionaryDatabase.getINSTANCE(context).entryWordsDao().getEntryWords(word!!)
            withContext(Dispatchers.Main + Util.coroutineExceptionHandler) {
                if (words.isEmpty()) Util.showToast("could not find such word!", context) else {
                    entryWords.clear()
                    entryWords.addAll(words)
                    viewPager.setCurrentItem(entryWords.indexOf(word), true)
                    entryAdapter!!.notifyDataSetChanged()
                }
            }
        }
    }

    private fun replaceSearchEntryWords(word: String, adapter: ArrayAdapter<String?>) {
        CoroutineScope(Dispatchers.IO + Util.coroutineExceptionHandler).launch {
            val words = DictionaryDatabase.getINSTANCE(context).entryWordsDao().getSimilarEntryWords(word)
            withContext(Dispatchers.Main + Util.coroutineExceptionHandler) {
                adapter.clear()
                adapter.addAll(words)
                adapter.notifyDataSetChanged()
                adapter.filter.filter(word, editText)
            }
        }
    }

    private fun collapseViews() {
        if (layoutContainer.visibility == View.VISIBLE) {
            layoutContainer.visibility = View.GONE
            dragView!!.alpha = 0.6f
            editTextDontReceiveFocus()
        } else {
            layoutContainer.visibility = View.VISIBLE
            dragView!!.alpha = 1.0f
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val position = Point()
        if (isViewInBounds(rootView, event.rawX.toInt(), event.rawY.toInt())) editTextReceiveFocus() else editTextDontReceiveFocus()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pointerStartX = event.rawX.toInt()
                pointerStartY = event.rawY.toInt()
                initialX = layoutParams!!.x
                initialY = layoutParams!!.y
                return true
            }

            MotionEvent.ACTION_UP -> collapseViews()
            MotionEvent.ACTION_MOVE -> {
                val deltaX = pointerStartX - event.rawX
                val deltaY = event.rawY - pointerStartY
                position.x = (initialX + deltaX).toInt()
                position.y = (initialY + deltaY).toInt()
                setPosition(position)
            }
        }
        return true
    }

    private fun setPosition(position: Point) {
        try {
            layoutParams!!.x = position.x
            layoutParams!!.y = position.y
            update()
        } catch (e: Exception) {
            Util.showToast("could not move view!", context)
            e.printStackTrace()
        }
    }

    private fun update() {
        try {
            windowManager.updateViewLayout(rootView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isViewInBounds(view: View?, x: Int, y: Int): Boolean {
        val outRect = Rect()
        val location = IntArray(2)
        view!!.getDrawingRect(outRect)
        view.getLocationOnScreen(location)
        outRect.offset(location[0], location[1])
        return outRect.contains(x, y)
    }

    private fun editTextReceiveFocus() {
        if (!wasInFocus) {
            layoutParams!!.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            update()
            wasInFocus = true
        }
    }

    private fun editTextDontReceiveFocus() {
        if (wasInFocus) {
            layoutParams!!.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            update()
            wasInFocus = false
            Util.hideKeyboard(context, editText)
        }
    }

    private fun open() {
        try {
            windowManager.addView(rootView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun close() {
        try {
            windowManager.removeView(rootView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}