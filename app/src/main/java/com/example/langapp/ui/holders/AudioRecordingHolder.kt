package com.example.langapp.ui.holders

import android.media.MediaPlayer
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.langapp.R
import com.example.langapp.data.model.AudioRecordingTask

class AudioRecordingHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val questionText: TextView = view.findViewById(R.id.questionText)
    private val questionImage: ImageView = view.findViewById(R.id.questionImage) // Добавляем ImageView
    private val playButton: ImageButton = view.findViewById(R.id.playButton)
    private val recordButton: Button = view.findViewById(R.id.recordButton)
    private val attemptsText: TextView = view.findViewById(R.id.attemptsText)
    private val recordingStatus: TextView = view.findViewById(R.id.recordingStatus)

    private var attempts = 0
    private var isRecording = false
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    fun bind(task: AudioRecordingTask, onClick: (AudioRecordingTask) -> Unit) {
        // Устанавливаем вопрос
        questionText.text = task.question

        // Показываем изображение, если есть
        task.image?.let { imageUrl ->
            questionImage.visibility = View.VISIBLE
            Glide.with(itemView.context)
                .load(imageUrl)
                .centerCrop()
                .into(questionImage)
        } ?: run {
            questionImage.visibility = View.GONE
        }

        // Устанавливаем текстовую подсказку, если есть
        task.textHint?.let { hint ->
            //textHint.text = "Подсказка: $hint"
            //textHint.visibility = View.VISIBLE
        }

        // Обновляем счетчик попыток
        updateAttemptsText(task.maxAttempts)

        // Обработчик кнопки воспроизведения
        playButton.setOnClickListener {
            if (isPlaying) {
                stopAudio()
            } else {
                playAudio(task.audioPrompt)
            }
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

    private fun playAudio(audioUrl: String) {
        // Останавливаем предыдущее воспроизведение
        stopAudio()

        mediaPlayer = MediaPlayer()
        isPlaying = true
        playButton.setImageResource(R.drawable.ic_pause) // Меняем иконку на паузу

        try {
            mediaPlayer?.apply {
                setDataSource(audioUrl)
                setOnPreparedListener {
                    start()
                    // Можно добавить индикатор загрузки
                }
                setOnCompletionListener {
                    stopAudio()
                }
                setOnErrorListener { _, what, extra ->
                    stopAudio()
                    // Показать сообщение об ошибке
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopAudio()
            // Показать сообщение об ошибке воспроизведения
        }
    }

    private fun stopAudio() {
        isPlaying = false
        playButton.setImageResource(R.drawable.ic_play) // Возвращаем иконку воспроизведения

        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
        mediaPlayer = null
    }

    private fun startRecording() {
        // Останавливаем воспроизведение перед записью
        stopAudio()

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

    // Метод для очистки ресурсов при переиспользовании холдера
    fun onDestroy() {
        stopAudio()
    }
}