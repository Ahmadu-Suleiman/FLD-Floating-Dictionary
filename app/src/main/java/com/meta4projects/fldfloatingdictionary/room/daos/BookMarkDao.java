package com.meta4projects.fldfloatingdictionary.room.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.meta4projects.fldfloatingdictionary.room.entities.BookMark;

import java.util.List;

@Dao
public interface BookMarkDao {

    @Query("SELECT * FROM bookmarks ORDER BY bookmark_id DESC")
    List<BookMark> getAllBookMarks();

    @Query("SELECT bookmark_word FROM bookmarks")
    List<String> getAllBookMarkWords();

    @Query("SELECT (COUNT(*) > 0) AS FOUND FROM bookmarks WHERE bookmark_word = :word")
    boolean bookmarkExists(String word);

    @Query("SELECT * FROM bookmarks WHERE bookmark_word = :word LIMIT 1")
    BookMark getBookmark(String word);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBookMark(BookMark bookMark);

    @Delete
    void deleteBookMark(BookMark bookMark);

    @Query("DELETE FROM bookmarks")
    void deleteAllBookMarks();
}
