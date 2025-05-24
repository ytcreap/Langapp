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
class AudioInputHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val audioButton: ImageButton = view.findViewById(R.id.audioButton)
    private val hintText: TextView = view.findViewById(R.id.hintText)
    private val inputField: EditText = view.findViewById(R.id.inputField)

    fun bind(task: AudioInputTask, onClick: (AudioInputTask) -> Unit) {
        hintText.text = task.textHint ?: "Прослушайте и введите ответ"
        inputField.doAfterTextChanged { text ->
            onClick(task.copy(userAnswer = text.toString()))
        }
        audioButton.setOnClickListener { /* Play audio prompt */ }
    }
}

