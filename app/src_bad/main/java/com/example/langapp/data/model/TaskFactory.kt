package com.example.langapp.data

import com.example.langapp.data.model.Task
import com.example.langapp.data.model.TaskType
import com.example.langapp.data.model.*

object TaskFactory {
    fun createTask(data: Map<String, Any>): Task {
        return when (TaskType.valueOf(data["type"] as String)) {
            TaskType.MULTIPLE_CHOICE -> createMultipleChoice(data)
            TaskType.IMAGE_INPUT -> createImageInput(data)
            TaskType.TEXT_INPUT -> createTextInput(data)
            TaskType.AUDIO_INPUT -> createAudioInput(data)
            TaskType.IMAGE_AUDIO -> createImageAudio(data)
            TaskType.IMAGE_RECORDING -> createImageRecording(data)
            TaskType.AUDIO_RECORDING -> createAudioRecording(data)
            TaskType.TEXT_RECORDING -> createTextRecording(data)
            TaskType.THEORY -> createTheory(data)
        }
    }

    private fun createMultipleChoice(data: Map<String, Any>) = MultipleChoiceTask(
        id = data["id"] as String,
        number = data["number"] as Int,
        question = data["question"] as String,
        options = data["options"] as List<String>,
        correctAnswer = data["correctAnswer"] as String,
        explanation = data["explanation"] as? String
    )

    private fun createImageInput(data: Map<String, Any>) = ImageInputTask(
        id = data["id"] as String,
        number = data["number"] as Int,
        imageUrl = data["imageUrl"] as String,
        acceptableAnswers = data["acceptableAnswers"] as List<String>,
        hint = data["hint"] as? String
    )

    // Остальные фабричные методы по аналогии
    private fun createTextInput(data: Map<String, Any>) = TextInputTask(...)
    private fun createAudioInput(data: Map<String, Any>) = AudioInputTask(...)
    private fun createImageAudio(data: Map<String, Any>) = ImageAudioTask(...)
    private fun createImageRecording(data: Map<String, Any>) = ImageRecordingTask(...)
    private fun createAudioRecording(data: Map<String, Any>) = AudioRecordingTask(...)
    private fun createTextRecording(data: Map<String, Any>) = TextRecordingTask(...)
    private fun createTheory(data: Map<String, Any>) = TheoryTask(...)
}
