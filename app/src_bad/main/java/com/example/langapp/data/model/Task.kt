package com.example.langapp.data.model

import com.google.firebase.database.PropertyName

sealed class Task {
    abstract val id: String
    abstract val type: String
}

data class PhoneticTask(
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("word") val word: String,
    @get:PropertyName("ipa") val ipa: String,
    @get:PropertyName("audio") val audio: String,
    @get:PropertyName("hint") val hint: String
) : Task() {
    override val type: String = "phonetics"
}

data class VocabularyTask(
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("word") val word: String,
    @get:PropertyName("image") val image: String,
    @get:PropertyName("translations") val translations: Map<String, String>,
    @get:PropertyName("audio") val audio: String? = null
) : Task() {
    override val type: String = "vocabulary"
}
