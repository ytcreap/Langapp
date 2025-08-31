package com.example.langapp.ui.holders

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.TextInputTask

class TextInputHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val promptText: TextView = view.findViewById(R.id.promptText)
    private val inputField: EditText = view.findViewById(R.id.inputField)
    private val audioButton: ImageButton = view.findViewById(R.id.audioButton)
    private val submitButton: Button = view.findViewById(R.id.submitButton)
    private val resultText: TextView = view.findViewById(R.id.resultText)

    fun bind(task: TextInputTask) {
        promptText.text = task.question
        inputField.setText("")
        resultText.isVisible = false
        submitButton.isEnabled = true

        audioButton.visibility = if (task.audioSupport != null) View.VISIBLE else View.GONE

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
}