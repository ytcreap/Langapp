package com.example.langapp.data.model

import android.os.Parcelable
import com.google.firebase.database.PropertyName
import kotlinx.android.parcel.Parcelize

// класс заданий с id и type
sealed class Task : Parcelable {
    abstract val id: String
    abstract val type: String
    abstract val taskname: String
}

// Базовые модели для вопросов
@Parcelize
data class Question(
    @get:PropertyName("name") val taskname: String,
    @get:PropertyName("question") val question: String,
    @get:PropertyName("options") val options: List<String> = emptyList(),
    @get:PropertyName("answer") val answer: String, // Может быть Int для индекса или String для текста
    @get:PropertyName("hint") val hint: String? = null
) : Parcelable

// 1. Множественный выбор
@Parcelize
data class MultipleChoiceTask(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("question") val question: String,
    @get:PropertyName("options") val options: List<String>,
    @get:PropertyName("correctAnswerIndex") val correctAnswerIndex: Int,
    @get:PropertyName("audioHint") val audioHint: String? = null,
    @get:PropertyName("explanation") val explanation: String? = null,
    @get:PropertyName("image") val image: String? = null, // Добавляем поддержку изображений
    val selectedIndex: Int? = null
) : Task() {
    override val type: String = "MULTIPLE_CHOICE"
}

// 2. Ввод по изображению
@Parcelize
data class ImageInputTask(
    @get:PropertyName("question") val question: String,
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("image") val image: String,
    @get:PropertyName("correctAnswer") val correctAnswer: String,
    @get:PropertyName("hint") val hint: String? = null,
    val userAnswer: String? = null
) : Task() {
    override val type: String = "IMAGE_INPUT"
}

// 3. Текстовый ввод
@Parcelize
data class TextInputTask(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("question") val question: String,
    @get:PropertyName("correctAnswer") val correctAnswer: String,
    @get:PropertyName("audioSupport") val audioSupport: String? = null,
    val userAnswer: String? = null
) : Task() {
    override val type: String = "TEXT_INPUT"
}

// 4. Аудио ввод
@Parcelize
data class AudioInputTask(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("audioPrompt") val audioPrompt: String,
    @get:PropertyName("correctAnswer") val correctAnswer: String,
    @get:PropertyName("textHint") val textHint: String? = null,
    val userAnswer: String? = null
) : Task() {
    override val type: String = "AUDIO_INPUT"
}

// 5. Изображение + аудио
@Parcelize
data class ImageAudioTask(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("image") val image: String,
    @get:PropertyName("audio") val audio: String,
    @get:PropertyName("questions") val questions: List<Question>
) : Task() {
    override val type: String = "IMAGE_AUDIO"
}

// 6. Изображение + запись
@Parcelize
data class ImageRecordingTask(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("image") val image: String,
    @get:PropertyName("targetText") val targetText: String,
    @get:PropertyName("referenceAudio") val referenceAudio: String? = null,
    @get:PropertyName("similarityThreshold") val similarityThreshold: Float = 0.7f
) : Task() {
    override val type: String = "IMAGE_RECORDING"
}

// 7. Аудио + запись
@Parcelize
data class AudioRecordingTask(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("question") val question: String,
    @get:PropertyName("audioPrompt") val audioPrompt: String,
    @get:PropertyName("targetText") val targetText: String,
    @get:PropertyName("textHint") val textHint: String? = null,
    @get:PropertyName("maxAttempts") val maxAttempts: Int = 3,
    @get:PropertyName("image") val image: String? = null // Добавляем поддержку изображений
) : Task() {
    override val type: String = "AUDIO_RECORDING"
}

// 8. Текст + запись
@Parcelize
data class TextRecordingTask(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("text") val text: String,
    @get:PropertyName("referenceAudio") val referenceAudio: String,
    @get:PropertyName("phoneticHint") val phoneticHint: String,
    @get:PropertyName("checkPronunciation") val checkPronunciation: Boolean = true
) : Task() {
    override val type: String = "TEXT_RECORDING"
}


// Устаревшие классы (можно удалить после миграции)
@Deprecated("Use specific task types instead")
@Parcelize
data class PhoneticTask(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("word") val word: String,
    @get:PropertyName("ipa") val ipa: String,
    @get:PropertyName("audio") val audio: String,
    @get:PropertyName("hint") val hint: String
) : Task() {
    override val type: String = "phonetics"
}

// 10. Набор заданий с изображениями
@Parcelize
data class TaskSet(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("tasks") val tasks: List<ImageInputTask>,
    @get:PropertyName("type") override val type: String = "TASK_SET"
) : Task()

// 11. Набор текстовых заданий
@Parcelize
data class TextInputSet(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("question") val question: String,
    @get:PropertyName("tasks") val tasks: List<TextInputTask>,
    @get:PropertyName("type") override val type: String = "TEXT_INPUT_SET"
) : Task()

// 12. Набор аудио заданий
@Parcelize
data class AudioRecordingSet(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("tasks") val tasks: List<AudioRecordingTask>,
    @get:PropertyName("type") override val type: String = "AUDIO_RECORDING_SET"
) : Task()

@Parcelize
data class AlphabetLetter(
    @get:PropertyName("image") val image: String,
    @get:PropertyName("sound") val sound: String,
    @get:PropertyName("letter") val letter: String
) : Parcelable

@Parcelize
data class AlphabetTask(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("question") val question: String,
    @get:PropertyName("letters") val letters: List<AlphabetLetter>,
    @get:PropertyName("autoPlay") val autoPlay: Boolean = true
) : Task() {
    override val type: String = "ALPHABET"
}

@Parcelize
data class SyllableItem(
    @get:PropertyName("sound") val sound: String,
    @get:PropertyName("text") val text: String
) : Parcelable

