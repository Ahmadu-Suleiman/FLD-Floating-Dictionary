package com.meta4projects.fldfloatingdictionary.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.meta4projects.fldfloatingdictionary.database.StringArrayConverter;

import java.util.ArrayList;

@Entity(tableName = "entries")
public class Entry {
    @ColumnInfo(name = "entry_id")
    @PrimaryKey(autoGenerate = true)
    private int entryId;
    
    @ColumnInfo(name = "entry_word")
    private String word;
    
    @ColumnInfo(name = "entry_partOfSpeech")
    private String partOfSpeech;
    
    @TypeConverters(StringArrayConverter.class)
    @ColumnInfo(name = "entry_definitions")
    private ArrayList<String> definitions;
    
    @TypeConverters(StringArrayConverter.class)
    @ColumnInfo(name = "entry_synonyms")
    private ArrayList<String> synonyms;
    
    @ColumnInfo(name = "entry_synonyms_note")
    private String synonyms_note;
    
    public int getEntryId() {
        return entryId;
    }
    
    public void setEntryId(int entryId) {
        this.entryId = entryId;
    }
    
    public String getWord() {
        return word;
    }
    
    public void setWord(String word) {
        this.word = word;
    }
    
    public String getPartOfSpeech() {
        return partOfSpeech;
    }
    
    public void setPartOfSpeech(String partOfSpeech) {
        this.partOfSpeech = partOfSpeech;
    }
    
    public ArrayList<String> getSynonyms() {
        return synonyms;
    }
    
    public void setSynonyms(ArrayList<String> synonyms) {
        this.synonyms = synonyms;
    }
    
    public String getSynonyms_note() {
        return synonyms_note;
    }
    
    public void setSynonyms_note(String synonyms_note) {
        this.synonyms_note = synonyms_note;
    }
    
    public ArrayList<String> getDefinitions() {
        return definitions;
    }
    
    public void setDefinitions(ArrayList<String> definitions) {
        this.definitions = definitions;
    }
}
