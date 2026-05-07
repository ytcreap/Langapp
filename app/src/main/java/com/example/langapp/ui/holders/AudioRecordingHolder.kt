package com.example.langapp.ui.holders

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.langapp.R
import com.example.langapp.data.model.AudioRecordingTask
import com.example.langapp.data.model.Task
import com.example.langapp.ui.recording.AudioRecorder
import java.io.File

class AudioRecordingHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val questionText: TextView = view.findViewById(R.id.questionText)
    private val questionImage: ImageView = view.findViewById(R.id.questionImage)
    private val playButton: ImageButton = view.findViewById(R.id.playButton)
    private val recordButton: Button = view.findViewById(R.id.recordButton)
    private val attemptsText: TextView = view.findViewById(R.id.attemptsText)
    private val recordingStatus: TextView = view.findViewById(R.id.recordingStatus)
    private val resultText: TextView? = view.findViewById(R.id.resultText)

    private val audioRecorder = AudioRecorder(view.context)
    private var attempts = 0
    private var isRecording = false
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    fun bind(
        task: AudioRecordingTask,
        onClick: (Task) -> Unit,
        onRecordingReady: (Task, File) -> Unit
    ) {
        questionText.text = task.question
        attempts = 0
        updateAttemptsText(task.maxAttempts)
        recordButton.isEnabled = true
        recordButton.text = "Записать"
        recordingStatus.visibility = View.GONE
        resultText?.visibility = View.GONE

        if (task.image.isNullOrBlank()) {
            questionImage.visibility = View.GONE
        } else {
            questionImage.visibility = View.VISIBLE
            Glide.with(itemView.context).load(task.image).centerCrop().into(questionImage)
        }

        playButton.visibility = if (task.audioPrompt.isBlank()) View.GONE else View.VISIBLE
        playButton.setOnClickListener {
            if (isPlaying) stopAudio() else playAudio(task.audioPrompt)
        }

        recordButton.setOnClickListener {
            if (attempts >= task.maxAttempts) {
                return@setOnClickListener
            }

            if (isRecording) {
                val audioFile = stopRecording()
                attempts++
                updateAttemptsText(task.maxAttempts)
                if (attempts >= task.maxAttempts) {
                    recordButton.isEnabled = false
                    recordButton.text = "Попытки закончились"
                }
                if (audioFile != null) {
                    recordingStatus.text = "Отправляем запись..."
                    recordingStatus.visibility = View.VISIBLE
                    onRecordingReady(task, audioFile)
                }
            } else {
                startRecording(task)
            }
        }

        itemView.setOnClickListener { onClick(task) }
    }

    private fun playAudio(audioUrl: String) {
        stopAudio()
        mediaPlayer = MediaPlayer()
        isPlaying = true
        playButton.setImageResource(R.drawable.ic_pause)

        try {
            mediaPlayer?.apply {
                setDataSource(audioUrl)
                setOnPreparedListener { start() }
                setOnCompletionListener { stopAudio() }
                setOnErrorListener { _, _, _ ->
                    stopAudio()
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopAudio()
        }
    }

    private fun stopAudio() {
        isPlaying = false
        playButton.setImageResource(R.drawable.ic_play)
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
        mediaPlayer = null
    }

    private fun startRecording(task: AudioRecordingTask) {
        if (ContextCompat.checkSelfPermission(itemView.context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            recordingStatus.text = "Разрешите доступ к микрофону"
            recordingStatus.visibility = View.VISIBLE
            return
        }

        stopAudio()
        try {
            audioRecorder.start("audio_${task.id}")
            isRecording = true
            recordButton.text = "Остановить запись"
            recordingStatus.text = "Идет запись..."
            recordingStatus.visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
            recordingStatus.text = "Не удалось начать запись"
            recordingStatus.visibility = View.VISIBLE
        }
    }

    private fun stopRecording(): File? {
        isRecording = false
        recordButton.text = "Записать"
        val file = audioRecorder.stop()
        if (file == null) {
            recordingStatus.text = "Запись не сохранена"
            recordingStatus.visibility = View.VISIBLE
        }
        return file
    }

    private fun updateAttemptsText(maxAttempts: Int) {
        attemptsText.text = "Попыток: $attempts/$maxAttempts"
    }

    fun onDestroy() {
        stopAudio()
        audioRecorder.cancel()
    }
}
