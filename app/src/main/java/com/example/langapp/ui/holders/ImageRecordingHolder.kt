package com.example.langapp.ui.holders
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.*
import com.bumptech.glide.Glide

class ImageRecordingHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val imageView: ImageView = view.findViewById(R.id.imageView)
    private val recordButton: Button = view.findViewById(R.id.recordButton)
    private val referenceButton: ImageButton = view.findViewById(R.id.referenceButton)

    fun bind(task: ImageRecordingTask, onClick: (ImageRecordingTask) -> Unit) {
        Glide.with(itemView.context).load(task.image).into(imageView)
        recordButton.setOnClickListener { onClick(task) }
        referenceButton.visibility = if (task.referenceAudio != null) View.VISIBLE else View.GONE
    }
}
