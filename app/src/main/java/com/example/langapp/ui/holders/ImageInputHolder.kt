package com.example.langapp.ui.holders

import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.ImageInputTask
import com.bumptech.glide.Glide

class ImageInputHolder(view: View) : RecyclerView.ViewHolder(view) {
    // UI элементы
    private val taskImage: ImageView = view.findViewById(R.id.taskImage)
    private val inputField: EditText = view.findViewById(R.id.inputField)
    private val hintButton: ImageButton = view.findViewById(R.id.hintButton)
    private val submitButton: ImageButton = view.findViewById(R.id.submitButton)
    private val hintText: TextView = view.findViewById(R.id.hintText)

    fun bind(task: ImageInputTask, onClick: (ImageInputTask) -> Unit) {
        // Загрузка изображения
        Glide.with(itemView.context)
            .load(task.image)
            .placeholder(R.drawable.ic_image_placeholder)
            .into(taskImage)

        // Настройка подсказки
        hintText.text = task.hint ?: ""
        hintButton.visibility = if (task.hint != null) View.VISIBLE else View.GONE
        hintButton.setOnClickListener {
            hintText.visibility = if (hintText.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Обработка ввода
        inputField.hint = itemView.context.getString(R.string.enter_answer)
        submitButton.setOnClickListener {
            val userAnswer = inputField.text.toString()
            onClick(task.copy(userAnswer = userAnswer))
        }
    }
}
