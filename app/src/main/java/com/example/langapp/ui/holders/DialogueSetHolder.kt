package com.example.langapp.ui.holders

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.R
import com.example.langapp.data.model.DialogueLine
import com.example.langapp.data.model.DialogueSet

class DialogueSetHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val titleText: TextView = view.findViewById(R.id.titleText)
    private val questionText: TextView = view.findViewById(R.id.questionText)
    private val dialoguesRecyclerView: RecyclerView = view.findViewById(R.id.dialoguesRecyclerView)
    private val playAllButton: ImageButton = view.findViewById(R.id.playAllButton)

    private var mediaPlayer: MediaPlayer? = null
    private var expandedPosition = -1
    private var adapter: ExpandableDialogAdapter? = null
    private var currentPlayingDialog: DialogData? = null
    private var currentPlayingIndex = -1
    private var isPlayingAll = false

    fun bind(dialogueSet: DialogueSet) {
        titleText.text = dialogueSet.taskname
        questionText.text = dialogueSet.question

        val dialoguesList = dialogueSet.dialogues.entries
            .sortedBy { it.key }
            .mapIndexed { index, (dialogKey, dialogueLines) ->
                DialogData(
                    id = dialogKey,
                    name = "Диалог ${index + 1}",
                    lines = dialogueLines.mapIndexed { lineIndex, line ->
                        // Определяем позицию говорящего (чередуем)
                        DialogueLine(
                            text = line.text,
                            sound = line.sound,
                            speaker = if (lineIndex % 2 == 0) "user1" else "user2",
                            isLeft = lineIndex % 2 == 0
                        )
                    },
                    position = index
                )
            }

        dialoguesRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(itemView.context)
        dialoguesRecyclerView.setHasFixedSize(false)

        adapter = ExpandableDialogAdapter(dialoguesList, ::playSingleAudio, ::onDialogClicked, ::onPlayAllDialog)
        dialoguesRecyclerView.adapter = adapter

        // Кнопка воспроизведения всех диалогов
        playAllButton.setOnClickListener {
            playAllDialogues(dialoguesList)
        }
    }

    // ДОБАВЛЯЕМ ЭТУ ФУНКЦИЮ
    private fun onDialogClicked(position: Int) {
        // Останавливаем текущее воспроизведение
        stopCurrentPlayback()

        // Переключаем раскрытие диалога
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

    private fun onPlayAllDialog(dialog: DialogData) {
        currentPlayingDialog = dialog
        isPlayingAll = true
        currentPlayingIndex = 0

        // Автоматически раскрываем диалог если не раскрыт
        val position = dialog.position
        if (expandedPosition != position) {
            onDialogClicked(position)
        }

        // Запускаем воспроизведение с первой реплики
        adapter?.startPlayingDialog(dialog.id, 0)
        playSingleAudio(dialog.lines[0].sound)
    }

    private fun playAllDialogues(dialogues: List<DialogData>) {
        if (dialogues.isEmpty()) return

        // Закрываем текущий воспроизводимый диалог
        stopCurrentPlayback()

        // Начинаем с первого диалога
        onPlayAllDialog(dialogues[0])
    }

    private fun playSingleAudio(audioUrl: String) {
        mediaPlayer?.release()
        mediaPlayer = null

        if (audioUrl.isEmpty()) {
            adapter?.onAudioError()
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioUrl)
                setOnPreparedListener {
                    adapter?.onAudioPrepared()
                    it.start()
                }
                setOnCompletionListener {
                    adapter?.onAudioCompleted()
                    release()
                    mediaPlayer = null

                    // Если воспроизводим весь диалог, переходим к следующей реплике
                    if (isPlayingAll && currentPlayingDialog != null) {
                        val lines = currentPlayingDialog!!.lines
                        if (currentPlayingIndex < lines.size - 1) {
                            currentPlayingIndex++
                            adapter?.startPlayingDialog(currentPlayingDialog!!.id, currentPlayingIndex)
                            playSingleAudio(lines[currentPlayingIndex].sound)
                        } else {
                            // Диалог закончен
                            isPlayingAll = false
                            currentPlayingDialog = null
                            currentPlayingIndex = -1
                            adapter?.stopPlayingDialog()
                        }
                    }
                }
                setOnErrorListener { mp, what, extra ->
                    adapter?.onAudioError()
                    release()
                    mediaPlayer = null
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            adapter?.onAudioError()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private fun stopCurrentPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPlayingAll = false
        currentPlayingDialog = null
        currentPlayingIndex = -1
        adapter?.stopPlayingDialog()
    }

    fun onDestroy() {
        stopCurrentPlayback()
        adapter = null
    }

    private inner class ExpandableDialogAdapter(
        private val dialogues: List<DialogData>,
        private val onPlayAudio: (String) -> Unit,
        private val onDialogClicked: (Int) -> Unit,
        private val onPlayAllDialog: (DialogData) -> Unit
    ) : RecyclerView.Adapter<ExpandableDialogAdapter.DialogViewHolder>() {

        private var currentPlayingDialogId: String? = null
        private var currentPlayingLineIndex = -1
        private var isLoadingAudio = false

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DialogViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_dialogue_expandable, parent, false)
            return DialogViewHolder(view)
        }

        override fun onBindViewHolder(holder: DialogViewHolder, position: Int) {
            val dialog = dialogues[position]
            val isPlayingThisDialog = dialog.id == currentPlayingDialogId
            holder.bind(
                dialog,
                onPlayAudio,
                position,
                onPlayAllDialog,
                isPlayingThisDialog,
                if (isPlayingThisDialog) currentPlayingLineIndex else -1,
                isLoadingAudio
            )

            val isExpanded = position == expandedPosition
            holder.setExpanded(isExpanded)
        }

        override fun getItemCount(): Int = dialogues.size

        override fun onViewRecycled(holder: DialogViewHolder) {
            super.onViewRecycled(holder)
            holder.clearReplies()
        }

        fun startPlayingDialog(dialogId: String, lineIndex: Int) {
            isLoadingAudio = true
            currentPlayingDialogId = dialogId
            currentPlayingLineIndex = lineIndex
            val position = dialogues.indexOfFirst { it.id == dialogId }
            if (position != -1) notifyItemChanged(position)
        }

        fun stopPlayingDialog() {
            isLoadingAudio = false
            val oldDialogId = currentPlayingDialogId
            currentPlayingDialogId = null
            currentPlayingLineIndex = -1
            oldDialogId?.let { id ->
                val position = dialogues.indexOfFirst { it.id == id }
                if (position != -1) notifyItemChanged(position)
            }
        }

        fun onAudioPrepared() {
            isLoadingAudio = false
            currentPlayingDialogId?.let { id ->
                val position = dialogues.indexOfFirst { it.id == id }
                if (position != -1) notifyItemChanged(position)
            }
        }

        fun onAudioCompleted() {
            // Обновляем UI после завершения воспроизведения
            currentPlayingDialogId?.let { id ->
                val position = dialogues.indexOfFirst { it.id == id }
                if (position != -1) notifyItemChanged(position)
            }
        }

        fun onAudioError() {
            isLoadingAudio = false
            stopPlayingDialog()
        }

        inner class DialogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val dialogueHeaderCard: CardView = view.findViewById(R.id.dialogueHeaderCard)
            private val expandIcon: ImageView = view.findViewById(R.id.expandIcon)
            private val dialogueTitle: TextView = view.findViewById(R.id.dialogueTitle)
            private val dialogueCount: TextView = view.findViewById(R.id.dialogueCount)
            private val repliesContainer: LinearLayout = view.findViewById(R.id.repliesContainer)
            private val headerContent: LinearLayout = view.findViewById(R.id.headerContent)
            private val playAllButton: ImageButton = view.findViewById(R.id.playAllButton)

            private var isExpanded = false
            private var currentDialog: DialogData? = null
            private var currentPosition = -1

            init {
                dialogueHeaderCard.setOnClickListener {
                    currentPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { pos ->
                        onDialogClicked(pos)
                    }
                }

                headerContent.setOnClickListener {
                    currentPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { pos ->
                        onDialogClicked(pos)
                    }
                }
            }

            fun bind(
                dialog: DialogData,
                onPlayAudio: (String) -> Unit,
                position: Int,
                onPlayAllDialog: (DialogData) -> Unit,
                isPlayingThisDialog: Boolean,
                currentPlayingLine: Int,
                isLoading: Boolean
            ) {
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

                // Настраиваем кнопку "Play All"
                playAllButton.visibility = if (isExpanded) View.VISIBLE else View.GONE
                playAllButton.setOnClickListener {
                    onPlayAllDialog(dialog)
                }

                // Создаем карточки для реплик
                createReplyCards(dialog.lines, onPlayAudio, isPlayingThisDialog, currentPlayingLine, isLoading)
            }

            fun setExpanded(expanded: Boolean) {
                isExpanded = expanded

                expandIcon.animate()
                    .rotation(if (expanded) 180f else 0f)
                    .setDuration(200)
                    .start()

                repliesContainer.visibility = if (expanded) View.VISIBLE else View.GONE
                playAllButton.visibility = if (expanded) View.VISIBLE else View.GONE
            }

            private fun createReplyCards(
                lines: List<DialogueLine>,
                onPlayAudio: (String) -> Unit,
                isPlayingThisDialog: Boolean,
                currentPlayingLine: Int,
                isLoading: Boolean
            ) {
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

                lines.forEachIndexed { index, line ->
                    val isCurrentPlayingLine = isPlayingThisDialog && index == currentPlayingLine

                    val replyCard = LayoutInflater.from(itemView.context)
                        .inflate(R.layout.item_dialogue_message, repliesContainer, false)

                    val messageBubble: LinearLayout = replyCard.findViewById(R.id.messageBubble)
                    val messageText: TextView = replyCard.findViewById(R.id.messageText)
                    val playButton: ImageButton = replyCard.findViewById(R.id.playButton)
                    val loadingSpinner: ProgressBar = replyCard.findViewById(R.id.loadingSpinner)
                    val timeText: TextView = replyCard.findViewById(R.id.timeText)

                    messageText.text = line.text
                    timeText.text = "Реплика ${index + 1}"

                    // Оформление в стиле Telegram
                    if (line.isLeft) {
                        // Сообщение слева (собеседник)
                        messageBubble.setBackgroundResource(R.drawable.message_bubble_left)
                        val params = messageBubble.layoutParams as LinearLayout.LayoutParams
                        params.gravity = android.view.Gravity.START
                        params.marginStart = 8.dpToPx(itemView.context)
                        params.marginEnd = 80.dpToPx(itemView.context)
                    } else {
                        // Сообщение справа (пользователь)
                        messageBubble.setBackgroundResource(R.drawable.message_bubble_right)
                        val params = messageBubble.layoutParams as LinearLayout.LayoutParams
                        params.gravity = android.view.Gravity.END
                        params.marginStart = 80.dpToPx(itemView.context)
                        params.marginEnd = 8.dpToPx(itemView.context)
                    }

                    // Подсветка текущей воспроизводимой реплики
                    if (isCurrentPlayingLine) {
                        messageBubble.backgroundTintList =
                            android.content.res.ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    itemView.context,
                                    R.color.message_highlight
                                )
                            )
                        messageText.setTextColor(
                            ContextCompat.getColor(itemView.context, R.color.white)
                        )
                    }

                    // Управление видимостью спиннера
                    if (isCurrentPlayingLine && isLoading) {
                        loadingSpinner.visibility = View.VISIBLE
                        playButton.visibility = View.GONE
                    } else {
                        loadingSpinner.visibility = View.GONE
                        playButton.visibility = View.VISIBLE
                        playButton.setImageResource(
                            if (isCurrentPlayingLine) R.drawable.ic_pause else R.drawable.ic_play
                        )
                    }

                    // Воспроизведение аудио
                    playButton.setOnClickListener {
                        onPlayAudio(line.sound)
                    }

                    replyCard.setOnClickListener {
                        onPlayAudio(line.sound)
                    }

                    // Анимация нажатия
                    replyCard.setOnTouchListener { view, motionEvent ->
                        when (motionEvent.action) {
                            android.view.MotionEvent.ACTION_DOWN -> {
                                view.animate()
                                    .scaleX(0.98f)
                                    .scaleY(0.98f)
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

    private fun Int.dpToPx(context: android.content.Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}