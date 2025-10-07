package com.example.langapp.ui.holders

import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.AudioRecordingTask

class AudioRecordingHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val questionText: TextView = view.findViewById(R.id.questionText)
    private val textHint: TextView = view.findViewById(R.id.textHint)
    private val playButton: ImageButton = view.findViewById(R.id.playButton)
    private val recordButton: Button = view.findViewById(R.id.recordButton)
    private val attemptsText: TextView = view.findViewById(R.id.attemptsText)
    private val recordingStatus: TextView = view.findViewById(R.id.recordingStatus)

    private var attempts = 0
    private var isRecording = false

    fun bind(task: AudioRecordingTask, onClick: (AudioRecordingTask) -> Unit) {
        // Устанавливаем вопрос
        questionText.text = task.question

        // Устанавливаем текстовую подсказку, если есть
        task.textHint?.let { hint ->
            textHint.text = "Подсказка: $hint"
            textHint.visibility = View.VISIBLE
        }

        // Обновляем счетчик попыток
        updateAttemptsText(task.maxAttempts)

        // Обработчик кнопки воспроизведения
        playButton.setOnClickListener {
            // TODO: Реализовать воспроизведение audioPrompt
            playAudio(task.audioPrompt)
        }

        // Обработчик кнопки записи
        recordButton.setOnClickListener {
            if (attempts < task.maxAttempts) {
                if (!isRecording) {
                    startRecording()
                } else {
                    stopRecording()
                    attempts++
                    updateAttemptsText(task.maxAttempts)

                    // Проверяем, остались ли попытки
                    if (attempts >= task.maxAttempts) {
                        recordButton.isEnabled = false
                        recordButton.text = "Попытки закончились"
                    }

                    // Вызываем колбэк для обработки записи
                    onClick(task)
                }
            }
        }
    }

    private fun playAudio(audioResource: String) {
        // TODO: Реализовать воспроизведение аудио из ресурсов или Firebase Storage
        // Например: MediaPlayer.create(context, getResourceId(audioResource)).start()
    }

    private fun startRecording() {
        isRecording = true
        recordButton.text = "Остановить запись"
        recordingStatus.text = "Запись..."
        recordingStatus.visibility = View.VISIBLE
        // TODO: Запустить запись аудио
    }

    private fun stopRecording() {
        isRecording = false
        recordButton.text = "Записать"
        recordingStatus.visibility = View.GONE
        // TODO: Остановить запись аудио
    }

    private fun updateAttemptsText(maxAttempts: Int) {
        attemptsText.text = "Попыток: $attempts/$maxAttempts"
    }
}