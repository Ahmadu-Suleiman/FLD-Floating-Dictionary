package com.meta4projects.fldfloatingdictionary.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StringArrayConverter {
    @TypeConverter
    fun fromStringArray(stringArray: AttributeList?): String? {
        if (stringArray == null) return null
        val gson = Gson()
        val type = object : TypeToken<AttributeList?>() {}.type
        return gson.toJson(stringArray, type)
    }

    @TypeConverter
    fun toStringArray(value: String?): AttributeList? {
        if (value == null) return null
        val gson = Gson()
        val type = object : TypeToken<AttributeList?>() {}.type
        return gson.fromJson(value, type)
    }
}