// data/repository/FirebaseRepository.kt
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
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private val _instance: FirebaseRepository by lazy { FirebaseRepository() }
        fun getInstance(): FirebaseRepository = _instance
    }

    fun getTasks(level: String, lesson: Int, taskType: String): Flow<List<Task>> = callbackFlow {
        // Проверяем аутентификацию
        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val reference = database.getReference("Lessons/$level/$lesson/$taskType")

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
                // Логируем ошибку, но не закрываем поток
                println("Firebase error: ${error.message}")
                trySend(emptyList())
            }
        }

        reference.addValueEventListener(listener)

        awaitClose {
            reference.removeEventListener(listener)
        }
    }

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
                audioPrompt = data["audioPrompt"] as? String ?: "",
                targetText = data["targetText"] as? String ?: "",
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

            else -> null
        }
    }

    fun saveUserProgress(level: String, lesson: Int, taskId: String, score: Int) {
        val currentUser = auth.currentUser ?: return

        val progressRef = database.getReference("UserProgress/${currentUser.uid}/$level/$lesson")
        progressRef.get().addOnSuccessListener { snapshot ->
            val currentProgress = if (snapshot.exists()) {
                snapshot.getValue(UserProgress::class.java) ?: UserProgress()
            } else {
                UserProgress()
            }

            currentProgress.completedTasks = currentProgress.completedTasks + taskId
            currentProgress.score = maxOf(currentProgress.score ?: 0, score)
            currentProgress.completed = currentProgress.completedTasks.size >= 3

            progressRef.setValue(currentProgress)
        }
    }
}