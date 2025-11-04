package com.example.langapp.ui.holders

import android.media.MediaPlayer
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.SyllableTask
import com.example.langapp.ui.adapters.SyllableLetterAdapter

class SyllableHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val questionText: TextView = view.findViewById(R.id.questionText)
    private val lettersRecyclerView: RecyclerView = view.findViewById(R.id.lettersRecyclerView)
    private var adapter: SyllableLetterAdapter? = null

    fun bind(task: SyllableTask) {
        // Устанавливаем правильное название задания
        questionText.text = task.taskname

        // Настраиваем RecyclerView для букв (5 колонок как в алфавите)
        lettersRecyclerView.layoutManager = GridLayoutManager(itemView.context, 5)
        adapter = SyllableLetterAdapter(task.letters)
        lettersRecyclerView.adapter = adapter
        lettersRecyclerView.setHasFixedSize(true)
    }

    fun onDestroy() {
        adapter?.onDestroy()
        adapter = null
    }
}