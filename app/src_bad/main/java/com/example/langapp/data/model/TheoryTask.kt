package com.example.langapp.data.model

import com.google.firebase.database.PropertyName

data class TheoryTask(
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("number") override val number: Int,
    @get:PropertyName("title") val title: String,
    @get:PropertyName("content") val content: String,
    @get:PropertyName("imageUrl") val imageUrl: String? = null,
    @get:PropertyName("audioUrl") val audioUrl: String? = null
) : Task() {
    override val type = TaskType.THEORY
}
