// ui/holders/MultipleChoiceSetHolder.kt
package com.example.langapp.ui.holders

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.MultipleChoiceSet
import com.example.langapp.data.model.Task

class MultipleChoiceSetHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val titleText: TextView = view.findViewById(R.id.titleText)
    private val tasksContainer: LinearLayout = view.findViewById(R.id.tasksContainer) // Изменено на LinearLayout

    fun bind(multipleChoiceSet: MultipleChoiceSet, onItemClick: (Task) -> Unit) {
        titleText.text = multipleChoiceSet.taskname
        tasksContainer.removeAllViews()

        // Динамически создаем представления для каждой задачи
        multipleChoiceSet.tasks.forEachIndexed { index, task ->
            // Инфлейтим макет для отдельной задачи
            val taskView = LayoutInflater.from(itemView.context)
                .inflate(R.layout.holder_multiple_choice, tasksContainer, false)

            // Настраиваем холдер для этой задачи
            val taskHolder = MultipleChoiceHolder(taskView)
            taskHolder.bind(task)

            tasksContainer.addView(taskView)

            // Добавляем разделитель между задачами (кроме последней)
            if (index < multipleChoiceSet.tasks.size - 1) {
                val divider = View(itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        2
                    ).apply {
                        setMargins(0, 32, 0, 32)
                    }
                    setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.dark_background))
                }
                tasksContainer.addView(divider)
            }
        }
    }
}