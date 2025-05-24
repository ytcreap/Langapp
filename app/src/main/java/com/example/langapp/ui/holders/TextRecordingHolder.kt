package com.example.langapp.ui.holders
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.*

class TextRecordingHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val textView: TextView = view.findViewById(R.id.textView)
    private val phoneticHint: TextView = view.findViewById(R.id.phoneticHint)
    private val recordButton: Button = view.findViewById(R.id.recordButton)
    private val referenceButton: ImageButton = view.findViewById(R.id.referenceButton)

    fun bind(task: TextRecordingTask, onClick: (TextRecordingTask) -> Unit) {
        textView.text = task.text
        phoneticHint.text = task.phoneticHint
        recordButton.setOnClickListener { onClick(task) }
        referenceButton.setOnClickListener { /* Play reference audio */ }
    }
}

