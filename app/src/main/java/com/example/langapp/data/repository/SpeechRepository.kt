package com.example.langapp.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class SpeechCheckResult(
    val success: Boolean,
    val passed: Boolean,
    val score: Double,
    val scorePercent: Double,
    val referenceText: String,
    val recognizedText: String,
    val errors: List<SpeechError>,
    val rawMessage: String? = null
)

data class SpeechError(
    val type: String,
    val expected: String,
    val actual: String
)

class SpeechRepository {
    private var speechBaseUrl = "http://10.0.2.2:8001/api/speech"

    companion object {
        private val _instance: SpeechRepository by lazy { SpeechRepository() }
        fun getInstance(): SpeechRepository = _instance
    }

    fun configureBaseUrl(baseUrl: String) {
        speechBaseUrl = baseUrl.trimEnd('/')
    }

    suspend fun checkRecording(
        audioFile: File,
        referenceText: String,
        taskId: String,
        taskType: String,
        threshold: Double = 0.7
    ): SpeechCheckResult = withContext(Dispatchers.IO) {
        val response = multipartRequest(
            path = "check-text",
            audioFile = audioFile,
            fields = mapOf(
                "referenceText" to referenceText,
                "taskId" to taskId,
                "taskType" to taskType,
                "threshold" to threshold.toString()
            )
        )

        parseSpeechResult(response)
    }

    private fun multipartRequest(
        path: String,
        audioFile: File,
        fields: Map<String, String>
    ): String {
        val boundary = "LangApp-${UUID.randomUUID()}"
        val connection = (URL("${speechBaseUrl.trimEnd('/')}/${path.trimStart('/')}").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        try {
            DataOutputStream(connection.outputStream).use { output ->
                fields.forEach { (name, value) ->
                    output.writeBytes("--$boundary\r\n")
                    output.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                    output.write(value.toByteArray(Charsets.UTF_8))
                    output.writeBytes("\r\n")
                }

                output.writeBytes("--$boundary\r\n")
                output.writeBytes("Content-Disposition: form-data; name=\"audio\"; filename=\"${audioFile.name}\"\r\n")
                output.writeBytes("Content-Type: audio/mp4\r\n\r\n")
                audioFile.inputStream().use { input ->
                    input.copyTo(output)
                }
                output.writeBytes("\r\n--$boundary--\r\n")
                output.flush()
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw IllegalStateException("Speech API error HTTP $status: $body")
            }
            return body
        } finally {
            connection.disconnect()
        }
    }

    private fun parseSpeechResult(body: String): SpeechCheckResult {
        val json = JSONObject(body)
        val errors = parseErrors(json.optJSONArray("errors"))
        return SpeechCheckResult(
            success = json.optBoolean("success", false),
            passed = json.optBoolean("passed", false),
            score = json.optDouble("score", 0.0),
            scorePercent = json.optDouble("scorePercent", json.optDouble("score", 0.0) * 100.0),
            referenceText = json.optString("referenceText", ""),
            recognizedText = json.optString("recognizedText", ""),
            errors = errors,
            rawMessage = json.optString("detail").takeIf { it.isNotBlank() }
        )
    }

    private fun parseErrors(array: JSONArray?): List<SpeechError> {
        if (array == null) return emptyList()
        val result = mutableListOf<SpeechError>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            result.add(
                SpeechError(
                    type = item.optString("type"),
                    expected = item.optString("expected"),
                    actual = item.optString("actual")
                )
            )
        }
        return result
    }
}
