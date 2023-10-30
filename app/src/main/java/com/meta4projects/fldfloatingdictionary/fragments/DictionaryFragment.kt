package com.meta4projects.fldfloatingdictionary.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.meta4projects.fldfloatingdictionary.R
import com.meta4projects.fldfloatingdictionary.adapters.EntryAdapter
import com.meta4projects.fldfloatingdictionary.others.EntryUtil
import com.meta4projects.fldfloatingdictionary.others.Util
import com.meta4projects.fldfloatingdictionary.room.database.BookmarkDatabase
import com.meta4projects.fldfloatingdictionary.room.database.DictionaryDatabase
import com.meta4projects.fldfloatingdictionary.room.entities.BookMark
import com.meta4projects.fldfloatingdictionary.room.entities.Entry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DictionaryFragment : Fragment() {
    private var entryWords = ArrayList<String>()
    private lateinit var entryAdapter: EntryAdapter
    private lateinit var editText: AutoCompleteTextView
    private lateinit var speaker: ImageView
    private lateinit var speakerWhole: ImageView
    private lateinit var bookmark: ImageView
    private lateinit var random: FloatingActionButton
    private lateinit var viewPager: ViewPager2
    private lateinit var textViewLoading: TextView
    private var bookmarked = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_dictionary, container, false)
        editText = view.findViewById(R.id.search_input_view_big)
        speaker = view.findViewById(R.id.speaker_big)
        speakerWhole = view.findViewById(R.id.speaker_whole)
        bookmark = view.findViewById(R.id.bookmark)
        random = view.findViewById(R.id.random_word)
        viewPager = view.findViewById(R.id.viewpager_big)
        textViewLoading = view.findViewById(R.id.loading_dictionary)
        val templateView = view.findViewById<TemplateView>(R.id.native_ad_dictionary)
        Util.loadNativeAd(requireActivity(), templateView, getString(R.string.native_ad_dictionary))
        if (arguments != null) initialiseWindow(requireArguments().getString(Util.WORD_EXTRA))
        return view
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initialiseWindow(wordGotten: String?) {
        val adapter = ArrayAdapter<String>(requireContext(), R.layout.layout_suggestion)
        editText.setAdapter(adapter)
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                replaceSearchEntryWords(s.toString().trim(), adapter)
            }

            override fun afterTextChanged(s: Editable) {}
        })
        editText.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = editText.text.toString().trim()
                if (query.isNotEmpty()) {
                    setWord(query)
                    editText.setText(query)
                    editText.dismissDropDown()
                    Util.hideKeyboard(requireContext(), editText)
                    return@setOnEditorActionListener true
                }
            }
            false
        }
        editText.onItemClickListener = OnItemClickListener { parent: AdapterView<*>, _: View?, position: Int, _: Long ->
            val query = parent.getItemAtPosition(position).toString()
            setWord(query)
            Util.hideKeyboard(requireContext(), editText)
        }
        setViewPager()
        speaker.setOnClickListener {
            val word = entryWords[viewPager.currentItem]
            Util.textToSpeech?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "speech_id")
        }
        speakerWhole.setOnClickListener {
            if (Util.textToSpeech?.isSpeaking == true) {
                Util.textToSpeech?.stop()
                speakerWhole.setImageResource(R.drawable.ic_play)
            } else {
                CoroutineScope(Dispatchers.IO + Util.coroutineExceptionHandler).launch {
                    val attributes = getAttributes()
                    withContext(Dispatchers.Main + Util.coroutineExceptionHandler) {
                        Util.textToSpeech?.speak(attributes, TextToSpeech.QUEUE_FLUSH, null, "whole_speech_id")
                    }
                }
            }
        }
        Util.textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                speakerWhole.setImageResource(R.drawable.ic_stop)
            }

            override fun onDone(utteranceId: String) {
                speakerWhole.setImageResource(R.drawable.ic_play)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                speakerWhole.setImageResource(R.drawable.ic_play)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                speakerWhole.setImageResource(R.drawable.ic_play)
            }
        })
        bookmark.setOnClickListener {
            val currentWord = entryWords[viewPager.currentItem]
            if (bookmarked) {
                removeBookmark(currentWord)
                Util.showToast("bookmark removed", requireContext())
            } else {
                addBookmark(currentWord)
                Util.showToast("bookmark added", requireContext())
            }
        }

        random.setOnClickListener {
            CoroutineScope(Dispatchers.IO + Util.coroutineExceptionHandler).launch {
                val randomWords = DictionaryDatabase.getINSTANCE(requireContext()).entryWordsDao().getRandomWords()
                withContext(Dispatchers.Main + Util.coroutineExceptionHandler) {
                    entryWords.clear()
                    entryWords.addAll(randomWords)
                    viewPager.setCurrentItem(entryWords.size / 2, true)
                    entryAdapter.notifyDataSetChanged()
                    checkBookmark()
                }
            }
        }
        if (wordGotten.isNullOrBlank()) requireActivity().finish() else setWord(wordGotten)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setViewPager() {
        entryAdapter = EntryAdapter(entryWords, requireContext(), true)
        viewPager.adapter = entryAdapter
        entryAdapter.setLinkClickListener { word -> setWord(word) }
        entryAdapter.notifyDataSetChanged()
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float,
                                        positionOffsetPixels: Int) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                Util.textToSpeech?.stop()
                speakerWhole.setImageResource(R.drawable.ic_play)
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                checkBookmark()
                val firstPosition = 0
                val lastPosition = entryWords.size - 1
                if (position == firstPosition || position == lastPosition) {
                    val currentWord = entryWords[viewPager.currentItem]

                    CoroutineScope(Dispatchers.IO + Util.coroutineExceptionHandler).launch {
                        val currentWords = DictionaryDatabase.getINSTANCE(requireContext()).entryWordsDao().getEntryWords(currentWord)
                        withContext(Dispatchers.Main + Util.coroutineExceptionHandler) {
                            entryWords.clear()
                            entryWords.addAll(currentWords)
                            viewPager.setCurrentItem(entryWords.indexOf(currentWord), false)
                            entryAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        })
    }

    private fun checkBookmark() {
        val word = entryWords[viewPager.currentItem]
        CoroutineScope(Dispatchers.IO + Util.coroutineExceptionHandler).launch {
            val exists = BookmarkDatabase.getINSTANCE(requireContext()).bookMarkDao().bookmarkExists(word)
            withContext(Dispatchers.Main + Util.coroutineExceptionHandler) {
                bookmarked = if (exists) {
                    bookmark.setImageResource(R.drawable.ic_bookmark)
                    true
                } else {
                    bookmark.setImageResource(R.drawable.ic_unbookmark)
                    false
                }
            }
        }
    }

    private fun addBookmark(word: String) {
        val bookMark = BookMark(word)
        CoroutineScope(Dispatchers.IO + Util.coroutineExceptionHandler).launch {
            BookmarkDatabase.getINSTANCE(requireContext()).bookMarkDao().insertBookMark(bookMark)

            withContext(Dispatchers.Main + Util.coroutineExceptionHandler) {
                bookmark.setImageResource(R.drawable.ic_bookmark)
                bookmarked = true
            }
        }
    }

    private fun removeBookmark(word: String?) {
        CoroutineScope(Dispatchers.IO + Util.coroutineExceptionHandler).launch {
            val bookMark = BookmarkDatabase.getINSTANCE(requireContext()).bookMarkDao().getBookmark(word)
            BookmarkDatabase.getINSTANCE(requireContext()).bookMarkDao().deleteBookMark(bookMark)

            withContext(Dispatchers.Main + Util.coroutineExceptionHandler) {
                bookmark.setImageResource(R.drawable.ic_unbookmark)
                bookmarked = false
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setWord(word: String?) {
        if (word.isNullOrBlank()) Util.showToast("could not find such word!", requireContext())
        else {
            CoroutineScope(Dispatchers.IO + Util.coroutineExceptionHandler).launch {
                val words = DictionaryDatabase.getINSTANCE(requireContext()).entryWordsDao().getEntryWords(word)
                withContext(Dispatchers.Main + Util.coroutineExceptionHandler) {
                    if (words.isEmpty()) Util.showToast("could not find such word!", requireContext()) else {
                        textViewLoading.visibility = View.GONE
                        entryWords.clear()
                        entryWords.addAll(words)
                        viewPager.setCurrentItem(entryWords.indexOf(word), true)
                        checkBookmark()
                        entryAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun replaceSearchEntryWords(word: String, adapter: ArrayAdapter<String>) {
        CoroutineScope(Dispatchers.IO + Util.coroutineExceptionHandler).launch {
            val words = DictionaryDatabase.getINSTANCE(requireContext()).entryWordsDao().getSimilarEntryWords(word)
            withContext(Dispatchers.Main + Util.coroutineExceptionHandler) {
                adapter.clear()
                adapter.addAll(words)
                adapter.notifyDataSetChanged()
                adapter.filter.filter(word, editText)
            }
        }
    }

    private suspend fun getAttributes(): String {
        val attributes = StringBuilder()
        val currentWord = entryWords[viewPager.currentItem]
        val entries = DictionaryDatabase.getINSTANCE(requireContext()).entryDao().getAllEntriesForWord(currentWord)
        entries.forEach { entry: Entry? ->
            attributes.append(entry!!.word).append(".").append(EntryUtil.getPartOfSpeech(entry.partOfSpeech)).append(".")
            if (entry.plural != null) attributes.append(entry.plural).append(".")
            if (entry.tenses != null) attributes.append(entry.tenses).append(".")
            if (entry.compare != null) attributes.append(entry.compare)
            attributes.append(".definition.")
            for (definition in entry.definitions!!) attributes.append(definition).append(".")
            if (entry.synonyms!!.isNotEmpty()) {
                attributes.append("synonyms.")
                entry.synonyms!!.forEach { synonym: String? ->
                    attributes.append(synonym).append(".")
                }
            }
            if (entry.antonyms!!.isNotEmpty()) {
                attributes.append("antonyms.")
                entry.antonyms!!.forEach { antonym: String? ->
                    attributes.append(antonym).append(".")
                }
            }
            if (entry.hypernyms!!.isNotEmpty()) {
                attributes.append("hypernyms.")
                entry.hypernyms!!.forEach { hypernym: String? ->
                    attributes.append(hypernym).append(".")
                }
            }
            if (entry.hyponyms!!.isNotEmpty()) {
                attributes.append("hyponyms.")
                entry.hyponyms!!.forEach { hyponym: String? ->
                    attributes.append(hyponym).append(".")
                }
            }
            if (entry.homophones!!.isNotEmpty()) {
                attributes.append("homophones.")
                entry.homophones!!.forEach { homophone: String? ->
                    attributes.append(homophone).append(".")
                }
            }
        }
        return attributes.toString()
    }

    override fun onDestroyView() {
        Util.textToSpeech?.stop()
        Util.textToSpeech?.setOnUtteranceProgressListener(null)
        super.onDestroyView()
    }
}