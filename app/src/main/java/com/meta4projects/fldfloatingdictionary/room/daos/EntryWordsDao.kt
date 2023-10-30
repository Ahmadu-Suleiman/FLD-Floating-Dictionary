package com.meta4projects.fldfloatingdictionary.room.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class EntryWordsDao {
    @Query("SELECT entry_word FROM entry_words WHERE entry_word LIKE :word || '%' LIMIT 20")
    abstract fun getSimilarEntryWords(word: String?): List<String>
    @Query("SELECT (COUNT(*) > 0) AS FOUND FROM entry_words WHERE entry_word = :word LIMIT 1")
    abstract fun wordExist(word: String?): Boolean

    @Query("SELECT entry_word FROM entry_words WHERE rowid = (abs(random()) % (select (select max(rowid) from entry_words)+1)) LIMIT 1")
    abstract fun getRandomWord(): String

    @Transaction
    open suspend fun getRandomWords(): List<String> {
     return getEntryWords(getRandomWord())
    }

    @Transaction
    open suspend fun getEntryWords(word: String): List<String> {
        val words: ArrayList<String> = ArrayList()
        if (wordExist(word)) {
            words.addAll(getPreviousWords(word))
            words.add(word)
            words.addAll(getNextWords(word))
        }
        return words
    }

    @Query("SELECT entry_word FROM entry_words WHERE entry_word < :word ORDER BY entry_word DESC LIMIT 50")
    abstract fun getPreviousWords(word: String?): List<String>
    @Query("SELECT entry_word FROM entry_words WHERE entry_word > :word ORDER BY entry_word ASC LIMIT 50")
    abstract fun getNextWords(word: String?): List<String>
}