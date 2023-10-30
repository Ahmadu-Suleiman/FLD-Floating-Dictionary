package com.meta4projects.fldfloatingdictionary.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.meta4projects.fldfloatingdictionary.room.AttributeList
import com.meta4projects.fldfloatingdictionary.room.StringArrayConverter

@Entity(tableName = "entries_greater_than_L", indices = [Index(name = "index_greater_than_L", value = arrayOf("entry_word"))])
class EntryGreaterThanL : Entry() {
    @ColumnInfo(name = "entry_id")
    @PrimaryKey(autoGenerate = true)
    override var entryId = 0

    @ColumnInfo(name = "entry_word")
    override var word: String? = null

    @ColumnInfo(name = "entry_plural")
    override var plural: String? = null

    @ColumnInfo(name = "entry_part_of_speech")
    override var partOfSpeech: String? = null

    @ColumnInfo(name = "entry_tenses")
    override var tenses: String? = null

    @ColumnInfo(name = "entry_compare")
    override var compare: String? = null

    @TypeConverters(StringArrayConverter::class)
    @ColumnInfo(name = "entry_definitions")
    override var definitions: AttributeList? = null

    @TypeConverters(StringArrayConverter::class)
    @ColumnInfo(name = "entry_synonyms")
    override var synonyms: AttributeList? = null

    @TypeConverters(StringArrayConverter::class)
    @ColumnInfo(name = "entry_antonyms")
    override var antonyms: AttributeList? = null

    @TypeConverters(StringArrayConverter::class)
    @ColumnInfo(name = "entry_hypernyms")
    override var hypernyms: AttributeList? = null

    @TypeConverters(StringArrayConverter::class)
    @ColumnInfo(name = "entry_hyponyms")
    override var hyponyms: AttributeList? = null

    @TypeConverters(StringArrayConverter::class)
    @ColumnInfo(name = "entry_homophones")
    override var homophones: AttributeList? = null
}