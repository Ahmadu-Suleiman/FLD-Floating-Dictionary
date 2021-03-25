package com.meta4projects.fldfloatingdictionary.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.meta4projects.fldfloatingdictionary.R;
import com.meta4projects.fldfloatingdictionary.database.DictionaryDatabase;
import com.meta4projects.fldfloatingdictionary.database.entities.Entry;
import com.meta4projects.fldfloatingdictionary.others.Util;

import java.util.List;

public class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.EntryHolder> {
    
    private final List<String> entryWords;
    private final Context context;
    
    public EntryAdapter(List<String> entryWords, Context context) {
        this.entryWords = entryWords;
        this.context = context;
    }
    
    @NonNull
    @Override
    public EntryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_entry_view, parent, false);
        return new EntryHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull EntryHolder holder, int position) {
        holder.setEntry(entryWords.get(position), context);
    }
    
    @Override
    public int getItemCount() {
        return entryWords.size();
    }
    
    static class EntryHolder extends RecyclerView.ViewHolder {
        
        final LinearLayout layoutView;
        
        public EntryHolder(@NonNull View itemView) {
            super(itemView);
            
            layoutView = itemView.findViewById(R.id.layout_view);
        }
        
        private void setEntry(final String entryWord, final Context context) {
            final List<Entry> entries = DictionaryDatabase.getINSTANCE(context.getApplicationContext()).entryDao().getAllEntriesForWord(entryWord);
            
            layoutView.removeAllViews();
            
            for (Entry entry : entries) {
                layoutView.addView(createEntryView(entry, context, layoutView));
            }
        }
        
        private View createEntryView(Entry entry, Context context, ViewGroup root) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View viewEntry = inflater.inflate(R.layout.layout_entry, root, false);
            
            TextView textViewWord = viewEntry.findViewById(R.id.textView_word);
            TextView textViewPartOfSpeech = viewEntry.findViewById(R.id.textView_part_of_speech);
            TextView textViewDefinitions = viewEntry.findViewById(R.id.textView_definitions);
            TextView textViewSynonymsHeading = viewEntry.findViewById(R.id.textView_synonyms_heading);
            TextView textViewSynonyms = viewEntry.findViewById(R.id.textView_synonyms);
            TextView textViewSynonymsNoteHeading = viewEntry.findViewById(R.id.textView_synonyms_note_heading);
            TextView textViewSynonymsNote = viewEntry.findViewById(R.id.textView_synonyms_note);
            
            textViewWord.setText(Util.toTitleCase(entry.getWord()));
            textViewPartOfSpeech.setText(Util.getPartOfSpeech(entry.getPartOfSpeech()));
            
            StringBuilder definitions = new StringBuilder();
            for (String definition : entry.getDefinitions()) {
                definitions.append("- ").append(definition).append("\n").append("\n");
            }
            
            StringBuilder synonyms = new StringBuilder();
            for (String synonym : entry.getSynonyms()) {
                synonyms.append("- ").append(synonym.trim()).append("\n");
            }
            
            textViewDefinitions.setText(definitions);
            
            if (entry.getSynonyms().isEmpty()) {
                textViewSynonymsHeading.setVisibility(View.GONE);
                textViewSynonyms.setVisibility(View.GONE);
            } else {
                textViewSynonyms.setText(synonyms.toString().toLowerCase());
            }
            
            if (entry.getSynonyms_note() == null || entry.getSynonyms_note().isEmpty()) {
                textViewSynonymsNoteHeading.setVisibility(View.GONE);
                textViewSynonymsNote.setVisibility(View.GONE);
            } else {
                String synonymsNote = "- " + entry.getSynonyms_note();
                textViewSynonymsNote.setText(synonymsNote);
            }
            
            return viewEntry;
        }
    }
}
