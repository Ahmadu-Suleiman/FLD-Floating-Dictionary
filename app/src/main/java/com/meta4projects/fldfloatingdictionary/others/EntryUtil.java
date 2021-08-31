package com.meta4projects.fldfloatingdictionary.others;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meta4projects.fldfloatingdictionary.R;
import com.meta4projects.fldfloatingdictionary.room.entities.Entry;

import java.util.List;

public class EntryUtil {

    public static String getPartOfSpeech(String abbreviation) {

        return switch (abbreviation) {
            case "n" -> "Noun";
            case "prp" -> "Preposition";
            case "adj" -> "Adjective";
            case "adv" -> "Adverb";
            case "prn" -> "Pronoun";
            case "v" -> "Verb";
            case "cn" -> "Conjunction";
            case "int" -> "Interjection";
            case "pct" -> "Punctuation";
            case "prt" -> "Particle";
            case "ar" -> "Article";
            case "dt" -> "Determiner";
            case "prv" -> "Proverb";
            case "sf" -> "Suffix";
            case "prf" -> "Prefix";
            case "intf" -> "Interfix";
            case "inf" -> "Infix";
            case "sm" -> "Symbol";
            case "ph" -> "Phrase";
            case "ab" -> "Abbreviation";
            case "af" -> "Affix";
            case "ch" -> "Character";
            case "cr" -> "Circumfix";
            case "nm" -> "Name";
            case "num" -> "Numeral";
            case "pp" -> "Postposition";
            case "prpp" -> "Prepositional phrase";
            default -> abbreviation;
        };
    }

    public static void setEntry(List<Entry> entries, LinearLayout layout, TextView textViewLoading, Context context) {
        for (Entry entry : entries)
            layout.addView(createEntryView(entry, context, layout));
        textViewLoading.setVisibility(View.GONE);
    }

    private static View createEntryView(Entry entry, Context context, ViewGroup root) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewEntry = inflater.inflate(R.layout.layout_entry, root, false);

        TextView textViewWord = viewEntry.findViewById(R.id.textView_word);
        TextView textViewPlural = viewEntry.findViewById(R.id.textView_plural);
        TextView textViewPartOfSpeech = viewEntry.findViewById(R.id.textView_part_of_speech);
        TextView textViewTenses = viewEntry.findViewById(R.id.textView_tenses);
        TextView textViewCompare = viewEntry.findViewById(R.id.textView_compare);
        TextView textViewDefinitions = viewEntry.findViewById(R.id.textView_definitions);
        TextView textViewSynonymsHeading = viewEntry.findViewById(R.id.textView_synonyms_heading);
        TextView textViewSynonyms = viewEntry.findViewById(R.id.textView_synonyms);
        TextView textViewAntonymsHeading = viewEntry.findViewById(R.id.textView_antonyms_heading);
        TextView textViewAntonyms = viewEntry.findViewById(R.id.textView_antonyms);
        TextView textViewHypernyms = viewEntry.findViewById(R.id.textView_hypernyms);
        TextView textViewHypernymsHeading = viewEntry.findViewById(R.id.textView_hypernyms_heading);
        TextView textViewHyponyms = viewEntry.findViewById(R.id.textView_hyponyms);
        TextView textViewHyponymsHeading = viewEntry.findViewById(R.id.textView_hyponyms_heading);
        TextView textViewHomophones = viewEntry.findViewById(R.id.textView_homophones);
        TextView textViewHomophonesHeading = viewEntry.findViewById(R.id.textView_homophones_heading);

        textViewWord.setText(entry.getWord());
        textViewPartOfSpeech.setText(getPartOfSpeech(entry.getPartOfSpeech()));

        if (isEmpty(entry.getPlural())) textViewPlural.setVisibility(View.GONE);
        else textViewPlural.setText(entry.getPlural());

        if (isEmpty(entry.getTenses())) textViewTenses.setVisibility(View.GONE);
        else textViewTenses.setText(entry.getTenses());

        if (isEmpty(entry.getCompare())) textViewCompare.setVisibility(View.GONE);
        else textViewCompare.setText(entry.getCompare());

        StringBuilder definitions = new StringBuilder();
        for (String definition : entry.getDefinitions())
            definitions.append("- ").append(definition).append("\n \n");

        textViewDefinitions.setText(definitions.toString().trim().concat("\n"));

        StringBuilder synonymText = new StringBuilder();
        for (String synonym : entry.getSynonyms())
            synonymText.append("- ").append(synonym).append("\n");
        textViewSynonyms.setText(synonymText);

        StringBuilder antonymText = new StringBuilder();
        for (String antonym : entry.getAntonyms())
            antonymText.append("- ").append(antonym).append("\n");
        textViewAntonyms.setText(antonymText);

        StringBuilder hypernymText = new StringBuilder();
        for (String hypernym : entry.getHypernyms())
            hypernymText.append("- ").append(hypernym).append("\n");
        textViewHypernyms.setText(hypernymText);

        StringBuilder hyponymText = new StringBuilder();
        for (String hyponym : entry.getHyponyms())
            hyponymText.append("- ").append(hyponym).append("\n");
        textViewHyponyms.setText(hyponymText);

        StringBuilder homophonesText = new StringBuilder();
        for (String homophone : entry.getHomophones())
            homophonesText.append("- ").append(homophone).append("\n");
        textViewHomophones.setText(homophonesText);

        if (entry.getSynonyms().isEmpty()) {
            textViewSynonymsHeading.setVisibility(View.GONE);
            textViewSynonyms.setVisibility(View.GONE);
        }

        if (entry.getAntonyms().isEmpty()) {
            textViewAntonymsHeading.setVisibility(View.GONE);
            textViewAntonyms.setVisibility(View.GONE);
        }

        if (entry.getHypernyms().isEmpty()) {
            textViewHypernyms.setVisibility(View.GONE);
            textViewHypernymsHeading.setVisibility(View.GONE);
        }

        if (entry.getHyponyms().isEmpty()) {
            textViewHyponyms.setVisibility(View.GONE);
            textViewHyponymsHeading.setVisibility(View.GONE);
        }

        if (entry.getHomophones().isEmpty()) {
            textViewHomophones.setVisibility(View.GONE);
            textViewHomophonesHeading.setVisibility(View.GONE);
        }

        return viewEntry;
    }

    private static boolean isEmpty(String word) {
        return word == null || word.isEmpty();
    }
}
