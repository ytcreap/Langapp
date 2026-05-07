package com.example.langapp.ui.holders

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.Task
import com.example.langapp.data.model.TextRecordingTask
import com.example.langapp.ui.recording.AudioRecorder
import java.io.File

class TextRecordingHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val textView: TextView = view.findViewById(R.id.textView)
    private val phoneticHint: TextView = view.findViewById(R.id.phoneticHint)
    private val recordButton: Button = view.findViewById(R.id.recordButton)
    private val referenceButton: ImageButton = view.findViewById(R.id.referenceButton)
    private val statusText: TextView = view.findViewById(R.id.recordingStatus)

    private val audioRecorder = AudioRecorder(view.context)
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false

    fun bind(
        task: TextRecordingTask,
        onClick: (Task) -> Unit,
        onRecordingReady: (Task, File) -> Unit
    ) {
        textView.text = task.text
        phoneticHint.text = task.phoneticHint
        phoneticHint.visibility = if (task.phoneticHint.isBlank()) View.GONE else View.VISIBLE
        statusText.visibility = View.GONE
        recordButton.text = "Записать"

        referenceButton.visibility = if (task.referenceAudio.isBlank()) View.GONE else View.VISIBLE
        referenceButton.setOnClickListener { playAudio(task.referenceAudio) }

        recordButton.setOnClickListener {
            if (isRecording) {
                val audioFile = stopRecording()
                if (audioFile != null) {
                    statusText.text = "Отправляем запись..."
                    statusText.visibility = View.VISIBLE
                    onRecordingReady(task, audioFile)
                }
            } else {
                startRecording(task)
            }
        }

        itemView.setOnClickListener { onClick(task) }
    }

    private fun startRecording(task: TextRecordingTask) {
        if (ContextCompat.checkSelfPermission(itemView.context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            statusText.text = "Разрешите доступ к микрофону"
            statusText.visibility = View.VISIBLE
            return
        }

        stopAudio()
        try {
            audioRecorder.start("text_${task.id}")
            isRecording = true
            recordButton.text = "Остановить запись"
            statusText.text = "Идет запись..."
            statusText.visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
            statusText.text = "Не удалось начать запись"
            statusText.visibility = View.VISIBLE
        }
    }

    private fun stopRecording(): File? {
        isRecording = false
        recordButton.text = "Записать"
        val file = audioRecorder.stop()
        if (file == null) {
            statusText.text = "Запись не сохранена"
            statusText.visibility = View.VISIBLE
        }
        return file
    }

    private fun playAudio(audioUrl: String) {
        stopAudio()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioUrl)
                setOnPreparedListener { start() }
                setOnCompletionListener { stopAudio() }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopAudio()
        }
    }

    private fun stopAudio() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
        mediaPlayer = null
    }
}
