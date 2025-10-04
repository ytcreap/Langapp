package com.example.langapp.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.langapp.data.repository.FirebaseRepository
import com.example.langapp.data.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class GalleryViewModel : ViewModel() {

    private val repository = FirebaseRepository.getInstance()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadTasks(level: String, lesson: Int, taskType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getTasks(level, lesson, taskType).collect { tasksList ->
                _tasks.value = tasksList
                _isLoading.value = false
            }
        }
    }
}