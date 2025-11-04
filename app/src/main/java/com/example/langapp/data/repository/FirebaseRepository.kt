package com.example.langapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.langapp.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseRepository {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private val _instance: FirebaseRepository by lazy { FirebaseRepository() }
        fun getInstance(): FirebaseRepository = _instance
    }

    // Получение всех доступных разделов для урока
    // data/repository/FirebaseRepository.kt
    fun getAvailableSections(level: String, lesson: Int): Flow<List<Section>> = callbackFlow {
        val reference = database.getReference("Lessons/$level/$lesson/sections")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sections = mutableListOf<Section>()

                if (!snapshot.exists()) {
                    trySend(emptyList())
                    return
                }

                snapshot.children.forEach { sectionSnapshot ->
                    val sectionData = sectionSnapshot.value as? Map<String, Any>
                    sectionData?.let { data ->
                        val section = createSectionFromData(data, sectionSnapshot.key ?: "")
                        section?.let { sections.add(it) }
                    }
                }

                trySend(sections)
            }

            override fun onCancelled(error: DatabaseError) {
                println("Firebase error: ${error.message}")
                trySend(emptyList())
            }
        }

        reference.addValueEventListener(listener)
        awaitClose {
            reference.removeEventListener(listener)
        }
    }

    // Получение задач из конкретного раздела
    fun getTasks(level: String, lesson: Int, sectionId: String): Flow<List<Task>> = callbackFlow {
        val reference = database.getReference("Lessons/$level/$lesson/sections/$sectionId/tasks")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = mutableListOf<Task>()

                if (!snapshot.exists()) {
                    trySend(emptyList())
                    return
                }

                snapshot.children.forEach { taskSnapshot ->
                    val taskData = taskSnapshot.value as? Map<String, Any>
                    taskData?.let { data ->
                        val task = createTaskFromData(data, taskSnapshot.key ?: "")
                        task?.let { tasks.add(it) }
                    }
                }

                trySend(tasks)
            }

            override fun onCancelled(error: DatabaseError) {
                println("Firebase error: ${error.message}")
                trySend(emptyList())
            }
        }

        reference.addValueEventListener(listener)
        awaitClose {
            reference.removeEventListener(listener)
        }
    }

    private fun createSectionFromData(data: Map<String, Any>, id: String): Section? {
        return try {
            Section(
                id = id,
                name = data["name"] as? String ?: "",
                description = data["description"] as? String ?: "",
                tasks = emptyList() // Задачи загружаются отдельно
            )
        } catch (e: Exception) {
            null
        }
    }

    // Существующий метод создания задач (без изменений)
    private fun createTaskFromData(data: Map<String, Any>, id: String): Task? {
        return when (data["type"] as? String) {
            "MULTIPLE_CHOICE" -> MultipleChoiceTask(
                taskname = data["name"] as? String ?: "",
                id = id,
                question = data["question"] as? String ?: "",
                options = (data["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                correctAnswerIndex = (data["correctAnswer"] as? Long)?.toInt() ?: 0,
                audioHint = data["audioHint"] as? String,
                explanation = data["explanation"] as? String,
                image = data["image"] as? String // Добавляем изображение
            )

            "IMAGE_INPUT" -> ImageInputTask(
                taskname = data["name"] as? String ?: "",
                id = id,
                question = data["question"] as? String ?: "",
                image = data["image"] as? String ?: "",
                correctAnswer = data["correctAnswer"] as? String ?: "",
                hint = data["hint"] as? String
            )

            "TEXT_INPUT" -> TextInputTask(
                taskname = data["name"] as? String ?: "",
                id = id,
                question = data["question"] as? String ?: "",
                correctAnswer = data["correctAnswer"] as? String ?: "",
                audioSupport = data["audioSupport"] as? String
            )

            "TEXT_RECORDING" -> TextRecordingTask(
                taskname = data["name"] as? String ?: "",
                id = id,
                text = data["text"] as? String ?: "",
                referenceAudio = data["referenceAudio"] as? String ?: "",
                phoneticHint = data["phoneticHint"] as? String ?: "",
                checkPronunciation = data["checkPronunciation"] as? Boolean ?: true
            )

            "AUDIO_RECORDING" -> AudioRecordingTask(
                taskname = data["name"] as? String ?: "",
                id = id,
                question = data["question"] as? String ?: "Повторите слово",
                audioPrompt = data["referenceAudio"] as? String ?: "",
                targetText = data["targetText"] as? String ?: (data["textHint"] as? String ?: ""),
                textHint = data["textHint"] as? String,
                maxAttempts = (data["maxAttempts"] as? Long)?.toInt() ?: 3,
                image = data["image"] as? String // Добавляем изображение
            )

            "THEORY" -> TheoryTask(
                taskname = data["name"] as? String ?: "",
                id = id,
                image = data["image"] as? String ?: "",
                title = data["title"] as? String ?: "",
                text = data["text"] as? String ?: "",
                interactiveElements = emptyList()
            )

            "ALPHABET" -> AlphabetTask(
                taskname = data["name"] as? String ?: "",
                id = id,
                question = data["question"] as? String ?: "Нажимай и повторяй",
                letters = parseLetters(data["letters"] as? Map<String, Map<String, String>>),
                autoPlay = data["autoPlay"] as? Boolean ?: true
            )

            "SYLLABLE" -> SyllableTask(
                taskname = data["name"] as? String ?: "",
                id = id,
                question = data["question"] as? String ?: "Нажимай на буквы и слоги",
                letters = parseSyllableLetters(data)
            )

            "AUDIO_RECORDING_SET" -> {
                val tasksData = data["tasks"] as? Map<String, Any> ?: emptyMap()
                val audioTasks = mutableListOf<AudioRecordingTask>()

                tasksData.forEach { (taskKey, taskValue) ->
                    when (taskValue) {
                        is String -> {
                            // Старый формат: "Вы завтракаете": "audio_url"
                            audioTasks.add(
                                AudioRecordingTask(
                                    taskname = taskKey,
                                    id = "${id}_${taskKey.hashCode()}",
                                    question = "Повторите фразу",
                                    audioPrompt = taskValue,
                                    targetText = taskKey,
                                    textHint = taskKey,
                                    maxAttempts = 3,
                                    image = null
                                )
                            )
                        }
                        is Map<*, *> -> {
                            // Новый формат: "audio_task_1": { "name": "...", "referenceAudio": "...", ... }
                            val taskMap = taskValue as Map<String, Any>
                            audioTasks.add(
                                AudioRecordingTask(
                                    taskname = taskMap["name"] as? String ?: taskKey,
                                    id = "${id}_${taskKey}",
                                    question = taskMap["question"] as? String ?: "Повторите фразу",
                                    audioPrompt = taskMap["referenceAudio"] as? String ?: "",
                                    targetText = taskMap["targetText"] as? String ?: "",
                                    textHint = taskMap["textHint"] as? String,
                                    maxAttempts = (taskMap["maxAttempts"] as? Long)?.toInt() ?: 3,
                                    image = taskMap["image"] as? String
                                )
                            )
                        }
                    }
                }

                AudioRecordingSet(
                    taskname = data["name"] as? String ?: "",
                    id = id,
                    tasks = audioTasks
                )
            }

            "MULTIPLE_CHOICE_SET" -> {
                val tasksData = data["tasks"] as? Map<String, Map<String, Any>> ?: emptyMap()
                val multipleChoiceTasks = tasksData.mapNotNull { (taskId, taskData) ->
                    createTaskFromData(taskData, taskId) as? MultipleChoiceTask
                }
                MultipleChoiceSet(
                    taskname = data["name"] as? String ?: "",
                    id = id,
                    tasks = multipleChoiceTasks
                )
            }

            else -> null
        }
    }
}

private fun parseLetters(lettersMap: Map<String, Map<String, String>>?): List<AlphabetLetter> {
    if (lettersMap == null) return emptyList()

    val russianAlphabetOrder = listOf(
        "А", "Б", "В", "Г", "Д", "Е", "Ё", "Ж", "З", "И", "Й",
        "К", "Л", "М", "Н", "О", "П", "Р", "С", "Т", "У", "Ф",
        "Х", "Ц", "Ч", "Ш", "Щ", "Ъ", "Ы", "Ь", "Э", "Ю", "Я"
    )

    return lettersMap.map { (_, letterData) ->
        AlphabetLetter(
            image = letterData["image"] ?: "",
            sound = letterData["sound"] ?: "",
            letter = letterData["letter"] ?: ""
        )
    }.sortedBy { letter ->
        russianAlphabetOrder.indexOf(letter.letter)
    }
}

private fun parseSyllableLetters(data: Map<String, Any>): List<SyllableLetter> {
    val letters = mutableListOf<SyllableLetter>()

    // Парсим согласные ИЛИ гласные в зависимости от данных
    val consonants = data["consonants"] as? Map<String, List<Map<String, String>>>
    val vowels = data["vowels"] as? Map<String, List<Map<String, String>>>

    val sourceData = consonants ?: vowels ?: return emptyList()

    sourceData.forEach { (letter, items) ->
        letters.add(SyllableLetter(
            letter = letter,
            items = items.map { itemData ->
                SyllableItem(
                    sound = itemData["sound"] ?: "",
                    text = itemData["text"] ?: ""
                )
            }
        ))
    }

    // Сортируем по алфавиту
    return letters.sortedBy { it.letter }
}