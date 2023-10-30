package com.meta4projects.fldfloatingdictionary.room.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import com.meta4projects.fldfloatingdictionary.room.daos.BookMarkDao
import com.meta4projects.fldfloatingdictionary.room.entities.BookMark

@Database(entities = [BookMark::class], version = 2, exportSchema = false)
abstract class BookmarkDatabase : RoomDatabase() {
    abstract fun bookMarkDao(): BookMarkDao

    companion object {
        @Volatile
        private var INSTANCE: BookmarkDatabase? = null

        @JvmStatic
        fun getINSTANCE(context: Context): BookmarkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = getDataBase(context)
                INSTANCE = instance
                instance
            }
        }

        private fun getDataBase(context: Context): BookmarkDatabase {
            return databaseBuilder(context.applicationContext, BookmarkDatabase::class.java, "bookmark_database").fallbackToDestructiveMigration().build()
        }
    }
}