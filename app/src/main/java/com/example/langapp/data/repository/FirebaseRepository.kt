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
                explanation = data["explanation"] as? String
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
                maxAttempts = (data["maxAttempts"] as? Long)?.toInt() ?: 3
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