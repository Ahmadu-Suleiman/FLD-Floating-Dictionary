package com.meta4projects.fldfloatingdictionary.others

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.meta4projects.fldfloatingdictionary.R
import com.meta4projects.fldfloatingdictionary.room.entities.Entry

object EntryUtil {
    fun getPartOfSpeech(abbreviation: String?): String {
        return when (abbreviation) {
            "n" -> "Noun"
            "prp" -> "Preposition"
            "adj" -> "Adjective"
            "adv" -> "Adverb"
            "prn" -> "Pronoun"
            "v" -> "Verb"
            "cn" -> "Conjunction"
            "int" -> "Interjection"
            "pct" -> "Punctuation"
            "prt" -> "Particle"
            "ar" -> "Article"
            "dt" -> "Determiner"
            "prv" -> "Proverb"
            "sf" -> "Suffix"
            "prf" -> "Prefix"
            "intf" -> "Interfix"
            "inf" -> "Infix"
            "sm" -> "Symbol"
            "ph" -> "Phrase"
            "ab" -> "Abbreviation"
            "af" -> "Affix"
            "ch" -> "Character"
            "cr" -> "Circumfix"
            "nm" -> "Name"
            "num" -> "Numeral"
            "pp" -> "Postposition"
            "prpp" -> "Prepositional phrase"
            else -> abbreviation!!
        }
    }

    fun setEntry(entries: List<Entry>, layout: LinearLayout, textViewLoading: TextView,
                 context: Context) {
        for (entry in entries) layout.addView(createEntryView(entry, context, layout))
        textViewLoading.visibility = View.GONE
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
        textViewPartOfSpeech.text = getPartOfSpeech(entry.partOfSpeech)
        if (entry.plural.isNullOrEmpty()) textViewPlural.visibility = View.GONE else textViewPlural.text = entry.plural
        if (entry.tenses.isNullOrEmpty()) textViewTenses.visibility = View.GONE else textViewTenses.text = entry.tenses
        if (entry.compare.isNullOrEmpty()) textViewCompare.visibility = View.GONE else textViewCompare.text = entry.compare
        val definitions = StringBuilder()
        for (definition in entry.definitions!!) definitions.append("- ").append(definition).append("\n \n")
        textViewDefinitions.text = definitions
        val synonymText = StringBuilder()
        for (synonym in entry.synonyms!!) synonymText.append("- ").append(synonym).append("\n")
        textViewSynonyms.text = synonymText
        val antonymText = StringBuilder()
        for (antonym in entry.antonyms!!) antonymText.append("- ").append(antonym).append("\n")
        textViewAntonyms.text = antonymText
        val hypernymText = StringBuilder()
        for (hypernym in entry.hypernyms!!) hypernymText.append("- ").append(hypernym).append("\n")
        textViewHypernyms.text = hypernymText
        val hyponymText = StringBuilder()
        for (hyponym in entry.hyponyms!!) hyponymText.append("- ").append(hyponym).append("\n")
        textViewHyponyms.text = hyponymText
        val homophonesText = StringBuilder()
        for (homophone in entry.homophones!!) homophonesText.append("- ").append(homophone).append("\n")
        textViewHomophones.text = homophonesText
        if (entry.synonyms.isNullOrEmpty()) {
            textViewSynonymsHeading.visibility = View.GONE
            textViewSynonyms.visibility = View.GONE
        }
        if (entry.antonyms.isNullOrEmpty()) {
            textViewAntonymsHeading.visibility = View.GONE
            textViewAntonyms.visibility = View.GONE
        }
        if (entry.hypernyms.isNullOrEmpty()) {
            textViewHypernyms.visibility = View.GONE
            textViewHypernymsHeading.visibility = View.GONE
        }
        if (entry.hyponyms.isNullOrEmpty()) {
            textViewHyponyms.visibility = View.GONE
            textViewHyponymsHeading.visibility = View.GONE
        }
        if (entry.homophones.isNullOrEmpty()) {
            textViewHomophones.visibility = View.GONE
            textViewHomophonesHeading.visibility = View.GONE
        }
        return viewEntry
    }
}