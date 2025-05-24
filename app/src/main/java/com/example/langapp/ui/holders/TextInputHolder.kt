package com.example.langapp.ui.holders

import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.*

class TextInputHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val promptText: TextView = view.findViewById(R.id.promptText)
    private val inputField: EditText = view.findViewById(R.id.inputField)
    private val audioButton: ImageButton = view.findViewById(R.id.audioButton)

    fun bind(task: TextInputTask, onClick: (TextInputTask) -> Unit) {
        promptText.text = task.textPrompt
        inputField.doAfterTextChanged { text ->
            onClick(task.copy(userAnswer = text.toString()))
        }
        audioButton.visibility = if (task.audioSupport != null) View.VISIBLE else View.GONE
    }
}
