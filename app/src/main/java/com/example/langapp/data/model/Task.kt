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
    @get:PropertyName("audioPrompt") val audioPrompt: String,
    @get:PropertyName("targetText") val targetText: String,
    @get:PropertyName("maxAttempts") val maxAttempts: Int = 3
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

// 9. Теория
@Parcelize
data class TheoryTask(
    @get:PropertyName("name") override val taskname: String,
    @get:PropertyName("id") override val id: String,
    @get:PropertyName("image") val image: String,
    @get:PropertyName("title") val title: String,
    @get:PropertyName("text") val text: String,
    @get:PropertyName("interactiveElements") val interactiveElements: List<Question> = emptyList()
) : Task() {
    override val type: String = "THEORY"
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
