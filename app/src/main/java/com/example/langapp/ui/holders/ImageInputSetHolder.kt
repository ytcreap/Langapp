package com.example.langapp.ui.holders

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.ImageInputSet
import com.example.langapp.data.model.Task
import com.example.langapp.ui.adapters.UniversalTaskAdapter

class ImageInputSetHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val titleText: TextView = view.findViewById(R.id.titleText)
    private val questionText: TextView = view.findViewById(R.id.questionText)
    private val tasksRecyclerView: RecyclerView = view.findViewById(R.id.tasksRecyclerView)

    fun bind(imageInputSet: ImageInputSet, onItemClick: (Task) -> Unit) {
        titleText.text = imageInputSet.taskname
        questionText.text = imageInputSet.question

        val adapter = UniversalTaskAdapter(imageInputSet.tasks, onItemClick)
        tasksRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
        tasksRecyclerView.adapter = adapter
    }
}