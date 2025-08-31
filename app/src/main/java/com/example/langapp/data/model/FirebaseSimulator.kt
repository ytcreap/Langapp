package com.example.langapp.data.model

object FirebaseSimulator {
    private val simulatedData = mapOf(
        "elementary" to mapOf(
            "1" to mapOf(
                "phonetics" to listOf(
                    // Текст + запись (произношение слова)
                    mapOf(
                        "name" to "Звук",
                        "id" to "ph1_1_rec",
                        "type" to "TEXT_RECORDING",
                        "text" to "Яблоко",
                        "referenceAudio" to "apple_pron",
                        "phoneticHint" to "[йаблако]"
                    ),

                    // Множественный выбор (выбор правильного звука)
                    mapOf(
                        "name" to "Задание 1",
                        "id" to "ph1_2_mc",
                        "type" to "MULTIPLE_CHOICE",
                        "question" to "Выберите правильное произношение",
                        "options" to listOf("[йаблако]", "[йэблоко]", "[йаблоко]"),
                        "correctAnswer" to 0,
                        "audioHint" to "apple_pron"
                    ),

                    // Аудио + запись (повторение)
                    mapOf(
                        "name" to "Задание 2",
                        "id" to "ph1_3_ar",
                        "type" to "AUDIO_RECORDING",
                        "referenceAudio" to "milk_pron",
                        "textHint" to "Молоко"
                    )


                ),

                "vocabulary" to listOf(
                    // Картинка + ввод текста
                    mapOf(
                        "name" to "Теория 1",
                        "id" to "voc1_img_input",
                        "type" to "IMAGE_INPUT",
                        "image" to "img_table",
                        "correctAnswer" to "стол",
                        "hint" to "Предмет мебели с плоской поверхностью"
                    ),

                    // Теория (объяснение слова)
                    mapOf(
                        "name" to "Теория 2",
                        "id" to "voc1_theory",
                        "type" to "THEORY",
                        "image" to "img_chair",
                        "text" to "Стул - предмет мебели для сидения одного человека...",
                        "title" to "ВНИМАНИЕ: ТЕОРИЯ"
                    )
                )
            )
        )
    )

    fun getTasks(level: String, lesson: Int, taskType: String): List<Task> {
        return simulatedData[level.lowercase()]
            ?.get(lesson.toString())
            ?.get(taskType.lowercase())
            ?.map { taskData ->
                when (taskData["type"] as String) {
                    "MULTIPLE_CHOICE" -> MultipleChoiceTask(
                        taskname = taskData["name"] as String,
                        id = taskData["id"] as String,
                        question = taskData["question"] as String,
                        options = taskData["options"] as List<String>,
                        correctAnswerIndex = taskData["correctAnswer"] as Int,
                        audioHint = taskData["audioHint"] as? String,
                        explanation = taskData["explanation"] as? String
                    )

                    "IMAGE_INPUT" -> ImageInputTask(
                        taskname = taskData["name"] as String,
                        id = taskData["id"] as String,
                        image = taskData["image"] as String,
                        correctAnswer = taskData["correctAnswer"] as String,
                        hint = taskData["hint"] as? String
                    )

                    "TEXT_INPUT" -> TextInputTask(
                        taskname = taskData["name"] as String,
                        id = taskData["id"] as String,
                        textPrompt = taskData["textPrompt"] as String,
                        correctAnswer = taskData["correctAnswer"] as String,
                        audioSupport = taskData["audioSupport"] as? String
                    )

                    "AUDIO_INPUT" -> AudioInputTask(
                        taskname = taskData["name"] as String,
                        id = taskData["id"] as String,
                        audioPrompt = taskData["audioPrompt"] as String,
                        correctAnswer = taskData["correctAnswer"] as String,
                        textHint = taskData["textHint"] as? String
                    )

                    "IMAGE_AUDIO" -> ImageAudioTask(
                        taskname = taskData["name"] as String,
                        id = taskData["id"] as String,
                        image = taskData["image"] as String,
                        audio = taskData["audio"] as String,
                        questions = (taskData["questions"] as? List<Map<String, Any>>)?.map { q ->
                            Question(
                                taskname = taskData["name"] as String,
                                question = q["question"] as String,
                                options = q["options"] as? List<String> ?: emptyList(),
                                answer = q["answer"] as String,
                                hint = q["hint"] as? String
                            )
                        } ?: emptyList()
                    )

                    "IMAGE_RECORDING" -> ImageRecordingTask(
                        taskname = taskData["name"] as String,
                        id = taskData["id"] as String,
                        image = taskData["image"] as String,
                        targetText = taskData["targetText"] as String,
                        referenceAudio = taskData["referenceAudio"] as? String,
                        similarityThreshold = (taskData["similarityThreshold"] as? Number)?.toFloat() ?: 0.7f
                    )

                    "AUDIO_RECORDING" -> AudioRecordingTask(
                        taskname = taskData["name"] as String,
                        id = taskData["id"] as String,
                        audioPrompt = taskData["audioPrompt"] as? String?: "",
                        targetText = taskData["targetText"] as? String?: "",
                        maxAttempts = (taskData["maxAttempts"] as? Int) ?: 3
                    )

                    "TEXT_RECORDING" -> TextRecordingTask(
                        taskname = taskData["name"] as String,
                        id = taskData["id"] as String,
                        text = taskData["text"] as String,
                        referenceAudio = taskData["referenceAudio"] as String,
                        phoneticHint = taskData["phoneticHint"] as String,
                        checkPronunciation = taskData["checkPronunciation"] as? Boolean ?: true
                    )

                    "THEORY" -> TheoryTask(
                        taskname = taskData["name"] as String,
                        id = taskData["id"] as String,
                        image = taskData["image"] as String,
                        title = taskData["title"] as? String ?: "",
                        text = taskData["text"] as? String ?: "",
                        interactiveElements = (taskData["interactiveElements"] as? List<Map<String, Any>>)?.map { q ->
                            Question(
                                taskname = taskData["name"] as String,
                                question = q["question"] as String,
                                options = q["options"] as? List<String> ?: emptyList(),
                                answer = q["answer"] as String,
                                hint = q["hint"] as? String
                            )
                        } ?: emptyList()
                    )

                    else -> throw IllegalArgumentException("Unknown task type: ${taskData["type"]}")
                }
            } ?: emptyList()
    }

}
