package com.example.langapp.data.model

import com.google.firebase.database.PropertyName

data class MultipleChoiceTask(
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("number") override val number: Int,
    @get:PropertyName("question") val question: String,
    @get:PropertyName("options") val options: List<String>,
    @get:PropertyName("correctAnswer") val correctAnswer: String,
    @get:PropertyName("explanation") val explanation: String? = null
) : Task() {
    override val type = TaskType.MULTIPLE_CHOICE
}
