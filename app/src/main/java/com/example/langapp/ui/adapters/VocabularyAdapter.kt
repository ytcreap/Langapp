package com.example.langapp.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.VocabularyItem
import com.example.langapp.ui.holders.VocabularyWordHolder

class VocabularyAdapter(
    private val items: List<VocabularyItem>,
    private val onPlayClick: (String) -> Unit,
    private val onItemClick: ((VocabularyItem) -> Unit)? = null
) : RecyclerView.Adapter<VocabularyWordHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VocabularyWordHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vocabulary_word, parent, false)
        return VocabularyWordHolder(view)
    }

    override fun onBindViewHolder(holder: VocabularyWordHolder, position: Int) {
        val item = items[position]
        holder.bind(item, onPlayClick)
    }

    override fun getItemCount(): Int = items.size
}