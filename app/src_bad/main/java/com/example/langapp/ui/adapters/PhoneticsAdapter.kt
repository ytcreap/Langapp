package com.example.langapp.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.PhoneticTask

class PhoneticsAdapter(
    private val items: List<PhoneticTask>,
) : RecyclerView.Adapter<PhoneticsAdapter.ViewHolder>() {

    private var audioClickListener: ((String) -> Unit)? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvWord: TextView = view.findViewById(R.id.tv_phonetic_word)
        val tvIpa: TextView = view.findViewById(R.id.tv_ipa)
        val btnAudio: ImageButton = view.findViewById(R.id.btn_audio)
        val tvHint: TextView = view.findViewById(R.id.tv_hint)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_phonetic, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size;
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.apply {
            tvWord.text = item.word
            tvIpa.text = item.ipa
            tvHint.text = item.hint
            btnAudio.setOnClickListener {
                audioClickListener?.invoke(item.audio)
            }
        }
    }

    fun setOnAudioClickListener(listener: (String) -> Unit) {
        audioClickListener = listener
    }
}
