package com.meta4projects.fldfloatingdictionary.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
class BookMark(@field:ColumnInfo(name = "bookmark_word") var bookmarkWord: String) {
    @ColumnInfo(name = "bookmark_id")
    @PrimaryKey(autoGenerate = true)
    var bookmarkId = 0

}