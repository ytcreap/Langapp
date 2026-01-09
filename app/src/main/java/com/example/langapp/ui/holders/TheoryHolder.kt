package com.example.langapp.ui.holders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.langapp.R
import com.example.langapp.data.model.*
import com.example.langapp.utils.MarkdownParser
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tables.TableTheme

class TheoryHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val titleText: TextView = view.findViewById(R.id.titleText)
    private val contentContainer: LinearLayout = view.findViewById(R.id.contentContainer)
    private val mainImageView: ImageView = view.findViewById(R.id.imageView)
    private val questionsContainer: LinearLayout = view.findViewById(R.id.questionsContainer)

    private lateinit var markwon: Markwon
    private var listCounter = 1

    init {
        // Инициализируем Markwon
        markwon = Markwon.builder(view.context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(TableTheme.Builder().build()))
            .build()

    }

    fun bind(task: TheoryTask, onClick: (TheoryTask) -> Unit) {
        // Сбрасываем счетчик списка
        listCounter = 1

        // Устанавливаем заголовок если есть
        if (task.title.isNotEmpty()) {
            titleText.text = task.title
            titleText.isVisible = true
        } else {
            titleText.isVisible = false
        }

        // Главное изображение (для обратной совместимости)
        if (task.image.isNotEmpty()) {
            loadImage(task.image, mainImageView)
            mainImageView.isVisible = true
        } else {
            mainImageView.isVisible = false
        }

        // Очищаем контейнер
        contentContainer.removeAllViews()

        // Рендерим Markdown контент
        if (task.lazyParsedMarkdown.isNotEmpty()) {
            println("DEBUG: Rendering ${task.lazyParsedMarkdown.size} markdown elements")
            renderMarkdown(task.lazyParsedMarkdown)
        } else if (task.text.isNotEmpty()) {
            // Для обратной совместимости
            val textView = TextView(itemView.context)
            markwon.setMarkdown(textView, task.text)
            textView.textSize = 16f
            textView.setPadding(0, 8.dpToPx(), 0, 8.dpToPx())
            contentContainer.addView(textView)
        } else {
            println("DEBUG: No markdown content to render")
        }

        // Интерактивные элементы
        questionsContainer.removeAllViews()
        task.interactiveElements.forEach { element ->
            renderInteractiveElement(element)
        }
    }

    private fun renderMarkdown(elements: List<MarkdownElement>) {
        elements.forEach { element ->
            when (element) {
                is MarkdownElement.Heading -> {
                    val textView = TextView(itemView.context)
                    // Создаем markdown заголовок
                    val markdownText = "${"#".repeat(element.level)} ${element.text}"
                    markwon.setMarkdown(textView, markdownText)

                    // Дополнительные стили
                    textView.textSize = when (element.level) {
                        1 -> 24f
                        2 -> 20f
                        3 -> 18f
                        else -> 16f
                    }
                    textView.setPadding(0, 16.dpToPx(), 0, 8.dpToPx())
                    contentContainer.addView(textView)
                }

                is MarkdownElement.Paragraph -> {
                    val textView = TextView(itemView.context)
                    markwon.setMarkdown(textView, element.text)
                    textView.textSize = 16f
                    textView.setPadding(0, 8.dpToPx(), 0, 8.dpToPx())
                    textView.setTextIsSelectable(true)
                    contentContainer.addView(textView)
                }

                is MarkdownElement.Image -> {
                    val imageView = ImageView(itemView.context)
                    imageView.layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 8.dpToPx(), 0, 8.dpToPx())
                    }

                    loadImage(element.url, imageView)

                    // Alt text под изображением
                    if (element.altText.isNotEmpty()) {
                        val altTextView = TextView(itemView.context)
                        altTextView.text = element.altText
                        altTextView.textSize = 12f
                        altTextView.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
                        altTextView.setPadding(0, 4.dpToPx(), 0, 8.dpToPx())

                        val container = LinearLayout(itemView.context)
                        container.orientation = LinearLayout.VERTICAL
                        container.addView(imageView)
                        container.addView(altTextView)
                        contentContainer.addView(container)
                    } else {
                        contentContainer.addView(imageView)
                    }
                }

                is MarkdownElement.ListItem -> {
                    val listContainer = LinearLayout(itemView.context)
                    listContainer.orientation = LinearLayout.HORIZONTAL
                    listContainer.setPadding(0, 4.dpToPx(), 0, 4.dpToPx())

                    // Маркер
                    val marker = TextView(itemView.context)
                    marker.text = if (element.isOrdered) {
                        "${listCounter++}."
                    } else {
                        "•"
                    }
                    marker.textSize = 16f
                    marker.setPadding(0, 0, 8.dpToPx(), 0)

                    // Текст элемента списка
                    val textView = TextView(itemView.context)
                    markwon.setMarkdown(textView, element.text)
                    textView.textSize = 16f
                    textView.layoutParams = LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    )

                    listContainer.addView(marker)
                    listContainer.addView(textView)
                    contentContainer.addView(listContainer)
                }

                is MarkdownElement.CodeBlock -> {
                    val codeView = TextView(itemView.context)

                    // Форматируем код
                    val codeText = "```${element.language}\n${element.code}\n```"
                    markwon.setMarkdown(codeView, codeText)

                    codeView.textSize = 14f
                    codeView.setTypeface(android.graphics.Typeface.MONOSPACE)
                    codeView.setBackgroundColor(itemView.context.getColor(R.color.gray_500))
                    codeView.setTextColor(itemView.context.getColor(android.R.color.white))
                    codeView.setPadding(12.dpToPx(), 12.dpToPx(), 12.dpToPx(), 12.dpToPx())

                    // Скругленные углы
                    codeView.background = itemView.context.getDrawable(R.drawable.code_block_background)

                    contentContainer.addView(codeView)
                }

                is MarkdownElement.Blockquote -> {
                    val quoteView = TextView(itemView.context)
                    markwon.setMarkdown(quoteView, "> ${element.text}")
                    quoteView.textSize = 16f
                    quoteView.setTypeface(quoteView.typeface, android.graphics.Typeface.ITALIC)
                    quoteView.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
                    quoteView.setBackgroundColor(itemView.context.getColor(R.color.gray_300))
                    quoteView.setPadding(12.dpToPx(), 8.dpToPx(), 12.dpToPx(), 8.dpToPx())

                    // Бордер слева
                    quoteView.background = itemView.context.getDrawable(R.drawable.blockquote_background)

                    contentContainer.addView(quoteView)
                }

                is MarkdownElement.HorizontalRule -> {
                    val view = View(itemView.context)
                    view.layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1.dpToPx()
                    ).apply {
                        setMargins(0, 16.dpToPx(), 0, 16.dpToPx())
                    }
                    view.setBackgroundColor(itemView.context.getColor(android.R.color.darker_gray))
                    contentContainer.addView(view)
                }
            }
        }
    }

    private fun renderInteractiveElement(element: InteractiveElement) {
        when (element.type) {
            "question" -> renderQuestion(element)
            "quiz" -> renderQuiz(element)
            "example" -> renderExample(element)
            "task" -> renderTask(element)
            else -> renderQuestion(element) // По умолчанию
        }
    }

    private fun renderQuestion(element: InteractiveElement) {
        val view = LayoutInflater.from(itemView.context)
            .inflate(R.layout.item_interactive_question, questionsContainer, false)

        val questionText = view.findViewById<TextView>(R.id.questionText)
        val answerInput = view.findViewById<EditText>(R.id.answerInput)
        val checkButton = view.findViewById<Button>(R.id.checkButton)
        val resultText = view.findViewById<TextView>(R.id.resultText)

        // Используем Markwon для форматирования вопроса
        markwon.setMarkdown(questionText, element.content)
        answerInput.hint = element.hint ?: "Введите ваш ответ..."

        checkButton.setOnClickListener {
            val userAnswer = answerInput.text.toString().trim()
            val isCorrect = userAnswer.equals(element.correctAnswer, ignoreCase = true)

            resultText.text = if (isCorrect) "✓ Правильно!" else "✗ Попробуйте еще раз"
            resultText.setTextColor(if (isCorrect) {
                itemView.context.getColor(android.R.color.holo_green_dark)
            } else {
                itemView.context.getColor(android.R.color.holo_red_dark)
            })
            resultText.isVisible = true
        }

        questionsContainer.addView(view)
    }

    private fun renderQuiz(element: InteractiveElement) {
        // Пока что используем ту же реализацию что и question
        renderQuestion(element)
    }

    private fun renderExample(element: InteractiveElement) {
        // Просто показываем пример
        val textView = TextView(itemView.context)
        markwon.setMarkdown(textView, "📘 **Пример**: ${element.content}")
        textView.textSize = 16f
        textView.setPadding(0, 8.dpToPx(), 0, 8.dpToPx())
        textView.setTextColor(itemView.context.getColor(android.R.color.holo_blue_dark))
        questionsContainer.addView(textView)
    }

    private fun renderTask(element: InteractiveElement) {
        // Задание
        val textView = TextView(itemView.context)
        markwon.setMarkdown(textView, "📝 **Задание**: ${element.content}")
        textView.textSize = 16f
        textView.setPadding(0, 8.dpToPx(), 0, 8.dpToPx())
        textView.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
        questionsContainer.addView(textView)
    }

    private fun loadImage(imageUrl: String, imageView: ImageView) {
        if (imageUrl.startsWith("http")) {
            // Загрузка из интернета
            Glide.with(itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .into(imageView)
        } else {
            // Загрузка из ресурсов
            val imageName = imageUrl.substringBeforeLast('.')
            val imageResId = itemView.resources.getIdentifier(
                imageName,
                "drawable",
                itemView.context.packageName
            )

            if (imageResId != 0) {
                Glide.with(itemView.context)
                    .load(imageResId)
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.placeholder_image)
            }
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * itemView.context.resources.displayMetrics.density).toInt()
    }
}