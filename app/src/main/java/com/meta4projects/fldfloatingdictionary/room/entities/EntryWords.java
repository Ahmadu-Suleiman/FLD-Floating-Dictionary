package com.meta4projects.fldfloatingdictionary.room.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "entry_words", indices = {@Index(name = "index_entry_word", value = "entry_word")})
public class EntryWords {

    @ColumnInfo(name = "entry_id")
    @PrimaryKey(autoGenerate = true)
    private int entryId;

    @ColumnInfo(name = "entry_word")
    private String word;

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
}
