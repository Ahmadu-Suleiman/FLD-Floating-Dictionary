package com.meta4projects.fldfloatingdictionary.adapters

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.meta4projects.fldfloatingdictionary.R
import com.meta4projects.fldfloatingdictionary.adapters.EntryAdapter.EntryHolder
import com.meta4projects.fldfloatingdictionary.others.EntryUtil
import com.meta4projects.fldfloatingdictionary.others.Util
import com.meta4projects.fldfloatingdictionary.room.database.DictionaryDatabase
import com.meta4projects.fldfloatingdictionary.room.entities.Entry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EntryAdapter : RecyclerView.Adapter<EntryHolder> {
    private val entryWords: ArrayList<String>
    private val context: Context
    private var linkClickListener: LinkClickListener? = null
    private var isBig = false

    constructor(entryWords: ArrayList<String>, context: Context) {
        this.entryWords = entryWords
        this.context = context
    }

    constructor(entryWords: ArrayList<String>, context: Context, isBig: Boolean) {
        this.entryWords = entryWords
        this.context = context
        this.isBig = isBig
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_entry_view, parent, false)
        return EntryHolder(view)
    }

    override fun onBindViewHolder(holder: EntryHolder, position: Int) {
        holder.setEntry(entryWords[position], context)
    }

    override fun getItemCount(): Int {
        return entryWords.size
    }

    fun setLinkClickListener(linkClickListener: LinkClickListener) {
        this.linkClickListener = linkClickListener
    }

    fun interface LinkClickListener {
        fun onLinkClicked(link: String?)
    }

    inner class EntryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutView: LinearLayout
        val scrollView: ScrollView

        init {
            layoutView = itemView.findViewById(R.id.layout_view)
            scrollView = itemView.findViewById(R.id.entry_scrollview)
        }

        fun setEntry(entryWord: String, context: Context) {
            CoroutineScope(Dispatchers.IO + Util.coroutineExceptionHandler).launch {
                val entries = DictionaryDatabase.getINSTANCE(context).entryDao().getAllEntriesForWord(entryWord)

                withContext(Dispatchers.Main + Util.coroutineExceptionHandler) {
                    layoutView.removeAllViews()
                    for (entry in entries) layoutView.addView(createEntryView(entry, context, layoutView))
                    scrollView.fullScroll(View.FOCUS_UP)
                }
            }
        }

        private fun createEntryView(entry: Entry, context: Context, root: ViewGroup): View {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val viewEntry = inflater.inflate(R.layout.layout_entry, root, false)
            val textViewWord = viewEntry.findViewById<TextView>(R.id.textView_word)
            val textViewPlural = viewEntry.findViewById<TextView>(R.id.textView_plural)
            val textViewPartOfSpeech = viewEntry.findViewById<TextView>(R.id.textView_part_of_speech)
            val textViewTenses = viewEntry.findViewById<TextView>(R.id.textView_tenses)
            val textViewCompare = viewEntry.findViewById<TextView>(R.id.textView_compare)
            val textViewDefinitions = viewEntry.findViewById<TextView>(R.id.textView_definitions)
            val textViewSynonymsHeading = viewEntry.findViewById<TextView>(R.id.textView_synonyms_heading)
            val textViewSynonyms = viewEntry.findViewById<TextView>(R.id.textView_synonyms)
            val textViewAntonymsHeading = viewEntry.findViewById<TextView>(R.id.textView_antonyms_heading)
            val textViewAntonyms = viewEntry.findViewById<TextView>(R.id.textView_antonyms)
            val textViewHypernyms = viewEntry.findViewById<TextView>(R.id.textView_hypernyms)
            val textViewHypernymsHeading = viewEntry.findViewById<TextView>(R.id.textView_hypernyms_heading)
            val textViewHyponyms = viewEntry.findViewById<TextView>(R.id.textView_hyponyms)
            val textViewHyponymsHeading = viewEntry.findViewById<TextView>(R.id.textView_hyponyms_heading)
            val textViewHomophones = viewEntry.findViewById<TextView>(R.id.textView_homophones)
            val textViewHomophonesHeading = viewEntry.findViewById<TextView>(R.id.textView_homophones_heading)
            textViewWord.text = entry.word
            textViewPartOfSpeech.text = EntryUtil.getPartOfSpeech(entry.partOfSpeech)
            if (TextUtils.isEmpty(entry.plural)) textViewPlural.visibility = View.GONE else textViewPlural.text = entry.plural
            if (TextUtils.isEmpty(entry.tenses)) textViewTenses.visibility = View.GONE else textViewTenses.text = entry.tenses
            if (TextUtils.isEmpty(entry.compare)) textViewCompare.visibility = View.GONE else textViewCompare.text = entry.compare
            val definitions = StringBuilder()
            for (definition in entry.definitions!!) definitions.append("- ").append(definition).append("\n\n")
            textViewDefinitions.text = definitions.toString()

            for (synonym in entry.synonyms!!) {
                val spannableSynonym = SpannableString(synonym)
                spannableSynonym.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        if (linkClickListener != null) {
                            linkClickListener!!.onLinkClicked(synonym)
                            scrollView.fullScroll(View.FOCUS_UP)
                        }
                    }
                }, 0, synonym.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val synonymText = TextUtils.concat("- ", spannableSynonym, "\n")
                textViewSynonyms.append(synonymText)
                textViewSynonyms.movementMethod = LinkMovementMethod.getInstance()
            }
            for (antonym in entry.antonyms!!) {
                val spannableAntonym = SpannableString(antonym)
                spannableAntonym.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        linkClickListener?.onLinkClicked(antonym)
                        scrollView.fullScroll(View.FOCUS_UP)
                    }
                }, 0, antonym.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val antonymText = TextUtils.concat("- ", spannableAntonym, "\n")
                textViewAntonyms.append(antonymText)
                textViewAntonyms.movementMethod = LinkMovementMethod.getInstance()
            }
            for (hypernym in entry.hypernyms!!) {
                val spannableHypernym = SpannableString(hypernym)
                spannableHypernym.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        linkClickListener?.onLinkClicked(hypernym)
                        scrollView.fullScroll(View.FOCUS_UP)
                    }
                }, 0, hypernym.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val hypernymText = TextUtils.concat("- ", spannableHypernym, "\n")
                textViewHypernyms.append(hypernymText)
                textViewHypernyms.movementMethod = LinkMovementMethod.getInstance()
            }
            for (hyponym in entry.hyponyms!!) {
                val spannableHyponym = SpannableString(hyponym)
                spannableHyponym.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        linkClickListener?.onLinkClicked(hyponym)
                        scrollView.fullScroll(View.FOCUS_UP)
                    }
                }, 0, hyponym.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val hyponymText = TextUtils.concat("- ", spannableHyponym, "\n")
                textViewHyponyms.append(hyponymText)
                textViewHyponyms.movementMethod = LinkMovementMethod.getInstance()
            }
            for (homophone in entry.homophones!!) {
                val spannableHomophone = SpannableString(homophone)
                spannableHomophone.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        linkClickListener?.onLinkClicked(homophone)
                        scrollView.fullScroll(View.FOCUS_UP)
                    }
                }, 0, homophone.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val homophoneText = TextUtils.concat("- ", spannableHomophone, "\n")
                textViewHomophones.append(homophoneText)
                textViewHomophones.movementMethod = LinkMovementMethod.getInstance()
            }
            if (entry.synonyms!!.isEmpty()) {
                textViewSynonymsHeading.visibility = View.GONE
                textViewSynonyms.visibility = View.GONE
            }
            if (entry.antonyms!!.isEmpty()) {
                textViewAntonymsHeading.visibility = View.GONE
                textViewAntonyms.visibility = View.GONE
            }
            if (entry.hypernyms!!.isEmpty()) {
                textViewHypernyms.visibility = View.GONE
                textViewHypernymsHeading.visibility = View.GONE
            }
            if (entry.hyponyms!!.isEmpty()) {
                textViewHyponyms.visibility = View.GONE
                textViewHyponymsHeading.visibility = View.GONE
            }
            if (entry.homophones!!.isEmpty()) {
                textViewHomophones.visibility = View.GONE
                textViewHomophonesHeading.visibility = View.GONE
            }
            if (!isBig) {
                textViewWord.setOnLongClickListener {
                    Util.copyText(entry.word, "word", context)
                    true
                }
                textViewPlural.setOnLongClickListener {
                    Util.copyText(entry.plural, "plural", context)
                    true
                }
                textViewPartOfSpeech.setOnLongClickListener {
                    Util.copyText(entry.partOfSpeech, "part of speech", context)
                    true
                }
                textViewDefinitions.setOnLongClickListener {
                    Util.copyText(definitions.toString(), "definitions", context)
                    true
                }
            }
            return viewEntry
        }
    }
}