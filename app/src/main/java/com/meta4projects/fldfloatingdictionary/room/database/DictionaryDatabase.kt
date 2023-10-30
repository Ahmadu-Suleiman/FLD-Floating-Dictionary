package com.meta4projects.fldfloatingdictionary.room.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import com.meta4projects.fldfloatingdictionary.room.daos.EntryDao
import com.meta4projects.fldfloatingdictionary.room.daos.EntryWordsDao
import com.meta4projects.fldfloatingdictionary.room.entities.EntryGreaterThanL
import com.meta4projects.fldfloatingdictionary.room.entities.EntryLessEqualToL
import com.meta4projects.fldfloatingdictionary.room.entities.EntryWords

@Database(entities = [EntryLessEqualToL::class, EntryGreaterThanL::class, EntryWords::class], version = 4, exportSchema = false)
abstract class DictionaryDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun entryWordsDao(): EntryWordsDao

    companion object {
        @Volatile
        private var INSTANCE: DictionaryDatabase? = null

        @JvmStatic
        fun getINSTANCE(context: Context): DictionaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = getDataBase(context)
                INSTANCE = instance
                instance
            }
        }

        private fun getDataBase(context: Context): DictionaryDatabase {
            return databaseBuilder(context.applicationContext, DictionaryDatabase::class.java, "dictionary_database").createFromAsset("WiktionaryDatabase.db").fallbackToDestructiveMigration().build()
        }
    }
}