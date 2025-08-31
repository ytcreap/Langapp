package com.example.langapp.ui.holders

import android.app.Dialog
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.langapp.R
import com.example.langapp.data.model.ImageInputTask

class ImageInputHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val questionText: TextView = view.findViewById(R.id.questionText)
    private val taskImage: ImageView = view.findViewById(R.id.taskImage)
    private val inputField: EditText = view.findViewById(R.id.inputField)
    private val hintButton: ImageButton = view.findViewById(R.id.hintButton)
    private val submitButton: ImageButton = view.findViewById(R.id.submitButton)
    private val hintText: TextView = view.findViewById(R.id.hintText)
    private val resultText: TextView = view.findViewById(R.id.resultText)

    fun bind(task: ImageInputTask) {
        // Устанавливаем вопрос
        questionText.text = task.question

        // Загрузка изображения с использованием ресурсов
        val imageName = task.image.substringBeforeLast('.')
        val imageResId = itemView.resources.getIdentifier(imageName, "drawable", itemView.context.packageName)

        if (imageResId != 0) {
            Glide.with(itemView.context)
                .load(imageResId)
                .placeholder(R.drawable.ic_image_placeholder)
                .into(taskImage)
        } else {
            taskImage.setImageResource(R.drawable.ic_image_placeholder)
        }

        // Добавляем возможность открытия изображения в полном размере
        taskImage.setOnClickListener {
            showFullScreenImage(imageResId)
        }

        // Настройка подсказки
        hintText.text = task.hint ?: ""
        hintButton.visibility = if (task.hint != null) View.VISIBLE else View.GONE
        hintButton.setOnClickListener {
            hintText.visibility = if (hintText.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Сброс состояния
        inputField.setText("")
        inputField.isEnabled = true
        resultText.isVisible = false
        submitButton.isEnabled = true

        // Обработка отправки ответа
        submitButton.setOnClickListener {
            val userAnswer = inputField.text.toString().trim()
            if (userAnswer.isEmpty()) {
                return@setOnClickListener
            }

            // Проверяем ответ, игнорируя регистр
            val isCorrect = userAnswer.equals(task.correctAnswer, ignoreCase = true)
            resultText.isVisible = true

            if (isCorrect) {
                resultText.text = "Правильно! ✅"
                resultText.setTextColor(ContextCompat.getColor(itemView.context, R.color.correct_answer))
            } else {
                resultText.text = "Неправильно! ❌\nПравильный ответ: ${task.correctAnswer}"
                resultText.setTextColor(ContextCompat.getColor(itemView.context, R.color.wrong_answer))
            }

            submitButton.isEnabled = false
            inputField.isEnabled = false
        }
    }

    private fun showFullScreenImage(imageResId: Int) {
        if (imageResId == 0) return

        val dialog = Dialog(itemView.context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_fullscreen_image)

        val fullscreenImageView = dialog.findViewById<ImageView>(R.id.fullscreenImageView)
        val closeButton = dialog.findViewById<ImageButton>(R.id.closeButton)

        // Загружаем изображение в полноразмерный режим
        Glide.with(itemView.context)
            .load(imageResId)
            .into(fullscreenImageView)

        // Закрытие диалога при клике на изображение
        fullscreenImageView.setOnClickListener {
            dialog.dismiss()
        }

        // Закрытие диалога при клике на кнопку закрытия
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}