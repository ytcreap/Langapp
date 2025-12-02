package com.example.langapp.ui.holders

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.DialogueLine
import com.example.langapp.data.model.DialogueSet

class DialogueSetHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val titleText: TextView = view.findViewById(R.id.titleText)
    private val questionText: TextView = view.findViewById(R.id.questionText)
    private val dialoguesRecyclerView: RecyclerView = view.findViewById(R.id.dialoguesRecyclerView)

    private var mediaPlayer: MediaPlayer? = null
    private var expandedPosition = -1 // Текущий раскрытый диалог
    private var adapter: ExpandableDialogAdapter? = null

    fun bind(dialogueSet: DialogueSet) {
        titleText.text = dialogueSet.taskname
        questionText.text = dialogueSet.question

        // Преобразуем Map в список диалогов и сортируем
        val dialoguesList = dialogueSet.dialogues.entries
            .sortedBy { it.key } // Сортируем по ключу (dialog0, dialog1, etc)
            .mapIndexed { index, (dialogKey, dialogueLines) ->
                DialogData(
                    id = dialogKey,
                    name = "Диалог ${index + 1}",
                    lines = dialogueLines,
                    position = index
                )
            }

        // Настраиваем RecyclerView
        dialoguesRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(itemView.context)
        dialoguesRecyclerView.setHasFixedSize(false)

        // Создаем адаптер
        adapter = ExpandableDialogAdapter(dialoguesList, ::playAudio, ::onDialogClicked)
        dialoguesRecyclerView.adapter = adapter
    }

    private fun onDialogClicked(position: Int) {
        val previousExpanded = expandedPosition
        expandedPosition = if (position == expandedPosition) -1 else position

        // Обновляем предыдущий раскрытый элемент
        if (previousExpanded != -1) {
            adapter?.notifyItemChanged(previousExpanded)
        }

        // Обновляем текущий элемент
        if (expandedPosition != -1) {
            adapter?.notifyItemChanged(expandedPosition)
        }
    }

    private fun playAudio(audioUrl: String) {
        // Останавливаем предыдущее воспроизведение
        mediaPlayer?.release()
        mediaPlayer = null

        if (audioUrl.isEmpty()) {
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioUrl)
                setOnPreparedListener {
                    it.start()
                }
                setOnCompletionListener {
                    release()
                    mediaPlayer = null
                }
                setOnErrorListener { mp, what, extra ->
                    release()
                    mediaPlayer = null
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        adapter = null
    }

    private inner class ExpandableDialogAdapter(
        private val dialogues: List<DialogData>,
        private val onPlayAudio: (String) -> Unit,
        private val onDialogClicked: (Int) -> Unit
    ) : RecyclerView.Adapter<ExpandableDialogAdapter.DialogViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DialogViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_dialogue_expandable, parent, false)
            return DialogViewHolder(view)
        }

        override fun onBindViewHolder(holder: DialogViewHolder, position: Int) {
            val dialog = dialogues[position]
            holder.bind(dialog, onPlayAudio, position)

            // Устанавливаем состояние раскрытия
            val isExpanded = position == expandedPosition
            holder.setExpanded(isExpanded)
        }

        override fun getItemCount(): Int = dialogues.size

        override fun onViewRecycled(holder: DialogViewHolder) {
            super.onViewRecycled(holder)
            holder.clearReplies()
        }

        inner class DialogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val dialogueHeaderCard: CardView = view.findViewById(R.id.dialogueHeaderCard)
            private val expandIcon: ImageView = view.findViewById(R.id.expandIcon)
            private val dialogueTitle: TextView = view.findViewById(R.id.dialogueTitle)
            private val dialogueCount: TextView = view.findViewById(R.id.dialogueCount)
            private val repliesContainer: LinearLayout = view.findViewById(R.id.repliesContainer)
            private val headerContent: LinearLayout = view.findViewById(R.id.headerContent)

            private var isExpanded = false
            private var currentDialog: DialogData? = null
            private var currentPosition = -1

            init {
                // Клик по всей карточке (включая иконку и текст)
                dialogueHeaderCard.setOnClickListener {
                    currentPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { pos ->
                        onDialogClicked(pos)
                    }
                }

                // Также клик по внутреннему контенту
                headerContent.setOnClickListener {
                    currentPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { pos ->
                        onDialogClicked(pos)
                    }
                }
            }

            fun bind(dialog: DialogData, onPlayAudio: (String) -> Unit, position: Int) {
                currentDialog = dialog
                currentPosition = position
                dialogueTitle.text = dialog.name

                val lineCount = dialog.lines.size
                dialogueCount.text = when {
                    lineCount == 0 -> "Нет реплик"
                    lineCount == 1 -> "1 реплика"
                    lineCount in 2..4 -> "$lineCount реплики"
                    else -> "$lineCount реплик"
                }

                // Создаем карточки для реплик
                createReplyCards(dialog.lines, onPlayAudio)
            }

            fun setExpanded(expanded: Boolean) {
                isExpanded = expanded

                // Анимация иконки
                expandIcon.animate()
                    .rotation(if (expanded) 180f else 0f)
                    .setDuration(200)
                    .start()

                // Показываем/скрываем контейнер с репликами
                repliesContainer.visibility = if (expanded) View.VISIBLE else View.GONE
            }

            private fun createReplyCards(lines: List<DialogueLine>, onPlayAudio: (String) -> Unit) {
                // Очищаем контейнер
                repliesContainer.removeAllViews()

                if (lines.isEmpty()) {
                    val emptyText = TextView(itemView.context).apply {
                        text = "Нет реплик в этом диалоге"
                        textSize = 14f
                        setTextColor(context.resources.getColor(R.color.gray_300, context.theme))
                        setPadding(16.dpToPx(context), 8.dpToPx(context), 16.dpToPx(context), 8.dpToPx(context))
                    }
                    repliesContainer.addView(emptyText)
                    return
                }

                // Создаем карточку для каждой реплики
                lines.forEachIndexed { index, line ->
                    val replyCard = LayoutInflater.from(itemView.context)
                        .inflate(R.layout.item_dialogue_reply, repliesContainer, false)

                    val replyText: TextView = replyCard.findViewById(R.id.replyText)
                    val playButton: ImageButton = replyCard.findViewById(R.id.playButton)
                    val replyNumber: TextView = replyCard.findViewById(R.id.replyNumber)

                    replyText.text = line.text
                    replyNumber.text = "Реплика ${index + 1}"

                    // Воспроизведение аудио
                    playButton.setOnClickListener {
                        onPlayAudio(line.sound)
                    }

                    // Клик по всей карточке тоже воспроизводит аудио
                    replyCard.setOnClickListener {
                        onPlayAudio(line.sound)
                    }

                    // Анимация нажатия на карточку
                    replyCard.setOnTouchListener { view, motionEvent ->
                        when (motionEvent.action) {
                            android.view.MotionEvent.ACTION_DOWN -> {
                                view.animate()
                                    .scaleX(0.95f)
                                    .scaleY(0.95f)
                                    .setDuration(100)
                                    .start()
                            }
                            android.view.MotionEvent.ACTION_UP,
                            android.view.MotionEvent.ACTION_CANCEL -> {
                                view.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100)
                                    .start()
                            }
                        }
                        false
                    }

                    repliesContainer.addView(replyCard)
                }
            }

            fun clearReplies() {
                repliesContainer.removeAllViews()
            }
        }
    }

    private data class DialogData(
        val id: String,
        val name: String,
        val lines: List<DialogueLine>,
        val position: Int
    )

    // Extension для преобразования dp в px
    private fun Int.dpToPx(context: android.content.Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}