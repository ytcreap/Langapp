package com.example.langapp.data.model

import com.example.langapp.data.model.Task
import com.example.langapp.data.model.TaskType
import com.google.firebase.database.PropertyName

data class ImageInputTask(
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("number") override val number: Int,
    @get:PropertyName("imageUrl") val imageUrl: String,
    @get:PropertyName("acceptableAnswers") val acceptableAnswers: List<String>,
    @get:PropertyName("hint") val hint: String? = null
) : Task() {
    override val type = TaskType.IMAGE_INPUT
}
