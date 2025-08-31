package com.example.langapp.vocabulary

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.navigation.fragment.findNavController
import com.example.langapp.data.model.FirebaseSimulator
import com.example.langapp.data.model.Task
import com.example.langapp.databinding.FragmentVocabularyBinding
import com.example.langapp.ui.adapters.TaskPagerAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import java.util.Locale

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
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments()
        val level = args.getString("LEVEL_KEY") ?: ""
        val lesson = args.getInt("TASK_NUMBER_KEY")
        val rawType = args.getString("TASK_TYPE_KEY") ?: ""
        val type = rawType.normalized()
        val name = args.getString("TASK_NAME_KEY") ?: ""

        if (type.isEmpty()) {
            showErrorDialog()
            return
        }

        // Получаем задания для этого типа
        val tasks = FirebaseSimulator.getTasks(
            level = level.lowercase(),
            lesson = lesson,
            taskType = type
        )

        when {
            tasks.isNotEmpty() -> {
                setupViewPager(tasks, level, lesson, type, name) // Теперь передаем List<Task>
                setupHeader(level, lesson, type)
            }

            else -> {
                binding.apply {
                    viewPager.visibility = View.GONE
                    tabLayout.visibility = View.GONE
                    tvEmptyState.visibility = View.VISIBLE
                    tvEmptyState.text = "Задания для этого урока пока недоступны"
                }
            }
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupViewPager(tasks: List<Task>, level: String, lesson: Int, type: String, name: String) {
        if (tasks.isEmpty()) {
            binding.apply {
                viewPager.visibility = View.GONE
                tabLayout.visibility = View.GONE
                tvEmptyState.visibility = View.VISIBLE
                tvEmptyState.text = "Задания для этого урока пока недоступны"
            }
            return
        }

        val pagerAdapter = TaskPagerAdapter(this, tasks, level, lesson, type)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tasks[position].taskname  // Берем название из задания
        }.attach()
    }


    private fun String.normalized(): String {
        return when (this.trim().lowercase(Locale.getDefault())) {
            "vocabulary", "лексика", "lexicon" -> "vocabulary"
            "phonetics", "фонетика" -> "phonetics"
            "grammar", "грамматика" -> "grammar"
            "texts", "тексты" -> "texts"
            "test", "тест", "quiz" -> "test"
            else -> {
                Log.w("Normalize", "Unsupported type: '${this}'")
                ""
            }
        }
    }

    private fun setupHeader(level: String, lesson: Int, type: String) {
        // Устанавливаем заголовок
        binding.tvHeader.text = when (type) {
            "фонетика" -> "Фонетическое задание"
            "лексика" -> "Лексическое задание"
            "грамматика" -> "Грамматическое задание"
            "тексты" -> "Работа с текстом"
            "тест" -> "Тестирование"
            else -> "Задание"
        }

        // Устанавливаем информацию о задании
        binding.tvLevel.text = "Уровень: $level"
        binding.tvLesson.text = "Урок: $lesson"
        binding.tvType.text = "Тип: ${type.capitalize()}"
    }

    private fun showErrorDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ошибка загрузки")
            .setMessage("Неподдерживаемый тип задания: ${arguments?.getString("TASK_TYPE_KEY")}")
            .setPositiveButton("OK") { _, _ -> findNavController().navigateUp() }
            .show()

    }


//    private fun playAudio(audioRes: Int) {
//        try {
//            if (mediaPlayer.isPlaying) {
//                mediaPlayer.stop()
//            }
//            mediaPlayer.reset()
//            mediaPlayer = MediaPlayer.create(requireContext(), audioRes)
//            mediaPlayer.start()
//        } catch (e: Exception) {
//            Toast.makeText(context, "Ошибка воспроизведения", Toast.LENGTH_SHORT).show()
//        }
//    }

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

//    private fun stopAudioPlayback() {
//        if (mediaPlayer.isPlaying) {
//            mediaPlayer.stop()
//        }
//        mediaPlayer.reset()
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::mediaPlayer.isInitialized) { // Проверка инициализации
            mediaPlayer.release()
        }
    }


    private fun getResourceId(resName: String): Int {
        return resources.getIdentifier(resName, "raw", requireContext().packageName)
    }
}

