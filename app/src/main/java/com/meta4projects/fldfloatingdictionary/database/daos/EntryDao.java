package com.meta4projects.fldfloatingdictionary.database.daos;

import androidx.room.Dao;
import androidx.room.Query;

import com.meta4projects.fldfloatingdictionary.database.entities.Entry;

import java.util.List;

@Dao
public interface EntryDao {
    
    @Query("SELECT DISTINCT entry_word FROM entries")
    List<String> getAllEntryWords();
    
    @Query("SELECT * FROM entries WHERE entry_word =:word ORDER BY entry_word ASC")
    List<Entry> getAllEntriesForWord(String word);
}
