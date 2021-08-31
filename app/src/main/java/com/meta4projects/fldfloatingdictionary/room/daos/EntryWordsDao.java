package com.meta4projects.fldfloatingdictionary.room.daos;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.ArrayList;
import java.util.List;

@Dao
public abstract class EntryWordsDao {

    @Query("SELECT entry_word FROM entry_words WHERE entry_word LIKE :word || '%' LIMIT 20")
    public abstract List<String> getSimilarEntryWords(String word);

    @Query("SELECT (COUNT(*) > 0) AS FOUND FROM entry_words WHERE entry_word = :word")
    public abstract boolean wordExist(String word);// TODO: 12/03/2023 remove found?

    @Query("SELECT entry_word FROM entry_words ORDER BY random() LIMIT 1")
    public abstract String getRandomWord();// TODO: 12/03/2023 add limit to random,capitalize

    @Transaction
    public List<String> getRandomWords() {
        return getEntryWords(getRandomWord());
    }

    @Transaction
    public List<String> getEntryWords(String word) {
        List<String> words = new ArrayList<>();
        if (wordExist(word)) {
            words.addAll(getPreviousWords(word));
            words.add(word);// TODO: 12/03/2023 fix blank dictionary if word not exist
            words.addAll(getNextWords(word));
            words.sort(String::compareToIgnoreCase);// TODO: 12/03/2023 remove?
        }
        return words;
    }

    @Query("SELECT entry_word FROM entry_words WHERE entry_word < :word ORDER BY entry_word DESC LIMIT 50")
    abstract List<String> getPreviousWords(String word);

    @Query("SELECT entry_word FROM entry_words WHERE entry_word > :word ORDER BY entry_word ASC LIMIT 50")
    abstract List<String> getNextWords(String word);
}
