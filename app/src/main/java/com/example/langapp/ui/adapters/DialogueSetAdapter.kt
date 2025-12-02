package com.example.langapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.DialogueTask

class DialogueSetAdapter(
    private val items: List<DialogueTask>,
    private val onItemClick: (DialogueTask) -> Unit
) : RecyclerView.Adapter<DialogueSetAdapter.DialogueViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DialogueViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dialogue_card, parent, false)
        return DialogueViewHolder(view)
    }

    override fun onBindViewHolder(holder: DialogueViewHolder, position: Int) {
        val dialogueTask = items[position]
        holder.bind(dialogueTask)

        holder.itemView.setOnClickListener {
            onItemClick(dialogueTask)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class DialogueViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dialogueTitle: TextView = view.findViewById(R.id.dialogueTitle)
        private val dialogueCount: TextView = view.findViewById(R.id.dialogueCount)

        fun bind(dialogueTask: DialogueTask) {
            dialogueTitle.text = dialogueTask.taskname
            val lineCount = dialogueTask.dialogue.size
            dialogueCount.text = when {
                lineCount == 0 -> "Нет реплик"
                lineCount == 1 -> "1 реплика"
                lineCount in 2..4 -> "$lineCount реплики"
                else -> "$lineCount реплик"
            }
        }
    }
}