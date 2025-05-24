package com.example.langapp.ui.holders
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.*

class AudioRecordingHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val playButton: ImageButton = view.findViewById(R.id.playButton)
    private val recordButton: Button = view.findViewById(R.id.recordButton)
    private val attemptsText: TextView = view.findViewById(R.id.attemptsText)

    fun bind(task: AudioRecordingTask, onClick: (AudioRecordingTask) -> Unit) {
        playButton.setOnClickListener { /* Play prompt */ }
        recordButton.setOnClickListener { onClick(task) }
        attemptsText.text = "Попыток: ${task.maxAttempts}"
    }
}

