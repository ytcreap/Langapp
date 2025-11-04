package com.example.langapp.ui.adapters

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.SyllableLetter

class SyllableGroupAdapter(
    private val groups: List<SyllableLetter>
) : RecyclerView.Adapter<SyllableGroupAdapter.GroupViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null

    inner class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val letterText: TextView = view.findViewById(R.id.letterText)
        private val syllablesRecyclerView: RecyclerView = view.findViewById(R.id.syllablesRecyclerView)
        private var syllablesAdapter: SyllableItemAdapter? = null

        fun bind(group: SyllableLetter) {
            letterText.text = group.letter

            // Настраиваем RecyclerView для слогов
            syllablesRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
            syllablesAdapter = SyllableItemAdapter(group.items)
            syllablesRecyclerView.adapter = syllablesAdapter
        }

        fun onDestroy() {
            syllablesAdapter?.onDestroy()
            syllablesAdapter = null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_syllable_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    override fun getItemCount() = groups.size

    fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}