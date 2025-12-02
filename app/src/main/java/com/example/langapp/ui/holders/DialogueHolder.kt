package com.example.langapp.ui.holders

import android.media.MediaPlayer
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.langapp.R
import com.example.langapp.data.model.DialogueLine
import com.example.langapp.data.model.DialogueTask
import com.example.langapp.data.model.DialogueSet

class DialogueHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val cardView: CardView = view.findViewById(R.id.cardView)
    private val dialogueImageView: ImageView = view.findViewById(R.id.dialogueImageView)
    private val dialogueTextView: TextView = view.findViewById(R.id.dialogueTextView)
    private val playButton: ImageButton = view.findViewById(R.id.playButton)
    private val prevButton: ImageButton = view.findViewById(R.id.prevButton)
    private val nextButton: ImageButton = view.findViewById(R.id.nextButton)
    private val progressText: TextView = view.findViewById(R.id.progressText)

    private var mediaPlayer: MediaPlayer? = null
    private var currentLineIndex = 0
    private var dialogueLines = emptyList<DialogueLine>()

    fun bind(dialogueTask: DialogueTask) {
        dialogueLines = dialogueTask.dialogue
        currentLineIndex = 0

        // Устанавливаем изображение, если есть
        val hasImage = dialogueTask.image?.isNotEmpty() == true
        if (hasImage) {
            dialogueImageView.visibility = View.VISIBLE
            Glide.with(itemView.context)
                .load(dialogueTask.image)
                .placeholder(R.drawable.placeholder_image)
                .centerInside()
                .into(dialogueImageView)
        } else {
            dialogueImageView.visibility = View.GONE
        }

        // Обновляем отображение текущей реплики
        updateDisplay()

        // Настраиваем кнопки
        setupButtons()

        // Автовоспроизведение если включено
        if (dialogueTask.autoPlay && dialogueLines.isNotEmpty()) {
            playCurrentLine()
        }
    }

    private fun updateDisplay() {
        if (dialogueLines.isEmpty()) {
            dialogueTextView.text = "Диалог пуст"
            progressText.text = "0/0"
            playButton.isEnabled = false
            prevButton.isEnabled = false
            nextButton.isEnabled = false
            return
        }

        val currentLine = dialogueLines[currentLineIndex]
        dialogueTextView.text = currentLine.text
        progressText.text = "${currentLineIndex + 1}/${dialogueLines.size}"

        // Обновляем состояние кнопок навигации
        prevButton.isEnabled = currentLineIndex > 0
        nextButton.isEnabled = currentLineIndex < dialogueLines.size - 1

        // Показываем/скрываем кнопки навигации если есть более 1 реплики
        val showNavigation = dialogueLines.size > 1
        prevButton.visibility = if (showNavigation) View.VISIBLE else View.GONE
        nextButton.visibility = if (showNavigation) View.VISIBLE else View.GONE
    }

    private fun setupButtons() {
        playButton.setOnClickListener {
            playCurrentLine()
        }

        prevButton.setOnClickListener {
            if (currentLineIndex > 0) {
                currentLineIndex--
                updateDisplay()
                playCurrentLine()
            }
        }

        nextButton.setOnClickListener {
            if (currentLineIndex < dialogueLines.size - 1) {
                currentLineIndex++
                updateDisplay()
                playCurrentLine()
            }
        }

        // Клик по карточке воспроизводит текущую реплику
        cardView.setOnClickListener {
            playCurrentLine()
        }

        // Клик по тексту также воспроизводит
        dialogueTextView.setOnClickListener {
            playCurrentLine()
        }
    }

    private fun playCurrentLine() {
        if (dialogueLines.isEmpty() || currentLineIndex >= dialogueLines.size) {
            return
        }

        val currentLine = dialogueLines[currentLineIndex]

        // Останавливаем предыдущее воспроизведение
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(currentLine.sound)
                prepareAsync()
                setOnPreparedListener {
                    it.start()
                    // Меняем иконку на паузу
                    playButton.setImageResource(R.drawable.ic_pause)
                }
                setOnCompletionListener {
                    playButton.setImageResource(R.drawable.ic_play)

                    // Автоматически переходим к следующей реплике если включено
                    if (currentLineIndex < dialogueLines.size - 1) {
                        currentLineIndex++
                        updateDisplay()
                        // Автовоспроизведение следующей реплики
                        playCurrentLine()
                    }
                }
                setOnErrorListener { mp, what, extra ->
                    playButton.setImageResource(R.drawable.ic_play)
                    mp.release()
                    mediaPlayer = null
                    true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                release()
                mediaPlayer = null
            }
        }
    }

    fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}