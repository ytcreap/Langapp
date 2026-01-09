package com.example.langapp.utils

import com.example.langapp.data.model.MarkdownElement
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tables.TableTheme
import android.content.Context
import android.text.Spanned

object MarkdownParser {

    private lateinit var markwon: Markwon

    fun initialize(context: Context) {
        markwon = Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(TableTheme.Builder().build()))
            .build()
    }

    fun parseMarkdown(content: String): List<MarkdownElement> {
        val elements = mutableListOf<MarkdownElement>()
        val lines = content.lines()

        var i = 0
        var inCodeBlock = false
        var codeBlockLanguage = ""
        var codeBlockContent = StringBuilder()
        var currentListItems = mutableListOf<MarkdownElement.ListItem>()
        var isOrderedList = false

        fun addParagraph(text: String) {
            if (text.isNotBlank()) {
                elements.add(MarkdownElement.Paragraph(text))
            }
        }

        fun flushList() {
            if (currentListItems.isNotEmpty()) {
                // Добавляем все элементы списка
                elements.addAll(currentListItems)
                currentListItems.clear()
            }
        }

        while (i < lines.size) {
            val line = lines[i].trim()

            if (inCodeBlock) {
                if (line.startsWith("```")) {
                    // Конец блока кода
                    elements.add(MarkdownElement.CodeBlock(codeBlockContent.toString(), codeBlockLanguage))
                    inCodeBlock = false
                    codeBlockContent = StringBuilder()
                } else {
                    codeBlockContent.append(line).append("\n")
                }
                i++
                continue
            }

            when {
                // Блок кода ```
                line.startsWith("```") -> {
                    flushList() // Завершаем предыдущий список если был
                    codeBlockLanguage = line.substring(3).trim()
                    inCodeBlock = true
                    i++
                }

                // Заголовки
                line.startsWith("#") -> {
                    flushList() // Завершаем предыдущий список если был
                    val level = line.takeWhile { it == '#' }.length
                    val text = line.substring(level).trim()
                    if (text.isNotEmpty()) {
                        elements.add(MarkdownElement.Heading(level, text))
                    }
                    i++
                }

                // Горизонтальная линия
                line.matches(Regex("""^[-*_]{3,}$""")) -> {
                    flushList() // Завершаем предыдущий список если был
                    elements.add(MarkdownElement.HorizontalRule)
                    i++
                }

                // Цитаты
                line.startsWith(">") -> {
                    flushList() // Завершаем предыдущий список если был
                    val text = line.substring(1).trim()
                    elements.add(MarkdownElement.Blockquote(text))
                    i++
                }

                // Нумерованный список
                line.matches(Regex("""^\d+\.\s+.*""")) -> {
                    val text = line.substring(line.indexOf('.') + 1).trim()
                    currentListItems.add(MarkdownElement.ListItem(text, true))
                    isOrderedList = true
                    i++
                }

                // Маркированный список
                line.matches(Regex("""^[-*+]\s+.*""")) -> {
                    val text = line.substring(1).trim()
                    currentListItems.add(MarkdownElement.ListItem(text, false))
                    isOrderedList = false
                    i++
                }

                // Изображения
                line.startsWith("![") && "](" in line -> {
                    flushList() // Завершаем предыдущий список если был
                    val regex = """!\[([^\]]*)\]\(([^)]+)\)""".toRegex()
                    val match = regex.find(line)
                    if (match != null) {
                        val (altText, url) = match.destructured
                        elements.add(MarkdownElement.Image(url, altText))
                    }
                    i++
                }

                // Таблицы (обрабатываем как параграф, Markwon сам отрендерит)
                line.contains('|') && line.trim().startsWith('|') -> {
                    flushList() // Завершаем предыдущий список если был
                    val tableContent = StringBuilder()
                    // Собираем всю таблицу
                    while (i < lines.size && lines[i].trim().startsWith('|')) {
                        tableContent.append(lines[i]).append("\n")
                        i++
                    }
                    elements.add(MarkdownElement.Paragraph(tableContent.toString().trim()))
                    continue
                }

                // Пустая строка
                line.isEmpty() -> {
                    flushList() // Пустая строка завершает список
                    i++
                }

                // Обычный текст
                else -> {
                    // Если предыдущая строка была списком, а текущая - продолжение
                    if (currentListItems.isNotEmpty() && !line.startsWith(" ")) {
                        // Это продолжение предыдущего элемента списка
                        val lastIndex = currentListItems.size - 1
                        val lastItem = currentListItems[lastIndex]
                        val newText = lastItem.text + " " + line.trim()
                        currentListItems[lastIndex] = MarkdownElement.ListItem(newText, isOrderedList)
                        i++
                    } else {
                        flushList() // Завершаем список перед параграфом

                        val paragraphBuilder = StringBuilder(line)
                        i++

                        // Собираем многострочный параграф
                        while (i < lines.size && lines[i].isNotBlank() &&
                            !isStartOfMarkdownElement(lines[i])) {
                            paragraphBuilder.append(" ").append(lines[i].trim())
                            i++
                        }

                        val text = paragraphBuilder.toString().trim()
                        if (text.isNotBlank()) {
                            addParagraph(text)
                        }
                    }
                }
            }
        }

        // Добавляем оставшиеся элементы списка
        flushList()

        return elements
    }

    private fun isStartOfMarkdownElement(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("#") ||
                trimmed.startsWith("```") ||
                trimmed.startsWith("![") ||
                trimmed.matches(Regex("""^\d+\.\s+.*""")) ||
                trimmed.matches(Regex("""^[-*+]\s+.*""")) ||
                trimmed.startsWith(">") ||
                trimmed.matches(Regex("""^[-*_]{3,}$""")) ||
                trimmed.startsWith("|")
    }

    fun getMarkwon(): Markwon {
        if (!this::markwon.isInitialized) {
            throw IllegalStateException("Markwon not initialized. Call initialize() first.")
        }
        return markwon
    }
}