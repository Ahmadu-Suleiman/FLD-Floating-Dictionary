package com.meta4projects.fldfloatingdictionary.room.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.meta4projects.fldfloatingdictionary.room.daos.EntryDao;
import com.meta4projects.fldfloatingdictionary.room.daos.EntryWordsDao;
import com.meta4projects.fldfloatingdictionary.room.entities.EntryGreaterThanL;
import com.meta4projects.fldfloatingdictionary.room.entities.EntryLessEqualToL;
import com.meta4projects.fldfloatingdictionary.room.entities.EntryWords;

@Database(entities = {EntryLessEqualToL.class, EntryGreaterThanL.class, EntryWords.class}, version = 3, exportSchema = false)
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
                        DictionaryDatabase.class, "dictionary_database")
                .createFromAsset("WiktionaryDatabase.db").fallbackToDestructiveMigration().build();
    }

    public abstract EntryDao entryDao();

    public abstract EntryWordsDao entryWordsDao();
}
