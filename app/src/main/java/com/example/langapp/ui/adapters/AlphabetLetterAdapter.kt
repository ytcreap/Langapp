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

class AlphabetLetterAdapter(
    private val letters: List<AlphabetLetter>
) : RecyclerView.Adapter<AlphabetLetterAdapter.LetterViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null

    inner class LetterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val letterImage: ImageView = view.findViewById(R.id.letterImage)
        private val letterText: TextView = view.findViewById(R.id.letterText)

        fun bind(letter: AlphabetLetter) {
            letterText.text = letter.letter

            // Загружаем картинку квадратной и подгоняем размер
            Glide.with(itemView.context)
                .load(letter.image)
                //.placeholder(R.drawable.ic_letter_placeholder)
                //.error(R.drawable.ic_letter_error)
                .centerCrop()
                .override(100, 100)
                .into(letterImage)

            itemView.setOnClickListener {
                playSound(letter.sound)
            }
        }

        private fun playSound(soundUrl: String) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer()
            try {
                mediaPlayer?.setDataSource(soundUrl)
                mediaPlayer?.setOnPreparedListener { it.start() }
                mediaPlayer?.setOnCompletionListener { it.release() }
                mediaPlayer?.prepareAsync()
            } catch (e: Exception) {
                mediaPlayer?.release()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LetterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alphabet_letter, parent, false)
        return LetterViewHolder(view)
    }

    override fun onBindViewHolder(holder: LetterViewHolder, position: Int) {
        holder.bind(letters[position])
    }

    override fun getItemCount() = letters.size
}