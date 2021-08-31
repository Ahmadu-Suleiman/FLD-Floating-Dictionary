package com.meta4projects.fldfloatingdictionary.room.daos;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import com.meta4projects.fldfloatingdictionary.room.entities.Entry;
import com.meta4projects.fldfloatingdictionary.room.entities.EntryGreaterThanL;
import com.meta4projects.fldfloatingdictionary.room.entities.EntryLessEqualToL;

import java.util.ArrayList;
import java.util.List;

@Dao
public abstract class EntryDao {

    @Transaction
    public List<Entry> getAllEntriesForWord(String word) {
        List<Entry> entries = new ArrayList<>();

        if (word.compareToIgnoreCase("M") >= 0)
            entries.addAll(getAllEntriesForWordGreaterThanL(word));
        else entries.addAll(getAllEntriesForWordLessEqualToL(word));
        return entries;
    }

    @Query("SELECT * FROM entries_greater_than_L WHERE entry_word =:word")
    abstract List<EntryGreaterThanL> getAllEntriesForWordGreaterThanL(String word);

    @Query("SELECT * FROM entries_less_equal_to_L WHERE entry_word =:word")
    abstract List<EntryLessEqualToL> getAllEntriesForWordLessEqualToL(String word);
}
