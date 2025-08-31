package com.example.langapp.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.*
import com.example.langapp.ui.holders.*

class UniversalTaskAdapter(
    private val items: List<Task>,
    private val onItemClick: (Task) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is MultipleChoiceTask -> VIEW_TYPE_MULTIPLE_CHOICE
            is ImageInputTask -> VIEW_TYPE_IMAGE_INPUT
            is TextInputTask -> VIEW_TYPE_TEXT_INPUT
            is AudioInputTask -> VIEW_TYPE_AUDIO_INPUT
            is ImageAudioTask -> VIEW_TYPE_IMAGE_AUDIO
            is ImageRecordingTask -> VIEW_TYPE_IMAGE_RECORDING
            is AudioRecordingTask -> VIEW_TYPE_AUDIO_RECORDING
            is TextRecordingTask -> VIEW_TYPE_TEXT_RECORDING
            is TheoryTask -> VIEW_TYPE_THEORY
            else -> throw IllegalArgumentException("Unknown task type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_MULTIPLE_CHOICE -> MultipleChoiceHolder(inflater.inflate(R.layout.holder_multiple_choice, parent, false))
            VIEW_TYPE_IMAGE_INPUT -> ImageInputHolder(inflater.inflate(R.layout.holder_image_input, parent, false))
            VIEW_TYPE_TEXT_INPUT -> TextInputHolder(inflater.inflate(R.layout.holder_text_input, parent, false))
            VIEW_TYPE_AUDIO_INPUT -> AudioInputHolder(inflater.inflate(R.layout.holder_audio_input, parent, false))
            VIEW_TYPE_IMAGE_AUDIO -> ImageAudioHolder(inflater.inflate(R.layout.holder_image_audio, parent, false))
            VIEW_TYPE_IMAGE_RECORDING -> ImageRecordingHolder(inflater.inflate(R.layout.holder_image_recording, parent, false))
            VIEW_TYPE_AUDIO_RECORDING -> AudioRecordingHolder(inflater.inflate(R.layout.holder_audio_recording, parent, false))
            VIEW_TYPE_TEXT_RECORDING -> TextRecordingHolder(inflater.inflate(R.layout.holder_text_record, parent, false))
            VIEW_TYPE_THEORY -> TheoryHolder(inflater.inflate(R.layout.holder_theory, parent, false))
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is MultipleChoiceHolder -> holder.bind(item as MultipleChoiceTask)
            is ImageInputHolder -> holder.bind(item as ImageInputTask)
            is TextInputHolder -> holder.bind(item as TextInputTask)
            is AudioInputHolder -> holder.bind(item as AudioInputTask, onItemClick)
            is ImageAudioHolder -> holder.bind(item as ImageAudioTask, onItemClick)
            is ImageRecordingHolder -> holder.bind(item as ImageRecordingTask, onItemClick)
            is AudioRecordingHolder -> holder.bind(item as AudioRecordingTask, onItemClick)
            is TextRecordingHolder -> holder.bind(item as TextRecordingTask, onItemClick)
            is TheoryHolder -> holder.bind(item as TheoryTask, onItemClick)
        }
    }

    override fun getItemCount() = items.size

    companion object {
        const val VIEW_TYPE_MULTIPLE_CHOICE = 1
        const val VIEW_TYPE_IMAGE_INPUT = 2
        const val VIEW_TYPE_TEXT_INPUT = 3
        const val VIEW_TYPE_AUDIO_INPUT = 4
        const val VIEW_TYPE_IMAGE_AUDIO = 5
        const val VIEW_TYPE_IMAGE_RECORDING = 6
        const val VIEW_TYPE_AUDIO_RECORDING = 7
        const val VIEW_TYPE_TEXT_RECORDING = 8
        const val VIEW_TYPE_THEORY = 9
    }
}
