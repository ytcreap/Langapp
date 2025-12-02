package com.example.langapp.ui.adapters

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.langapp.R
import com.example.langapp.data.model.AlphabetLetter
import java.text.Collator
import java.util.Locale

class AlphabetLetterAdapter(
    private val letters: List<AlphabetLetter>
) : RecyclerView.Adapter<AlphabetLetterAdapter.LetterViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null

    // Сортируем буквы при инициализации
    private val sortedLetters = letters.sortedBy { letter ->
        normalizeLetterForSorting(letter.letter)
    }

    inner class LetterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val letterImage: ImageView = view.findViewById(R.id.letterImage)
        private val letterText: TextView = view.findViewById(R.id.letterText)

        fun bind(letter: AlphabetLetter) {
            letterText.text = letter.letter

            // Загружаем картинку
            if (letter.image.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(letter.image)
                    .centerCrop()
                    .override(100, 100)
                    .into(letterImage)
            } else {
                // Если нет картинки, показываем заглушку или скрываем
                letterImage.setImageResource(R.drawable.placeholder_image)
            }

            // Обработка клика - просто воспроизводим звук
            itemView.setOnClickListener {
                playSound(letter.sound)
            }
        }

        private fun playSound(soundUrl: String) {
            // Останавливаем предыдущее воспроизведение
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer()

            try {
                mediaPlayer?.apply {
                    setDataSource(soundUrl)
                    setOnPreparedListener { it.start() }
                    setOnCompletionListener {
                        it.release()
                        mediaPlayer = null
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mediaPlayer?.release()
                mediaPlayer = null
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LetterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alphabet_letter, parent, false)
        return LetterViewHolder(view)
    }

    override fun onBindViewHolder(holder: LetterViewHolder, position: Int) {
        holder.bind(sortedLetters[position])
    }

    override fun getItemCount() = sortedLetters.size

    fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    /**
     * Нормализация буквы для сортировки
     */
    private fun normalizeLetterForSorting(letter: String): String {
        return letter
            .lowercase(Locale("ru", "RU"))
            .replace("ё", "е")
            .replace("й", "и")
            .replace(Regex("[́`']"), "")
            .trim()
    }
}