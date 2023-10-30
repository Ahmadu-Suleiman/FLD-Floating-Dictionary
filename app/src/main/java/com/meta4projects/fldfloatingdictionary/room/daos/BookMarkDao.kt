package com.meta4projects.fldfloatingdictionary.room.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.meta4projects.fldfloatingdictionary.room.entities.BookMark

@Dao
interface BookMarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY bookmark_id DESC")
    fun getAllBookMarks(): List<BookMark>

    @Query("SELECT bookmark_word FROM bookmarks")
    fun getAllBookMarkWords(): List<String>

    @Query("SELECT (COUNT(*) > 0) AS FOUND FROM bookmarks WHERE bookmark_word = :word")
    fun bookmarkExists(word: String?): Boolean

    @Query("SELECT * FROM bookmarks WHERE bookmark_word = :word LIMIT 1")
    fun getBookmark(word: String?): BookMark

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBookMark(bookMark: BookMark)

    @Delete
    fun deleteBookMark(bookMark: BookMark)

    @Query("DELETE FROM bookmarks")
    fun deleteAllBookMarks()
}