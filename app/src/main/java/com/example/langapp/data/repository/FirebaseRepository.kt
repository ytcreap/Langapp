package com.example.langapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.langapp.data.model.*
import com.example.langapp.utils.MarkdownParser
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class FirebaseRepository {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val apiScope = CoroutineScope(Dispatchers.IO)
    private var apiBaseUrl = "http://10.0.2.2:3000/api"

    companion object {
        private val _instance: FirebaseRepository by lazy { FirebaseRepository() }
        fun getInstance(): FirebaseRepository = _instance
    }

    fun configureApiBaseUrl(baseUrl: String) {
        apiBaseUrl = baseUrl.trimEnd('/')
    }

    fun getAvailableSectionsFromApi(level: String, lesson: Int): Flow<List<Section>> = flow {
        emit(fetchAvailableSectionsFromApi(level, lesson))
    }

    fun getAvailableSectionsSingleFromApi(level: String, lesson: Int, callback: (List<Section>) -> Unit) {
        apiScope.launch {
            val sections = try {
                fetchAvailableSectionsFromApi(level, lesson)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
            withContext(Dispatchers.Main) {
                callback(sections)
            }
        }
    }

    fun getTasksFromApi(level: String, lesson: Int, sectionId: String): Flow<List<Task>> = flow {
        emit(fetchTasksFromApi(level, lesson, sectionId))
    }

    fun checkIfLessonExistsFromApi(level: String, lesson: Int, callback: (Boolean) -> Unit) {
        apiScope.launch {
            val exists = try {
                val response = apiGet("lessons/${encodePath(level)}/$lesson/exists") as? Map<*, *>
                response?.get("exists") as? Boolean ?: false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
            withContext(Dispatchers.Main) {
                callback(exists)
            }
        }
    }

    fun getFirstSectionOfLessonFromApi(level: String, lesson: Int): Flow<Section?> = flow {
        val response = apiGet("lessons/${encodePath(level)}/$lesson/sections/first")
        emit(parseSectionFromApi(response))
    }

    fun getUserProfileFromApi(userId: String, callback: (UserProfile?) -> Unit) {
        apiScope.launch {
            val profile = try {
                val response = apiGet("users/${encodePath(userId)}") as? Map<String, Any>
                response?.let { createUserProfileFromData(userId, it) }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            withContext(Dispatchers.Main) {
                callback(profile)
            }
        }
    }

    fun getUserProgressFromApi(userId: String, level: String, callback: (Int) -> Unit) {
        apiScope.launch {
            val progress = try {
                val response = apiGet("users/${encodePath(userId)}/progress/${encodePath(level)}") as? Map<*, *>
                (response?.get("progress") as? Number)?.toInt() ?: 0
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
            withContext(Dispatchers.Main) {
                callback(progress)
            }
        }
    }

    fun updateUserProfileViaApi(userId: String, fullName: String, group: String, callback: (Boolean, String?) -> Unit) {
        apiScope.launch {
            val result = try {
                val body = JSONObject()
                    .put("fullName", fullName)
                    .put("group", group)
                    .toString()
                apiRequest("PATCH", "users/${encodePath(userId)}", body)
                true to null
            } catch (e: Exception) {
                e.printStackTrace()
                false to e.message
            }
            withContext(Dispatchers.Main) {
                callback(result.first, result.second)
            }
        }
    }

    fun createUserProfileViaApiIfNeeded(userId: String, email: String, fullName: String? = null, group: String? = null) {
        apiScope.launch {
            try {
                val body = JSONObject()
                    .put("email", email)
                    .put("fullName", fullName)
                    .put("group", group)
                    .toString()
                apiRequest("POST", "users/${encodePath(userId)}/ensure", body)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun fetchAvailableSectionsFromApi(level: String, lesson: Int): List<Section> {
        val response = apiGet("lessons/${encodePath(level)}/$lesson/sections") as? List<*> ?: return emptyList()
        return response.mapNotNull { parseSectionFromApi(it) }
    }

    private suspend fun fetchTasksFromApi(level: String, lesson: Int, sectionId: String): List<Task> {
        val response = apiGet("lessons/${encodePath(level)}/$lesson/sections/${encodePath(sectionId)}/tasks") as? List<*> ?: return emptyList()
        return response.mapNotNull { taskData ->
            val data = taskData as? Map<String, Any> ?: return@mapNotNull null
            val id = data["id"] as? String ?: return@mapNotNull null
            createTaskFromData(data, id)
        }
    }

    private fun parseSectionFromApi(value: Any?): Section? {
        val data = value as? Map<String, Any> ?: return null
        val id = data["id"] as? String ?: return null
        return createSectionFromData(data, id)
    }

    private suspend fun apiGet(path: String): Any? {
        return apiRequest("GET", path)
    }

    private suspend fun apiRequest(method: String, path: String, body: String? = null): Any? = withContext(Dispatchers.IO) {
        val url = URL("${apiBaseUrl.trimEnd('/')}/${path.trimStart('/')}")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }

        try {
            if (body != null) {
                connection.outputStream.use { output ->
                    output.write(body.toByteArray(Charsets.UTF_8))
                }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (status !in 200..299) {
                throw IllegalStateException("API request failed with HTTP $status: $text")
            }

            parseJsonResponse(text)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseJsonResponse(text: String): Any? {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed == "null") {
            return null
        }

        return when (trimmed.first()) {
            '[' -> jsonArrayToList(JSONArray(trimmed))
            '{' -> jsonObjectToMap(JSONObject(trimmed))
            else -> trimmed
        }
    }

    private fun jsonObjectToMap(json: JSONObject): Map<String, Any> {
        val map = LinkedHashMap<String, Any>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            jsonToValue(json.get(key))?.let { value ->
                map[key] = value
            }
        }
        return map
    }

    private fun jsonArrayToList(json: JSONArray): List<Any> {
        val list = ArrayList<Any>()
        for (index in 0 until json.length()) {
            jsonToValue(json.get(index))?.let { value ->
                list.add(value)
            }
        }
        return list
    }

    private fun jsonToValue(value: Any?): Any? {
        return when (value) {
            null, JSONObject.NULL -> null
            is JSONObject -> jsonObjectToMap(value)
            is JSONArray -> jsonArrayToList(value)
            is Int -> value.toLong()
            is Long -> value
            is Double -> if (value % 1.0 == 0.0) value.toLong() else value
            is Float -> if (value % 1.0f == 0.0f) value.toLong() else value.toDouble()
            else -> value
        }
    }

    private fun encodePath(value: String): String {
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }

    // Получение всех доступных разделов для урока
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

    //
    fun getAvailableSectionsSingle(level: String, lesson: Int, callback: (List<Section>) -> Unit) {
        val reference = database.getReference("Lessons/$level/$lesson/sections")

        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sections = mutableListOf<Section>()

                if (!snapshot.exists()) {
                    callback(emptyList())
                    return
                }

                snapshot.children.forEach { sectionSnapshot ->
                    val sectionData = sectionSnapshot.value as? Map<String, Any>
                    sectionData?.let { data ->
                        val section = createSectionFromData(data, sectionSnapshot.key ?: "")
                        section?.let { sections.add(it) }
                    }
                }

                callback(sections)
            }

            override fun onCancelled(error: DatabaseError) {
                println("Firebase error: ${error.message}")
                callback(emptyList())
            }
        })
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
                taskname = data["name"] as? String ?: "Задание ${id}",
                id = id,
                question = data["question"] as? String ?: "",
                options = (data["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                correctAnswerIndex = (data["correctAnswer"] as? Long)?.toInt() ?: 0,
                audioHint = data["audioHint"] as? String,
                explanation = data["explanation"] as? String,
                image = data["image"] as? String // Добавляем изображение
            )

            "IMAGE_INPUT" -> ImageInputTask(
                taskname = data["name"] as? String ?: "Задание ${id}",
                id = id,
                question = data["question"] as? String ?: "",
                image = data["image"] as? String ?: "",
                correctAnswer = data["correctAnswer"] as? String ?: "",
                hint = data["hint"] as? String
            )

            "TEXT_INPUT" -> TextInputTask(
                taskname = data["name"] as? String ?: "Задание ${id}",
                id = id,
                question = data["question"] as? String ?: "",
                correctAnswer = data["correctAnswer"] as? String ?: "",
                audioSupport = data["audioSupport"] as? String
            )

            "TEXT_RECORDING" -> TextRecordingTask(
                taskname = data["name"] as? String ?: "Задание ${id}",
                id = id,
                text = data["text"] as? String ?: "",
                referenceAudio = data["referenceAudio"] as? String ?: "",
                phoneticHint = data["phoneticHint"] as? String ?: "",
                checkPronunciation = data["checkPronunciation"] as? Boolean ?: true
            )

            "AUDIO_RECORDING" -> AudioRecordingTask(
                taskname = data["name"] as? String ?: "Задание ${id}",
                id = id,
                question = data["question"] as? String ?: "Повторите слово",
                audioPrompt = data["referenceAudio"] as? String ?: "",
                targetText = data["targetText"] as? String ?: (data["textHint"] as? String ?: ""),
                textHint = data["textHint"] as? String,
                maxAttempts = (data["maxAttempts"] as? Long)?.toInt() ?: 3,
                image = data["image"] as? String // Добавляем изображение
            )

            "IMAGE_RECORDING" -> ImageRecordingTask(
                taskname = data["name"] as? String ?: "Recording task ${id}",
                id = id,
                image = data["image"] as? String ?: "",
                targetText = data["targetText"] as? String ?: (data["textHint"] as? String ?: ""),
                referenceAudio = data["referenceAudio"] as? String,
                similarityThreshold = (data["similarityThreshold"] as? Number)?.toFloat() ?: 0.7f
            )

            "ALPHABET" -> AlphabetTask(
                taskname = data["name"] as? String ?: "Задание ${id}",
                id = id,
                question = data["question"] as? String ?: "Нажимай и повторяй",
                letters = parseLetters(data["letters"] as? Map<String, Map<String, String>>),
                autoPlay = data["autoPlay"] as? Boolean ?: true
            )

            "SYLLABLE" -> SyllableTask(
                taskname = data["name"] as? String ?: "Задание ${id}",
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
                                    taskname = data["name"] as? String ?: "Задание ${id}",
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
                    taskname = data["name"] as? String ?: "Задание ${id}",
                    id = id,
                    tasks = audioTasks
                )
            }

            "IMAGE_RECORDING_SET" -> {
                val tasksData = data["tasks"] as? Map<String, Map<String, Any>> ?: emptyMap()
                val imageRecordingTasks = tasksData.mapNotNull { (taskId, taskData) ->
                    createTaskFromData(taskData + ("type" to "IMAGE_RECORDING"), taskId) as? ImageRecordingTask
                }
                ImageRecordingSet(
                    taskname = data["name"] as? String ?: "Recording set ${id}",
                    id = id,
                    tasks = imageRecordingTasks
                )
            }

            "TEXT_RECORDING_SET" -> {
                val tasksData = data["tasks"] as? Map<String, Map<String, Any>> ?: emptyMap()
                val textRecordingTasks = tasksData.mapNotNull { (taskId, taskData) ->
                    createTaskFromData(taskData + ("type" to "TEXT_RECORDING"), taskId) as? TextRecordingTask
                }
                TextRecordingSet(
                    taskname = data["name"] as? String ?: "Recording set ${id}",
                    id = id,
                    tasks = textRecordingTasks
                )
            }

            "MULTIPLE_CHOICE_SET" -> {
                val tasksData = data["tasks"] as? Map<String, Map<String, Any>> ?: emptyMap()
                val multipleChoiceTasks = tasksData.mapNotNull { (taskId, taskData) ->
                    createTaskFromData(taskData, taskId) as? MultipleChoiceTask
                }
                MultipleChoiceSet(
                    taskname = data["name"] as? String ?: "Задание ${id}",
                    id = id,
                    tasks = multipleChoiceTasks
                )
            }

            "IMAGE_AUDIO_SET" -> {
                // Устаревший тип - преобразуем в новый VocabularyImageAudioSet если нужно
                val vocabulary = data["vocabulary"] as? Map<String, Map<String, String>> ?: emptyMap()

                VocabularyImageAudioSet(
                    taskname = data["name"] as? String ?: "Задание ${id}",
                    id = id,
                    question = data["question"] as? String ?: "Слушай и запоминай слова",
                    vocabulary = vocabulary
                )
            }

            "VOCABULARY_IMAGE_AUDIO_SET" -> {
                // Получаем вложенные задачи
                val tasksMap = data["tasks"] as? Map<String, Map<String, String>> ?: emptyMap()

                // Преобразуем в LinkedHashMap для сохранения порядка
                val linkedTasksMap = LinkedHashMap(tasksMap)

                VocabularyImageAudioSet(
                    taskname = data["name"] as? String ?: "Задание ${id}",
                    id = id,
                    question = data["question"] as? String ?: "Слушай и запоминай слова",
                    vocabulary = linkedTasksMap
                )
            }

            "DIALOGUE" -> {
                val dialogueData = data["dialogue"] as? List<Map<String, String>> ?: emptyList()
                val dialogueLines = dialogueData.mapNotNull { lineData ->
                    DialogueLine(
                        sound = lineData["sound"] ?: "",
                        text = lineData["text"] ?: ""
                    )
                }

                DialogueTask(
                    taskname = data["name"] as? String ?: "Задание ${id}",
                    id = id,
                    question = data["question"] as? String ?: "Послушайте диалог",
                    dialogue = dialogueLines,
                    image = data["image"] as? String,
                    autoPlay = data["autoPlay"] as? Boolean ?: true
                )
            }

            "DIALOGUE_SET" -> {
                val dialoguesData = data["dialogues"] as? Map<String, List<Map<String, String>>> ?: emptyMap()
                val parsedDialogues = mutableMapOf<String, List<DialogueLine>>()

                dialoguesData.forEach { (dialogKey, dialogLines) ->
                    val lines = dialogLines.mapNotNull { lineData ->
                        DialogueLine(
                            sound = lineData["sound"] ?: "",
                            text = lineData["text"] ?: ""
                        )
                    }
                    parsedDialogues[dialogKey] = lines
                }

                DialogueSet(
                    taskname = data["name"] as? String ?: "Задание ${id}",
                    id = id,
                    question = data["question"] as? String ?: "Нажмите на диалог чтобы увидеть реплики",
                    dialogues = parsedDialogues
                )
            }

            "TEXT_INPUT_SET" -> {
                val tasksData = data["tasks"] as? Map<String, Map<String, Any>> ?: emptyMap()
                val textInputTasks = tasksData.mapNotNull { (taskId, taskData) ->
                    TextInputTask(
                        taskname = data["name"] as? String ?: "Задание ${id}",
                        id = "${id}_$taskId",
                        question = taskData["question"] as? String ?: "",
                        correctAnswer = taskData["correctAnswer"] as? String ?: "",
                        audioSupport = taskData["audioSupport"] as? String
                    )
                }
                TextInputSet(
                    taskname = data["name"] as? String ?: "Задание ${id}",
                    id = id,
                    question = data["question"] as? String ?: "Ответьте на вопросы",
                    tasks = textInputTasks
                )
            }

            "IMAGE_INPUT_SET" -> {
                val tasksData = data["tasks"] as? Map<String, Map<String, Any>> ?: emptyMap()
                val imageInputTasks = tasksData.mapNotNull { (taskId, taskData) ->
                    ImageInputTask(
                        taskname = data["name"] as? String ?: "Задание ${id}",
                        id = "${id}_$taskId",
                        question = taskData["question"] as? String ?: "",
                        image = taskData["image"] as? String ?: "",
                        correctAnswer = taskData["correctAnswer"] as? String ?: "",
                        hint = taskData["hint"] as? String
                    )
                }
                ImageInputSet(
                    taskname = data["name"] as? String ?: "Задание ${id}",
                    id = id,
                    question = data["question"] as? String ?: "Ответьте на вопросы по картинкам",
                    tasks = imageInputTasks
                )
            }

            "Theory" -> {
                val markdownContent = data["content"] as? String ?: ""
                val parsedMarkdown = if (markdownContent.isNotEmpty()) {
                    com.example.langapp.utils.MarkdownParser.parseMarkdown(markdownContent)
                } else {
                    // Для обратной совместимости
                    val text = data["text"] as? String ?: ""
                    if (text.isNotEmpty()) {
                        listOf(MarkdownElement.Paragraph(text))
                    } else {
                        emptyList()
                    }
                }

                // Парсим интерактивные элементы если есть
                val interactiveElements = parseInteractiveElements(data["interactiveElements"])

                TheoryTask(
                    taskname = data["name"] as? String ?: "Задание ${id}",
                    id = id,
                    image = data["image"] as? String ?: "",
                    title = data["title"] as? String ?: "",
                    text = data["text"] as? String ?: "",
                    markdownContent = markdownContent,
                    parsedMarkdown = parsedMarkdown,
                    interactiveElements = interactiveElements
                )
            }

            else -> null
        }
    }
    fun checkIfLessonExists(level: String, lesson: Int, callback: (Boolean) -> Unit) {
        val reference = database.getReference("Lessons/$level/$lesson")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callback(snapshot.exists())
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false)
            }
        })
    }

    fun getFirstSectionOfLesson(level: String, lesson: Int): Flow<Section?> = callbackFlow {
        val reference = database.getReference("Lessons/$level/$lesson/sections")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.children.firstOrNull() != null) {
                    val firstSectionSnapshot = snapshot.children.first()
                    val sectionData = firstSectionSnapshot.value as? Map<String, Any>
                    sectionData?.let { data ->
                        val section = createSectionFromData(data, firstSectionSnapshot.key ?: "")
                        trySend(section)
                        return
                    }
                }
                trySend(null)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }

        reference.addValueEventListener(listener)
        awaitClose {
            reference.removeEventListener(listener)
        }
    }

    private fun parseInteractiveElements(data: Any?): List<InteractiveElement> {
        val elements = mutableListOf<InteractiveElement>()

        when (data) {
            is List<*> -> {
                data.forEach { element ->
                    when (element) {
                        is Map<*, *> -> {
                            try {
                                val elementMap = element as Map<String, Any>
                                val interactiveElement = InteractiveElement(
                                    type = elementMap["type"] as? String ?: "question",
                                    content = elementMap["content"] as? String ?: "",
                                    options = (elementMap["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                                    correctAnswer = elementMap["correctAnswer"] as? String ?: "",
                                    hint = elementMap["hint"] as? String
                                )
                                elements.add(interactiveElement)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        else -> {
                            // Попробуем парсить старый формат Question если нужно
                            try {
                                val elementMap = element as? Map<String, Any>
                                elementMap?.let {
                                    val interactiveElement = InteractiveElement(
                                        type = "question",
                                        content = it["question"] as? String ?: "",
                                        options = (it["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                                        correctAnswer = it["answer"] as? String ?: "",
                                        hint = it["hint"] as? String
                                    )
                                    elements.add(interactiveElement)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
            is Map<*, *> -> {
                // Если это одиночный элемент
                try {
                    val elementMap = data as Map<String, Any>
                    elements.add(InteractiveElement(
                        type = elementMap["type"] as? String ?: "question",
                        content = elementMap["content"] as? String ?: "",
                        options = (elementMap["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        correctAnswer = elementMap["correctAnswer"] as? String ?: "",
                        hint = elementMap["hint"] as? String
                    ))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return elements
    }

    fun getUserProfile(userId: String, callback: (UserProfile?) -> Unit) {
        val reference = database.getReference("Users/$userId")

        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        val userData = snapshot.value as? Map<String, Any>
                        val profile = createUserProfileFromData(userId, userData ?: emptyMap())
                        callback(profile)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        callback(null)
                    }
                } else {
                    callback(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Firebase error loading user profile: ${error.message}")
                callback(null)
            }
        })
    }

    // Получение прогресса пользователя
    fun getUserProgress(userId: String, level: String, callback: (Int) -> Unit) {
        // Здесь нужно определить структуру хранения прогресса
        // Предположим, что прогресс хранится в "UserProgress/$userId/$level"
        val reference = database.getReference("UserProgress/$userId/$level")

        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val progress = snapshot.getValue(Int::class.java) ?: 0
                callback(progress)
            }

            override fun onCancelled(error: DatabaseError) {
                println("Firebase error loading progress: ${error.message}")
                callback(0)
            }
        })
    }

    // Получение текущего аутентифицированного пользователя
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    // Получение UID текущего пользователя
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // Создание UserProfile из данных Firebase
    private fun createUserProfileFromData(userId: String, data: Map<String, Any>): UserProfile {
        return UserProfile(
            userId = userId,
            fullName = data["fullName"] as? String ?:
            ((data["firstName"] as? String ?: "") + " " + (data["lastName"] as? String ?: "")).trim(),
            group = data["group"] as? String ?: "Не указана",
            email = data["email"] as? String ?: "",
            // Прогресс будем загружать отдельно
            elementaryProgress = 0,
            basicProgress = 0,
            intermediateProgress = 0,
            additionalProgress = 0
        )
    }

    fun loadCurrentUserProfile(callback: (UserProfile?) -> Unit) {
        val currentUser = getCurrentUser()
        if (currentUser == null) {
            callback(null)
            return
        }

        getUserProfile(currentUser.uid) { profile ->
            callback(profile)
        }
    }
    /**
     * Обновляет профиль пользователя в базе данных
     */
    fun updateUserProfile(userId: String, fullName: String, group: String, callback: (Boolean, String?) -> Unit) {
        val updates = HashMap<String, Any>()
        updates["fullName"] = fullName
        updates["group"] = group

        database.getReference("Users/$userId")
            .updateChildren(updates)
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener { e ->
                callback(false, e.message)
            }
    }

    /**
     * Создает профиль пользователя если его нет
     */
    fun createUserProfileIfNeeded(userId: String, email: String, fullName: String? = null, group: String? = null) {
        val userRef = database.getReference("Users/$userId")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // Создаем профиль если его нет
                    val profileData = HashMap<String, Any>()
                    profileData["email"] = email
                    profileData["fullName"] = fullName ?: email.split("@").first()
                    profileData["group"] = group ?: "Не указана"

                    userRef.setValue(profileData)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Error checking user profile: ${error.message}")
            }
        })
    }

    /**
     * Проверяет, является ли пользователь зарегистрированным через Google
     */
    fun isGoogleUser(): Boolean {
        val currentUser = auth.currentUser
        return currentUser?.let { user ->
            user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
        } ?: false
    }

    /**
     * Обновляет email пользователя
     */
    suspend fun updateUserEmail(currentEmail: String, newEmail: String, password: String): Boolean {
        return try {
            // Переаутентификация пользователя
            val credential = EmailAuthProvider.getCredential(currentEmail, password)
            auth.currentUser?.reauthenticate(credential)?.await()

            // Обновление email
            auth.currentUser?.updateEmail(newEmail)?.await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Обновляет пароль пользователя
     */
    suspend fun updateUserPassword(currentEmail: String, currentPassword: String, newPassword: String): Boolean {
        return try {
            // Переаутентификация пользователя
            val credential = EmailAuthProvider.getCredential(currentEmail, currentPassword)
            auth.currentUser?.reauthenticate(credential)?.await()

            // Обновление пароля
            auth.currentUser?.updatePassword(newPassword)?.await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

data class UserProfile(
    val userId: String,
    val fullName: String,
    val group: String,
    val email: String,
    val elementaryProgress: Int,
    val basicProgress: Int,
    val intermediateProgress: Int,
    val additionalProgress: Int
)

private fun createImageAudioTask(data: Map<String, Any>, id: String): ImageAudioTask? {
    return try {
        ImageAudioTask(
            taskname = data["name"] as? String ?: "Задание ${id}",
            id = id,
            image = data["image"] as? String ?: "",
            audio = data["audio"] as? String ?: "",
            questions = parseQuestions(data["questions"] as? List<Map<String, Any>>)
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun parseQuestions(questionsData: List<Map<String, Any>>?): List<Question> {
    if (questionsData == null) return emptyList()

    return questionsData.mapNotNull { questionData ->
        try {
            Question(
                taskname = questionData["name"] as? String ?: "",
                question = questionData["question"] as? String ?: "",
                options = (questionData["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                answer = questionData["answer"] as? String ?: "",
                hint = questionData["hint"] as? String
            )
        } catch (e: Exception) {
            null
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
