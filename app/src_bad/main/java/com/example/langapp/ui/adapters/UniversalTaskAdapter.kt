package com.example.langapp.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.Task
import com.example.langapp.data.model.*
import com.example.langapp.databinding.*
import com.example.langapp.di.getRenderer

class UniversalTaskAdapter(
    private val tasks: List<Task>,
    private val onAnswerSubmitted: (taskId: String, answer: String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return tasks[position].type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (TaskType.values()[viewType]) {
            TaskType.MULTIPLE_CHOICE -> {
                val binding = ItemMultipleChoiceBinding.inflate(inflater, parent, false)
                MultipleChoiceViewHolder(binding)
            }
            TaskType.IMAGE_INPUT -> {
                val binding = ItemImageInputBinding.inflate(inflater, parent, false)
                ImageInputViewHolder(binding)
            }
            // Остальные типы аналогично
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val task = tasks[position]
        when (holder) {
            is MultipleChoiceViewHolder -> holder.bind(task as MultipleChoiceTask)
            is ImageInputViewHolder -> holder.bind(task as ImageInputTask)
            // Остальные типы
        }
    }

    override fun getItemCount() = tasks.size

    inner class MultipleChoiceViewHolder(
        private val binding: ItemMultipleChoiceBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: MultipleChoiceTask) {
            binding.tvQuestion.text = task.question
            // Инициализация вариантов ответа
            binding.btnSubmit.setOnClickListener {
                onAnswerSubmitted(task.id, selectedAnswer)
            }
        }
    }

    inner class ImageInputViewHolder(
        private val binding: ItemImageInputBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: ImageInputTask) {
            // Загрузка изображения через Glide/Picasso
            binding.etAnswer.hint = task.hint
            binding.btnSubmit.setOnClickListener {
                onAnswerSubmitted(task.id, binding.etAnswer.text.toString())
            }
        }
    }

    // Остальные ViewHolder'ы по аналогии
}
