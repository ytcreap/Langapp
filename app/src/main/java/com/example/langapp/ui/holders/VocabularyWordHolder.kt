package com.example.langapp.ui.holders

import android.text.method.ScrollingMovementMethod
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
        // Устанавливаем текст (может быть длинным)
        wordTextView.text = vocabularyItem.word

        // Настраиваем TextView для длинного текста
        wordTextView.isSingleLine = false
        wordTextView.maxLines = Int.MAX_VALUE // Не ограничиваем количество строк
        wordTextView.ellipsize = null // Отключаем троеточие в конце
        wordTextView.movementMethod = ScrollingMovementMethod.getInstance() // Добавляем возможность скролла

        // Проверяем, длинный ли это текст (больше 100 символов)
        val isLongText = vocabularyItem.word.length > 100

        if (isLongText) {
            // Для длинного текста увеличиваем отступы
            textContainer.setPadding(
                textContainer.paddingLeft,
                24.dpToPx(itemView.context),
                textContainer.paddingRight,
                24.dpToPx(itemView.context)
            )
            // Увеличиваем размер шрифта для лучшей читаемости
            wordTextView.textSize = 16f
        } else {
            // Для короткого текста используем стандартные отступы
            textContainer.setPadding(
                textContainer.paddingLeft,
                16.dpToPx(itemView.context),
                textContainer.paddingRight,
                16.dpToPx(itemView.context)
            )
            wordTextView.textSize = 18f
        }

        // Проверяем наличие изображения
        val hasImage = vocabularyItem.image.isNotEmpty()

        if (hasImage) {
            // Показываем изображение
            wordImageView.visibility = View.VISIBLE
            wordImageView.layoutParams.height = 200.dpToPx(itemView.context)

            // Загружаем картинку
            Glide.with(itemView.context)
                .load(vocabularyItem.image)
                .placeholder(R.drawable.placeholder_image)
                .centerInside()
                .into(wordImageView)

            // Для карточек с изображением уменьшаем отступы текста
            textContainer.setPadding(
                textContainer.paddingLeft,
                12.dpToPx(itemView.context),
                textContainer.paddingRight,
                12.dpToPx(itemView.context)
            )
        } else {
            // Скрываем изображение
            wordImageView.visibility = View.GONE
            wordImageView.layoutParams.height = 0
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
    }
}

// Extension для преобразования dp в px
private fun Int.dpToPx(context: android.content.Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}