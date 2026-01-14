// HomeViewModel.kt
package com.example.langapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.langapp.data.repository.FirebaseRepository
import com.example.langapp.utils.UserProfileManager
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = FirebaseRepository.getInstance()

    // LiveData из менеджера
    val userProfile = UserProfileManager.userProfile

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> = _updateSuccess

    init {
        loadUserData()
    }

    fun loadUserData() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // Просто загружаем данные через менеджер
                UserProfileManager.loadUserData()
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки данных: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        loadUserData()
    }

    /**
     * Обновляет данные профиля пользователя
     */
    fun updateProfile(fullName: String, group: String) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // Обновляем через менеджер
                UserProfileManager.updateUserData(fullName, group)
                _updateSuccess.value = true
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Ошибка: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Сбрасывает статус успешного обновления
     */
    fun resetUpdateStatus() {
        _updateSuccess.value = false
    }

    /**
     * Проверяет, является ли пользователь зарегистрированным через Google
     */
    fun isGoogleUser(): Boolean {
        return UserProfileManager.isGoogleUser()
    }

    /**
     * Обновляет email пользователя
     */
    suspend fun updateEmail(currentEmail: String, newEmail: String, password: String): Boolean {
        return UserProfileManager.updateEmail(currentEmail, newEmail, password)
    }

    /**
     * Обновляет пароль пользователя
     */
    suspend fun updatePassword(currentEmail: String, currentPassword: String, newPassword: String): Boolean {
        return UserProfileManager.updatePassword(currentEmail, currentPassword, newPassword)
    }

    /**
     * Получает текущий email пользователя
     */
    fun getCurrentEmail(): String {
        return userProfile.value?.email ?: ""
    }
}