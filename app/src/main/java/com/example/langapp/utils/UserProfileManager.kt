// UserProfileManager.kt
package com.example.langapp.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.langapp.data.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth

object UserProfileManager {

    private val repository = FirebaseRepository.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // LiveData для данных пользователя
    private val _userProfile = MutableLiveData<SimpleUserProfile?>()
    val userProfile: LiveData<SimpleUserProfile?> = _userProfile

    data class SimpleUserProfile(
        val fullName: String,
        val group: String,
        val email: String = ""
    )

    init {
        // Настраиваем слушатель изменений авторизации
        auth.addAuthStateListener { firebaseAuth ->
            loadUserData()
        }

        // Загружаем данные при инициализации
        loadUserData()
    }

    fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            repository.loadCurrentUserProfile { profile ->
                if (profile != null) {
                    _userProfile.postValue(
                        SimpleUserProfile(
                            fullName = profile.fullName,
                            group = profile.group,
                            email = profile.email
                        )
                    )
                } else {
                    // Если профиля нет, используем данные из FirebaseAuth
                    val displayName = currentUser.displayName ?:
                    currentUser.email?.split("@")?.first() ?: "Пользователь"
                    _userProfile.postValue(
                        SimpleUserProfile(
                            fullName = displayName,
                            group = "Зарегистрирован",
                            email = currentUser.email ?: ""
                        )
                    )
                }
            }
        } else {
            _userProfile.postValue(null)
        }
    }

    fun updateUserData(fullName: String, group: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            repository.updateUserProfile(currentUser.uid, fullName, group) { success, _ ->
                if (success) {
                    // Обновляем локальные данные
                    _userProfile.postValue(
                        SimpleUserProfile(
                            fullName = fullName,
                            group = group,
                            email = currentUser.email ?: ""
                        )
                    )
                }
            }
        }
    }

    fun clearUserData() {
        _userProfile.postValue(null)
    }

    fun isGoogleUser(): Boolean {
        return repository.isGoogleUser()
    }

    suspend fun updateEmail(currentEmail: String, newEmail: String, password: String): Boolean {
        return repository.updateUserEmail(currentEmail, newEmail, password)
    }

    suspend fun updatePassword(currentEmail: String, currentPassword: String, newPassword: String): Boolean {
        return repository.updateUserPassword(currentEmail, currentPassword, newPassword)
    }
}