@Parcelize
data class SyllableLetter(
    val letter: String,
    val items: List<SyllableItem>,
    val image: String = "", // для совместимости с AlphabetLetter
    val sound: String = "" // для совместимости с AlphabetLetter
) : Parcelable


@Parcelize
data class SyllableTask(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("question") val question: String,
    val letters: List<SyllableLetter>,
    @get:PropertyName("type") override val type: String = "SYLLABLE"
) : Task()

// data/model/Task.kt
@Parcelize
data class MultipleChoiceSet(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("tasks") val tasks: List<MultipleChoiceTask>,
    @get:PropertyName("type") override val type: String = "MULTIPLE_CHOICE_SET"
) : Task()

@Parcelize
data class ImageAudioSet(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("tasks") val tasks: List<ImageAudioTask>,
    @get:PropertyName("type") override val type: String = "IMAGE_AUDIO_SET"
) : Task()

@Parcelize
data class VocabularyItem(
    @get:PropertyName("word") val word: String,
    @get:PropertyName("image") val image: String,
    @get:PropertyName("audio") val audio: String,
    @get:PropertyName("text") val fullText: String = ""
) : Parcelable

@Parcelize
data class VocabularyImageAudioSet(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("question") val question: String = "Слушай и запоминай слова",
    @get:PropertyName("tasks") val vocabulary: Map<String, Map<String, String>>, // Изменяем PropertyName на "tasks"
    @get:PropertyName("type") override val type: String = "VOCABULARY_IMAGE_AUDIO_SET"
) : Task()

@Parcelize
data class DialogueTask(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("question") val question: String = "Послушайте диалог",
    @get:PropertyName("dialogue") val dialogue: List<DialogueLine>,
    @get:PropertyName("image") val image: String? = null,
    @get:PropertyName("autoPlay") val autoPlay: Boolean = true,
    @get:PropertyName("showAvatars") val showAvatars: Boolean = true, // Показывать аватары в диалоге
    @get:PropertyName("leftSpeaker") val leftSpeaker: String = "Собеседник 1", // Имя левого говорящего
    @get:PropertyName("rightSpeaker") val rightSpeaker: String = "Собеседник 2" // Имя правого говорящего
) : Task() {
    override val type: String = "DIALOGUE"
}

@Parcelize
data class DialogueSet(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("question") val question: String = "Диалоги для прослушивания",
    @get:PropertyName("dialogues") val dialogues: Map<String, List<DialogueLine>>,
    @get:PropertyName("type") override val type: String = "DIALOGUE_SET",
    @get:PropertyName("defaultLeftSpeaker") val defaultLeftSpeaker: String = "Собеседник 1",
    @get:PropertyName("defaultRightSpeaker") val defaultRightSpeaker: String = "Собеседник 2"
) : Task()

@Parcelize
data class DialogueLine(
    @get:PropertyName("sound") val sound: String,
    @get:PropertyName("text") val text: String,
    @get:PropertyName("speaker") val speaker: String = "", // Имя говорящего (например, "Анна", "Иван")
    @get:PropertyName("isLeft") val isLeft: Boolean = true, // Позиция сообщения (true - слева, false - справа)
    @get:PropertyName("avatar") val avatar: String? = null, // URL аватара говорящего (опционально)
    @get:PropertyName("timestamp") val timestamp: String? = null // Время сообщения (опционально)
) : Parcelable {
}

@Parcelize
data class ImageInputSet(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("question") val question: String = "Ответьте на вопросы по картинкам",
    @get:PropertyName("tasks") val tasks: List<ImageInputTask>,
    @get:PropertyName("type") override val type: String = "IMAGE_INPUT_SET"
) : Task()

@Parcelize
sealed class MarkdownElement : Parcelable {
    @Parcelize
    data class Heading(val level: Int, val text: String) : MarkdownElement()
    @Parcelize
    data class Paragraph(val text: String) : MarkdownElement()
    @Parcelize
    data class Image(val url: String, val altText: String = "") : MarkdownElement()
    @Parcelize
    data class ListItem(val text: String, val isOrdered: Boolean = false) : MarkdownElement()
    @Parcelize
    data class CodeBlock(val code: String, val language: String = "") : MarkdownElement()
    @Parcelize
    data class Blockquote(val text: String) : MarkdownElement()
    @Parcelize
    object HorizontalRule : MarkdownElement()
}

@Parcelize
data class InteractiveElement(
    @get:PropertyName("type") val type: String, // "question", "quiz", "example", "task"
    @get:PropertyName("content") val content: String,
    @get:PropertyName("options") val options: List<String> = emptyList(),
    @get:PropertyName("correctAnswer") val correctAnswer: String = "",
    @get:PropertyName("hint") val hint: String? = null
) : Parcelable

// Обновляем TheoryTask
@Parcelize
data class TheoryTask(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("image") val image: String = "",
    @get:PropertyName("title") val title: String = "",
    @get:PropertyName("text") val text: String = "",
    @get:PropertyName("content") val markdownContent: String = "", // Новое поле для Markdown
    val parsedMarkdown: List<MarkdownElement> = emptyList(), // Парсированный Markdown
    @get:PropertyName("interactiveElements") val interactiveElements: List<InteractiveElement> = emptyList()
) : Task() {
    override val type: String = "THEORY"

    val lazyParsedMarkdown: List<MarkdownElement> by lazy {
        if (markdownContent.isNotEmpty()) {
            com.example.langapp.utils.MarkdownParser.parseMarkdown(markdownContent)
        } else if (text.isNotEmpty()) {
            listOf(MarkdownElement.Paragraph(text))
        } else {
            emptyList()
        }
    }
}