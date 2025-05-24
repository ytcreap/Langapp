package com.example.langapp

import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.ui.adapters.PhoneticsAdapter
import com.example.langapp.ui.adapters.VocabularyAdapter
import com.example.langapp.data.model.PhoneticTask
import com.example.langapp.data.model.Task
import com.example.langapp.data.model.VocabularyTask
import android.content.Context

object AdapterFactory {

    fun createAdapter(
        taskType: String,
        items: List<Task>,
        context: Context
    ): RecyclerView.Adapter<*> {
        return when (taskType) { // Уже получаем нормализованное значение
            "фонетика" -> PhoneticsAdapter(items.filterIsInstance<PhoneticTask>())
            "лексика" -> VocabularyAdapter(items.filterIsInstance<VocabularyTask>(), context)
            else -> throw IllegalArgumentException("Invalid type: $taskType")
        }
    }
    private fun String.normalized() = this
        .trim()
        .replace("[^A-Za-zА-Яа-я]".toRegex(), "")
        .lowercase()
}
