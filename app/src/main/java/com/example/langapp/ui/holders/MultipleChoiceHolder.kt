package com.example.langapp.ui.holders

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.MultipleChoiceTask

class MultipleChoiceHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val questionText: TextView = view.findViewById(R.id.questionText)
    private val optionsContainer: RadioGroup = view.findViewById(R.id.optionsContainer)
    private val submitButton: Button = view.findViewById(R.id.submitButton)
    private val resultText: TextView = view.findViewById(R.id.resultText)
    private var selectedAnswerIndex: Int = -1

    fun bind(task: MultipleChoiceTask) {
        questionText.text = task.question
        optionsContainer.removeAllViews()
        resultText.isVisible = false
        submitButton.isEnabled = true

        // Создаем варианты ответов
        task.options.forEachIndexed { index, option ->
            val radioButton = LayoutInflater.from(itemView.context)
                .inflate(R.layout.item_radio_option, optionsContainer, false) as RadioButton
            radioButton.text = option
            radioButton.id = index
            optionsContainer.addView(radioButton)
        }

        optionsContainer.setOnCheckedChangeListener { _, checkedId ->
            selectedAnswerIndex = checkedId
        }

        submitButton.setOnClickListener {
            if (selectedAnswerIndex == -1) {
                return@setOnClickListener
            }

            val isCorrect = selectedAnswerIndex == task.correctAnswerIndex
            resultText.isVisible = true

            if (isCorrect) {
                resultText.text = "Правильно! ✅"
                resultText.setTextColor(ContextCompat.getColor(itemView.context, R.color.correct_answer))
            } else {
                resultText.text = "Неправильно! ❌\nПравильный ответ: ${task.options[task.correctAnswerIndex]}"
                resultText.setTextColor(ContextCompat.getColor(itemView.context, R.color.wrong_answer))

                task.explanation?.let {
                    resultText.append("\n\nОбъяснение: $it")
                }
            }

            submitButton.isEnabled = false
        }
    }
}