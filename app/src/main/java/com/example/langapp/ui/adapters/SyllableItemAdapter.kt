package com.example.langapp.ui.adapters

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.SyllableItem

class SyllableItemAdapter(
    private val items: List<SyllableItem>
) : RecyclerView.Adapter<SyllableItemAdapter.ItemViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val syllableText: TextView = view.findViewById(R.id.syllableText)

        fun bind(item: SyllableItem) {
            syllableText.text = item.text

            itemView.setOnClickListener {
                playSound(item.sound)
            }
        }

        private fun playSound(soundUrl: String) {
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
                mediaPlayer?.release()
                mediaPlayer = null
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_syllable, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}