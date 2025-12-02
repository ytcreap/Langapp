package com.example.langapp.ui.holders

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.langapp.R
import com.example.langapp.data.model.VocabularyItem

class VocabularyWordHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val cardView: CardView = view.findViewById(R.id.cardView)
    private val wordImageView: ImageView = view.findViewById(R.id.wordImageView)
    private val wordTextView: TextView = view.findViewById(R.id.wordTextView)
    private val playButton: ImageButton = view.findViewById(R.id.playButton)
    private val textContainer: LinearLayout = view.findViewById(R.id.textContainer)

    fun bind(
        vocabularyItem: VocabularyItem,
        onPlayClick: (String) -> Unit
    ) {
        // Устанавливаем слово
        wordTextView.text = vocabularyItem.word

        // Проверяем наличие изображения
        val hasImage = vocabularyItem.image.isNotEmpty()

        if (hasImage) {
            // Показываем изображение и делаем карточку высокой
            wordImageView.visibility = View.VISIBLE
            wordImageView.layoutParams.height = 200.dpToPx(itemView.context)

            // Загружаем картинку
            Glide.with(itemView.context)
                .load(vocabularyItem.image)
                .placeholder(R.drawable.placeholder_image)
                .centerInside()
                .into(wordImageView)
        } else {
            // Скрываем изображение, делаем карточку компактной
            wordImageView.visibility = View.GONE
            wordImageView.layoutParams.height = 0

            // Увеличиваем отступы текстового контейнера
            textContainer.setPadding(
                textContainer.paddingLeft,
                16.dpToPx(itemView.context),
                textContainer.paddingRight,
                16.dpToPx(itemView.context)
            )
        }

        // Функция воспроизведения аудио
        val playAudio = {
            if (vocabularyItem.audio.isNotEmpty()) {
                onPlayClick(vocabularyItem.audio)
            }
        }

        // Обработчики кликов
        playButton.setOnClickListener {
            playAudio.invoke()
        }

        cardView.setOnClickListener {
            // Анимация нажатия
            cardView.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    cardView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()

            playAudio.invoke()
        }

        // Клик на изображение (если оно есть)
        if (hasImage) {
            wordImageView.setOnClickListener {
                playAudio.invoke()
            }
        }

        // Клик на текст
        wordTextView.setOnClickListener {
            playAudio.invoke()
        }

        // Долгое нажатие
        cardView.setOnLongClickListener {
            // Например, добавить в избранное или показать детали
            true
        }
    }
}

// Extension для преобразования dp в px
private fun Int.dpToPx(context: android.content.Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}