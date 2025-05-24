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

class TheoryHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val titleText: TextView = view.findViewById(R.id.titleText)
    private val contentText: TextView = view.findViewById(R.id.contentText)
    private val imageView: ImageView = view.findViewById(R.id.imageView)
    private val questionsContainer: LinearLayout = view.findViewById(R.id.questionsContainer)

    fun bind(task: TheoryTask, onClick: (TheoryTask) -> Unit) {
        titleText.text = task.title
        contentText.text = task.text
        val imageName = task.image.substringBeforeLast('.')
        val imageResId = itemView.resources.getIdentifier(imageName, "drawable", itemView.context.packageName)

        if (imageResId != 0) {
            Glide.with(itemView.context)
                .load(imageResId)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.placeholder_image)
        }

        questionsContainer.removeAllViews()
        task.interactiveElements.forEach { question ->
            val questionView = LayoutInflater.from(itemView.context)
                .inflate(R.layout.item_question, questionsContainer, false)
            // Настройка интерактивных элементов
            questionsContainer.addView(questionView)
        }
    }
}
