package com.meta4projects.fldfloatingdictionary.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.meta4projects.fldfloatingdictionary.database.daos.EntryDao;
import com.meta4projects.fldfloatingdictionary.database.entities.Entry;

@Database(entities = {Entry.class}, version = 1)
public abstract class DictionaryDatabase extends RoomDatabase {
    
    private static volatile DictionaryDatabase INSTANCE;
    
    public static DictionaryDatabase getINSTANCE(final Context context) {
        if (INSTANCE == null) {
            synchronized (DictionaryDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = getDataBase(context);
                }
            }
        }
        
        return INSTANCE;
    }
    
    private static DictionaryDatabase getDataBase(final Context context) {
        return Room.databaseBuilder(context.getApplicationContext(),
                DictionaryDatabase.class, "dictionary_database").allowMainThreadQueries()
                .createFromAsset("dictionary_database.db").build();
    }
    
    public abstract EntryDao entryDao();
}
