package com.meta4projects.fldfloatingdictionary.room.entities;

import java.util.ArrayList;

public abstract class Entry {

    private int entryId;

    private String word;

    private String plural;

    private String partOfSpeech;

    private String tenses;

    private String compare;

    private ArrayList<String> definitions;

    private ArrayList<String> synonyms;

    private ArrayList<String> antonyms;

    private ArrayList<String> hypernyms;

    private ArrayList<String> hyponyms;

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
