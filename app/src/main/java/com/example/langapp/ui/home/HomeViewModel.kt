package com.example.langapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    private val _userData = MutableLiveData<UserProfile>()
    val userData: LiveData<UserProfile> = _userData

    init {
        loadMockData()
    }

    private fun loadMockData() {
        val mockUser = UserProfile(
            fullName = "Алфёров Никита Дмитриевич",
            group = "БПОи-22-04",
            elementaryProgress = 0,
            basicProgress = 3,
            intermediateProgress = 0
        )
        _userData.value = mockUser
    }

    data class UserProfile(
        val fullName: String,
        val group: String,
        val elementaryProgress: Int,
        val basicProgress: Int,
        val intermediateProgress: Int
    )
}
