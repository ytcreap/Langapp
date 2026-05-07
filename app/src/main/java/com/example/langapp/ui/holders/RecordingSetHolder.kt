package com.example.langapp.ui.holders

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.ImageRecordingSet
import com.example.langapp.data.model.Task
import com.example.langapp.data.model.TextRecordingSet
import com.example.langapp.ui.adapters.UniversalTaskAdapter
import java.io.File

class RecordingSetHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val titleText: TextView = view.findViewById(R.id.titleText)
    private val tasksRecyclerView: RecyclerView = view.findViewById(R.id.tasksRecyclerView)

    fun bind(
        task: Task,
        onItemClick: (Task) -> Unit,
        onRecordingReady: (Task, File) -> Unit
    ) {
        val tasks = when (task) {
            is ImageRecordingSet -> task.tasks
            is TextRecordingSet -> task.tasks
            else -> emptyList()
        }

        titleText.text = task.taskname
        tasksRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
        tasksRecyclerView.adapter = UniversalTaskAdapter(tasks, onItemClick, onRecordingReady)
    }
}
