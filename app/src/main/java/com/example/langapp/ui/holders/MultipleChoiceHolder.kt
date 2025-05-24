package com.example.langapp.ui.holders

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.MultipleChoiceTask

class MultipleChoiceHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val questionText: TextView = view.findViewById(R.id.questionText)
    private val optionsContainer: LinearLayout = view.findViewById(R.id.optionsContainer)
    private val audioHintButton: ImageButton = view.findViewById(R.id.audioHintButton)

    fun bind(task: MultipleChoiceTask, onClick: (MultipleChoiceTask) -> Unit) {
        questionText.text = task.question
        optionsContainer.removeAllViews()

        task.options.forEachIndexed { index, option ->
            val optionView = LayoutInflater.from(itemView.context)
                .inflate(R.layout.item_option, optionsContainer, false)
            optionView.findViewById<TextView>(R.id.optionText).text = option
            optionView.setOnClickListener {
                onClick(task.copy(selectedIndex = index))
            }
            optionsContainer.addView(optionView)
        }

        audioHintButton.visibility = if (task.audioHint != null) View.VISIBLE else View.GONE
        audioHintButton.setOnClickListener { /* Play audio hint */ }
    }
}
