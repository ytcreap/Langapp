package com.example.langapp.ui.tasks

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.langapp.data.model.AudioRecordingTask
import com.example.langapp.data.model.ImageRecordingTask
import com.example.langapp.data.model.Task
import com.example.langapp.data.model.TextRecordingTask
import com.example.langapp.data.repository.SpeechCheckResult
import com.example.langapp.data.repository.SpeechRepository
import com.example.langapp.databinding.FragmentTaskPageBinding
import com.example.langapp.ui.adapters.UniversalTaskAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class TaskPageFragment : Fragment() {
    private var _binding: FragmentTaskPageBinding? = null
    private val binding get() = _binding!!
    private val speechRepository = SpeechRepository.getInstance()
    private var speechScope: CoroutineScope? = null

    companion object {
        private const val RECORD_AUDIO_REQUEST_CODE = 1201

        fun newInstance(
            task: Task,
            level: String,
            lesson: Int,
            type: String,
            taskNumber: Int,
            taskName: String
        ): TaskPageFragment {
            return TaskPageFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("TASK", task)
                    putString("LEVEL", level)
                    putInt("LESSON", lesson)
                    putString("TYPE", type)
                    putInt("TASK_NUMBER", taskNumber)
                    putString("TASK_NAME", taskName)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        speechScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        requestAudioPermissionIfNeeded()

        val task = arguments?.getParcelable<Task>("TASK") ?: return
        val adapter = UniversalTaskAdapter(
            items = listOf(task),
            onItemClick = { clickedTask ->
                if (clickedTask is AudioRecordingTask) {
                    handleAudioRecording(clickedTask, null)
                }
            },
            onRecordingReady = { recordingTask, audioFile ->
                handleRecordingAnswer(recordingTask, audioFile)
            }
        )

        binding.rvTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTasks.adapter = adapter
    }

    override fun onDestroyView() {
        speechScope?.cancel()
        speechScope = null
        super.onDestroyView()
        _binding = null
    }

    private fun requestAudioPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
        }
    }

    private fun handleRecordingAnswer(task: Task, audioFile: File) {
        when (task) {
            is AudioRecordingTask -> handleAudioRecording(task, audioFile)
            is ImageRecordingTask -> handleImageRecording(task, audioFile)
            is TextRecordingTask -> handleTextRecording(task, audioFile)
            else -> Unit
        }
    }

    private fun handleAudioRecording(task: AudioRecordingTask, audioFile: File?) {
        if (audioFile == null) {
            return
        }
        submitRecording(
            audioFile = audioFile,
            referenceText = task.targetText.ifBlank { task.textHint.orEmpty() },
            taskId = task.id,
            taskType = task.type,
            threshold = 0.7
        )
    }

    private fun handleImageRecording(task: ImageRecordingTask, audioFile: File) {
        submitRecording(
            audioFile = audioFile,
            referenceText = task.targetText,
            taskId = task.id,
            taskType = task.type,
            threshold = task.similarityThreshold.toDouble()
        )
    }

    private fun handleTextRecording(task: TextRecordingTask, audioFile: File) {
        submitRecording(
            audioFile = audioFile,
            referenceText = task.text,
            taskId = task.id,
            taskType = task.type,
            threshold = 0.7
        )
    }

    private fun submitRecording(
        audioFile: File,
        referenceText: String,
        taskId: String,
        taskType: String,
        threshold: Double
    ) {
        if (referenceText.isBlank()) {
            Toast.makeText(requireContext(), "У задания не задан эталонный текст", Toast.LENGTH_LONG).show()
            audioFile.delete()
            return
        }

        Toast.makeText(requireContext(), "Проверяем произношение...", Toast.LENGTH_SHORT).show()
        speechScope?.launch {
            try {
                val result = speechRepository.checkRecording(
                    audioFile = audioFile,
                    referenceText = referenceText,
                    taskId = taskId,
                    taskType = taskType,
                    threshold = threshold
                )
                showSpeechResult(result)
            } catch (e: Exception) {
                e.printStackTrace()
                showSpeechError(e.message ?: "Не удалось проверить запись")
            } finally {
                audioFile.delete()
            }
        }
    }

    private fun showSpeechResult(result: SpeechCheckResult) {
        val title = if (result.passed) "Ответ принят" else "Нужно повторить"
        val errors = if (result.errors.isEmpty()) {
            ""
        } else {
            "\n\nОшибки:\n" + result.errors.take(5).joinToString("\n") { error ->
                when (error.type) {
                    "replace" -> "Ожидалось: ${error.expected}, распознано: ${error.actual}"
                    "delete" -> "Пропущено: ${error.expected}"
                    "insert" -> "Лишнее: ${error.actual}"
                    else -> error.actual.ifBlank { error.type }
                }
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(
                "Точность: ${result.scorePercent}%\n" +
                    "Эталон: ${result.referenceText}\n" +
                    "Распознано: ${result.recognizedText.ifBlank { "пусто" }}" +
                    errors
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSpeechError(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Ошибка проверки")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
