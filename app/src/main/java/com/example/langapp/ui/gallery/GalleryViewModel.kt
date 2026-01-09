package com.example.langapp.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.langapp.data.model.Section
import com.example.langapp.data.model.Task
import com.example.langapp.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GalleryViewModel : ViewModel() {
    private val repository = FirebaseRepository.getInstance()

    private val _availableSections = MutableStateFlow<List<Section>>(emptyList())
    val availableSections: StateFlow<List<Section>> = _availableSections.asStateFlow()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Загрузка доступных разделов для урока
    fun loadAvailableSections(level: String, lesson: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAvailableSections(level, lesson).collect { sections ->
                _availableSections.value = sections
                _isLoading.value = false
            }
        }
    }

    // Загрузка задач из раздела
    fun loadTasks(level: String, lesson: Int, sectionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getTasks(level, lesson, sectionId).collect { tasks ->
                _tasks.value = tasks
                _isLoading.value = false
            }
        }
    }

    // В файле GalleryViewModel.kt
    fun checkIfLessonExists(level: String, lesson: Int, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val repository = FirebaseRepository.getInstance()
                repository.checkIfLessonExists(level, lesson) { exists ->
                    callback(exists)
                }
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    fun getFirstSectionOfLesson(level: String, lesson: Int): Flow<Section?> {
        val repository = FirebaseRepository.getInstance()
        return repository.getFirstSectionOfLesson(level, lesson)
    }


}