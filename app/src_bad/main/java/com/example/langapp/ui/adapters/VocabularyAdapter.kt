package com.example.langapp.ui.adapters

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.VocabularyTask
import com.example.langapp.vocabulary.VocabularyItem


// Адаптер для RecyclerView
class VocabularyAdapter(
    private val items: List<VocabularyTask>,
    private val context: Context
) : RecyclerView.Adapter<VocabularyAdapter.ViewHolder>() {

    private var audioClickListener: ((String) -> Unit)? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivWordImage: ImageView = view.findViewById(R.id.iv_word_image)
        val tvWord: TextView = view.findViewById(R.id.tv_word)
        val btnAudio: ImageButton = view.findViewById(R.id.btn_audio)
        val tvTranslation: TextView = view.findViewById(R.id.tv_translation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vocabulary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder) {
            // Загрузка изображения
            val imageResId = context.resources.getIdentifier(
                item.image,
                "drawable",
                context.packageName
            )
            ivWordImage.setImageResource(if (imageResId != 0) imageResId else R.drawable.placeholder)

            tvWord.text = item.word
            tvTranslation.text = item.translations["en"] ?: ""

            // Обработка аудио
            btnAudio.visibility = if (item.audio != null) View.VISIBLE else View.GONE
            btnAudio.setOnClickListener {
                item.audio?.let { audioResName ->
                    audioClickListener?.invoke(audioResName)
                }
            }
        }
    }

    fun setOnAudioClickListener(listener: (String) -> Unit) {
        audioClickListener = listener
    }

    override fun getItemCount() = items.size
}
