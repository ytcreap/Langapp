package com.example.langapp.vocabulary

import android.media.MediaPlayer
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.AdapterFactory
import com.example.langapp.R
import com.example.langapp.data.model.FirebaseSimulator
import com.example.langapp.data.model.PhoneticTask
import com.example.langapp.data.model.VocabularyTask
import com.example.langapp.databinding.FragmentVocabularyBinding
import com.example.langapp.ui.adapters.PhoneticsAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class VocabularyFragment : Fragment() {
    private var _binding: FragmentVocabularyBinding? = null
    private val binding get() = _binding!!
    private lateinit var mediaPlayer: MediaPlayer
    private var currentAudioPosition = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVocabularyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = requireArguments()
        val level = args.getString("LEVEL_KEY") ?: ""
        val lesson = args.getInt("TASK_NUMBER_KEY")
        val rawType = args.getString("TASK_TYPE_KEY") ?: ""
        val type = rawType.normalized()
        if (type.isEmpty()) {
            showErrorDialog()
            findNavController().navigateUp()
            return
        }
        val tasks = FirebaseSimulator.getTasks(
            level = level.lowercase(),
            lesson = lesson,
            type = type // Используем уже нормализованное значение
        )
        val adapter = AdapterFactory.createAdapter(
            taskType = type,
            items = tasks,
            context = requireContext()
        )
        binding.rvVocabulary.adapter = adapter
        when (type.normalized()) {
            "phonetics", "фонетика" -> setupPhonetics(tasks.filterIsInstance<PhoneticTask>())
            "vocabulary", "лексика" -> setupVocabulary(tasks.filterIsInstance<VocabularyTask>())
            else -> showErrorDialog()
        }
    }
    private fun showErrorDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ошибка загрузки")
            .setMessage("Неподдерживаемый тип задания: ${arguments?.getString("TASK_TYPE_KEY")}")
            .setPositiveButton("OK") { _, _ -> findNavController().navigateUp() }
            .show()
    }
    private fun String.normalized() = this
        .trim()
        .replace("[^A-Za-zА-Яа-я]".toRegex(), "")
        .lowercase()


    private fun playAudio(audioRes: Int) {
        try {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.reset()
            mediaPlayer = MediaPlayer.create(requireContext(), audioRes)
            mediaPlayer.start()
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка воспроизведения", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        arguments?.let {
            outState.putAll(it)
        }
    }
    private fun validateArguments() {
        if (arguments == null || !requireArguments().containsKey("TASK_NUMBER_KEY")) {
            Toast.makeText(context, "Ошибка загрузки задания", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun stopAudioPlayback() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.reset()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::mediaPlayer.isInitialized) { // Проверка инициализации
            mediaPlayer.release()
        }
    }

    private fun setupPhonetics(tasks: List<PhoneticTask>) {
        binding.rvVocabulary.adapter = PhoneticsAdapter(tasks).apply {
            setOnAudioClickListener { audioResId ->
                playAudio(getResourceId(audioResId))
            }
        }
    }

    private fun getResourceId(resName: String): Int {
        return resources.getIdentifier(resName, "raw", requireContext().packageName)
    }
    private fun setupVocabulary(tasks: List<VocabularyTask>) {
        binding.rvVocabulary.adapter = com.example.langapp.ui.adapters.VocabularyAdapter(
            items = tasks,
            context = requireContext()
        ).apply {
            setOnAudioClickListener { audioResId ->
                playAudio(getResourceId(audioResId))
            }
        }
    }
}

// Data class для элементов словаря
data class VocabularyItem(
    val word: String,
    @DrawableRes val imageRes: Int,
    @RawRes val audioRes: Int
)

