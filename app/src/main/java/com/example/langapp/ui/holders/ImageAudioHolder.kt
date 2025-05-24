package com.example.langapp.ui.holders
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.*
import com.bumptech.glide.Glide

class ImageAudioHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val imageView: ImageView = view.findViewById(R.id.imageView)
    private val audioButton: ImageButton = view.findViewById(R.id.audioButton)
    private val questionsContainer: LinearLayout = view.findViewById(R.id.questionsContainer)

    fun bind(task: ImageAudioTask, onClick: (ImageAudioTask) -> Unit) {
        Glide.with(itemView.context).load(task.image).into(imageView)
        audioButton.setOnClickListener { /* Play audio */ }

        questionsContainer.removeAllViews()
        task.questions.forEach { question ->
            val questionView = LayoutInflater.from(itemView.context)
                .inflate(R.layout.holder_image_audio, questionsContainer, false)
            // Настройка вопроса
            questionsContainer.addView(questionView)
        }
    }
}

