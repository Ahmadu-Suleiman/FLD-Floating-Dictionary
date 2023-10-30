package com.meta4projects.fldfloatingdictionary.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "entry_words", indices = [Index(name = "index_entry_word", value = arrayOf("entry_word"))])
class EntryWords {
    @ColumnInfo(name = "entry_id")
    @PrimaryKey(autoGenerate = true)
    var entryId = 0

    @ColumnInfo(name = "entry_word")
    var word: String? = null
}