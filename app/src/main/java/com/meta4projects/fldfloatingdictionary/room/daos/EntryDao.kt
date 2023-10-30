package com.meta4projects.fldfloatingdictionary.room.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.meta4projects.fldfloatingdictionary.room.entities.Entry
import com.meta4projects.fldfloatingdictionary.room.entities.EntryGreaterThanL
import com.meta4projects.fldfloatingdictionary.room.entities.EntryLessEqualToL

@Dao
abstract class EntryDao {
    @Transaction
    open suspend fun getAllEntriesForWord(word: String): List<Entry> {
        val entries: ArrayList<Entry> = ArrayList()
        if (word.compareTo("M", ignoreCase = true) >= 0) entries.addAll(getAllEntriesForWordGreaterThanL(word))
        else entries.addAll(getAllEntriesForWordLessEqualToL(word))
        return entries
    }

    @Query("SELECT * FROM entries_greater_than_L WHERE entry_word =:word")
    abstract fun getAllEntriesForWordGreaterThanL(word: String): List<EntryGreaterThanL>

    @Query("SELECT * FROM entries_less_equal_to_L WHERE entry_word =:word")
    abstract fun getAllEntriesForWordLessEqualToL(word: String): List<EntryLessEqualToL>
}