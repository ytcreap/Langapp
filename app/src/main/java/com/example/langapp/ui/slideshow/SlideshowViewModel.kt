package com.example.langapp.ui.slideshow

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SlideshowViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Здесь будет находиться помощь (скорее всего на разных языках)"
    }
    val text: LiveData<String> = _text
}