package com.example.langapp.ui.adapters

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.langapp.R
import com.example.langapp.data.model.SyllableLetter

class SyllableLetterAdapter(
    private val letters: List<SyllableLetter>
) : RecyclerView.Adapter<SyllableLetterAdapter.LetterViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null

    inner class LetterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val letterImage: ImageView = view.findViewById(R.id.letterImage)
        private val letterText: TextView = view.findViewById(R.id.letterText)

        fun bind(letter: SyllableLetter) {
            letterText.text = letter.letter

            // Загружаем изображение (если есть) или используем заглушку
            if (letter.image.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(letter.image)
                    .centerCrop()
                    .override(100, 100)
                    .into(letterImage)
            } else {
                // Заглушка для буквы
                //letterImage.setImageResource(R.drawable.ic_letter_placeholder)
            }

            // При нажатии на букву показываем диалог со слогами
            itemView.setOnClickListener {
                showSyllablesDialog(letter)
            }
        }

        private fun showSyllablesDialog(letter: SyllableLetter) {
            val dialogView = LayoutInflater.from(itemView.context)
                .inflate(R.layout.dialog_syllables, null)

            val titleText = dialogView.findViewById<TextView>(R.id.dialogTitle)
            val syllablesRecyclerView = dialogView.findViewById<RecyclerView>(R.id.syllablesRecyclerView)

            titleText.text = "Буква ${letter.letter}"

            // Настраиваем адаптер для слогов
            syllablesRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
            val adapter = SyllableItemAdapter(letter.items)
            syllablesRecyclerView.adapter = adapter

            val dialog = AlertDialog.Builder(itemView.context)
                .setView(dialogView)
                .setPositiveButton("Закрыть") { dialog, _ ->
                    adapter.onDestroy()
                    dialog.dismiss()
                }
                .create()

            dialog.setOnDismissListener {
                adapter.onDestroy()
            }

            dialog.show()
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

    fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}