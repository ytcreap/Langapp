package com.example.langapp.ui.holders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.langapp.R
import com.example.langapp.data.model.ImageInputTask
import com.example.langapp.data.model.Task

class ImageInputTaskHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val imageView: ImageView = view.findViewById(R.id.imageView)
    private val questionText: TextView = view.findViewById(R.id.questionText)

    fun bind(task: ImageInputTask, onItemClick: (Task) -> Unit) {
        questionText.text = task.question

        // КРИТИЧЕСКИ ВАЖНО: Загружаете ли вы изображение?
        if (task.image.isNotEmpty()) {
            // Используйте Glide, Picasso или Coil для загрузки
            Glide.with(itemView.context)
                .load(task.image) // Убедитесь, что это правильный URL/path
                .placeholder(R.drawable.placeholder)
                .into(imageView)
        } else {
            imageView.visibility = View.GONE
        }

        itemView.setOnClickListener {
            onItemClick(task)
        }
    }
}