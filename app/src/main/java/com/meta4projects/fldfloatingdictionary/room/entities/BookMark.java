package com.meta4projects.fldfloatingdictionary.room.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "bookmarks")
public class BookMark {

    @ColumnInfo(name = "bookmark_id")
    @PrimaryKey(autoGenerate = true)
    private int bookmarkId;

    @ColumnInfo(name = "bookmark_word")
    private String bookmarkWord;

    public BookMark(String bookmarkWord) {
        this.bookmarkWord = bookmarkWord;
    }

    public int getBookmarkId() {
        return bookmarkId;
    }

    public void setBookmarkId(int bookmarkId) {
        this.bookmarkId = bookmarkId;
    }

    public String getBookmarkWord() {
        return bookmarkWord;
    }

    public void setBookmarkWord(String bookmarkWord) {
        this.bookmarkWord = bookmarkWord;
    }
}
