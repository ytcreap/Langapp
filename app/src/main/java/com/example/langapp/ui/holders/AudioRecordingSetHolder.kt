// ui/holders/AudioRecordingSetHolder.kt
package com.example.langapp.ui.holders

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.AudioRecordingSet
import com.example.langapp.data.model.Task
import com.example.langapp.ui.adapters.UniversalTaskAdapter

class AudioRecordingSetHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val titleText: TextView = view.findViewById(R.id.titleText)
    private val tasksRecyclerView: RecyclerView = view.findViewById(R.id.tasksRecyclerView)

    fun bind(audioRecordingSet: AudioRecordingSet, onItemClick: (Task) -> Unit) {
        titleText.text = audioRecordingSet.taskname

        val adapter = UniversalTaskAdapter(audioRecordingSet.tasks, onItemClick)
        tasksRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
        tasksRecyclerView.adapter = adapter
    }
}