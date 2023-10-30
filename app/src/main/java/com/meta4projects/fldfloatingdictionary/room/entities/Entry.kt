package com.meta4projects.fldfloatingdictionary.room.entities

import com.meta4projects.fldfloatingdictionary.room.AttributeList

abstract class Entry {
    open var entryId = 0
    open var word: String? = null
    open var plural: String? = null
    open var partOfSpeech: String? = null
    open var tenses: String? = null
    open var compare: String? = null
    open var definitions: AttributeList? = null
    open var synonyms: AttributeList? = null
    open var antonyms: AttributeList? = null
    open var hypernyms: AttributeList? = null
    open var hyponyms: AttributeList? = null
    open var homophones: AttributeList? = null
}