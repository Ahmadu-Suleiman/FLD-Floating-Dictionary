package com.meta4projects.fldfloatingdictionary.room.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.meta4projects.fldfloatingdictionary.room.daos.BookMarkDao;
import com.meta4projects.fldfloatingdictionary.room.entities.BookMark;

@Database(entities = {BookMark.class}, version = 1, exportSchema = false)
public abstract class BookmarkDatabase extends RoomDatabase {

    private static volatile BookmarkDatabase INSTANCE;

    public static BookmarkDatabase getINSTANCE(final Context context) {
        if (INSTANCE == null) {
            synchronized (BookmarkDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = getDataBase(context);
                }
            }
        }

        return INSTANCE;
    }

    private static BookmarkDatabase getDataBase(final Context context) {
        return Room.databaseBuilder(context.getApplicationContext(),
                BookmarkDatabase.class, "bookmark_database").build();
    }

    public abstract BookMarkDao bookMarkDao();
}
