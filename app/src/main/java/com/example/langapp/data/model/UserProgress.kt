package com.example.langapp.data.model

data class UserProgress(
    var completed: Boolean = false,
    var score: Int? = null,
    var completedTasks: List<String> = emptyList()
)