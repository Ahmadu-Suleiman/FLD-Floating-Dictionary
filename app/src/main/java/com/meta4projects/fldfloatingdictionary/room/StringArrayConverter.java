package com.meta4projects.fldfloatingdictionary.room;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class StringArrayConverter {

    @TypeConverter
    public String fromStringArray(ArrayList<String> stringArray) {
        if (stringArray == null)
            return null;

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        return gson.toJson(stringArray, type);
    }

    @TypeConverter
    public ArrayList<String> toStringArray(String value) {
        if (value == null)
            return null;

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        return gson.fromJson(value, type);
    }
}
