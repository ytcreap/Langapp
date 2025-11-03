package com.example.langapp.ui.holders

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.AlphabetTask
import com.example.langapp.ui.adapters.AlphabetLetterAdapter

class AlphabetHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val questionText: TextView = view.findViewById(R.id.questionText)
    private val lettersRecyclerView: RecyclerView = view.findViewById(R.id.lettersRecyclerView)

    fun bind(task: AlphabetTask) {
        questionText.text = task.question

        // Простая сетка 5 колонок
        lettersRecyclerView.layoutManager = GridLayoutManager(itemView.context, 5)
        lettersRecyclerView.adapter = AlphabetLetterAdapter(task.letters)
    }
}