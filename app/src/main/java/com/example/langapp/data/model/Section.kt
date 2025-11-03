package com.example.langapp.data.model

data class Section(
    val id: String,
    val name: String,
    val description: String,
    val tasks: List<Task> = emptyList()
)