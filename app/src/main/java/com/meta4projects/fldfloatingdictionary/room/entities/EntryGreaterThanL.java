package com.meta4projects.fldfloatingdictionary.room.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.meta4projects.fldfloatingdictionary.room.StringArrayConverter;

import java.util.ArrayList;

@Entity(tableName = "entries_greater_than_L", indices = {@Index(name = "index_greater_than_L", value = "entry_word")})
public class EntryGreaterThanL extends Entry {

    @ColumnInfo(name = "entry_id")
    @PrimaryKey(autoGenerate = true)
    private int entryId;

    @ColumnInfo(name = "entry_word")
    private String word;

    @ColumnInfo(name = "entry_plural")
    private String plural;

    @ColumnInfo(name = "entry_part_of_speech")
    private String partOfSpeech;

    @ColumnInfo(name = "entry_tenses")
    private String tenses;

    @ColumnInfo(name = "entry_compare")
    private String compare;

    @TypeConverters(StringArrayConverter.class)
    @ColumnInfo(name = "entry_definitions")
    private ArrayList<String> definitions;

    @TypeConverters(StringArrayConverter.class)
    @ColumnInfo(name = "entry_synonyms")
    private ArrayList<String> synonyms;

    @TypeConverters(StringArrayConverter.class)
    @ColumnInfo(name = "entry_antonyms")
    private ArrayList<String> antonyms;

    @TypeConverters(StringArrayConverter.class)
    @ColumnInfo(name = "entry_hypernyms")
    private ArrayList<String> hypernyms;

    @TypeConverters(StringArrayConverter.class)
    @ColumnInfo(name = "entry_hyponyms")
    private ArrayList<String> hyponyms;

    @TypeConverters(StringArrayConverter.class)
    @ColumnInfo(name = "entry_homophones")
    private ArrayList<String> homophones;

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

    public String getPlural() {
        return plural;
    }

    public void setPlural(String plural) {
        this.plural = plural;
    }

    public String getPartOfSpeech() {
        return partOfSpeech;
    }

    public void setPartOfSpeech(String partOfSpeech) {
        this.partOfSpeech = partOfSpeech;
    }

    public String getTenses() {
        return tenses;
    }

    public void setTenses(String tenses) {
        this.tenses = tenses;
    }

    public String getCompare() {
        return compare;
    }

    public void setCompare(String compare) {
        this.compare = compare;
    }

    public ArrayList<String> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(ArrayList<String> definitions) {
        this.definitions = definitions;
    }

    public ArrayList<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(ArrayList<String> synonyms) {
        this.synonyms = synonyms;
    }

    public ArrayList<String> getAntonyms() {
        return antonyms;
    }

    public void setAntonyms(ArrayList<String> antonyms) {
        this.antonyms = antonyms;
    }

    public ArrayList<String> getHypernyms() {
        return hypernyms;
    }

    public void setHypernyms(ArrayList<String> hypernyms) {
        this.hypernyms = hypernyms;
    }

    public ArrayList<String> getHyponyms() {
        return hyponyms;
    }

    public void setHyponyms(ArrayList<String> hyponyms) {
        this.hyponyms = hyponyms;
    }

    public ArrayList<String> getHomophones() {
        return homophones;
    }

    public void setHomophones(ArrayList<String> homophones) {
        this.homophones = homophones;
    }
}
