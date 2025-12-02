package com.example.langapp.ui.holders

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.ImageAudioSet
import com.example.langapp.data.model.Task
import com.example.langapp.ui.adapters.UniversalTaskAdapter

class ImageAudioSetHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val titleText: TextView = view.findViewById(R.id.titleText)
    private val tasksRecyclerView: RecyclerView = view.findViewById(R.id.tasksRecyclerView)

    fun bind(imageAudioSet: ImageAudioSet, onItemClick: (Task) -> Unit) {
        // Устанавливаем заголовок набора
        titleText.text = imageAudioSet.taskname

        // Настраиваем RecyclerView для подзадач
        val adapter = UniversalTaskAdapter(imageAudioSet.tasks, onItemClick)
        tasksRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
        tasksRecyclerView.adapter = adapter
    }
}