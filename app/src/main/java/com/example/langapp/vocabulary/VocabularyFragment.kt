package com.example.langapp.vocabulary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.langapp.data.model.Task
import com.example.langapp.databinding.FragmentVocabularyBinding
import com.example.langapp.ui.adapters.TaskPagerAdapter
import com.example.langapp.ui.gallery.GalleryViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class VocabularyFragment : Fragment() {
    private var _binding: FragmentVocabularyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GalleryViewModel by viewModels()

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

        if (type.isEmpty()) {
            showErrorDialog()
            return
        }

        // Загружаем задачи из Firebase
        viewModel.loadTasks(level, lesson, type)
        setupObservers(level, lesson, type)
        setupHeader(level, lesson, type)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupObservers(level: String, lesson: Int, type: String) {
        lifecycleScope.launch {
            viewModel.tasks.collectLatest { tasks ->
                if (tasks.isNotEmpty()) {
                    setupViewPager(tasks, level, lesson, type)
                } else {
                    showEmptyState()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                if (isLoading) {
                    binding.viewPager.visibility = View.GONE
                    binding.tabLayout.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.GONE
                }
            }
        }
    }

    private fun setupViewPager(tasks: List<Task>, level: String, lesson: Int, type: String) {
        val pagerAdapter = TaskPagerAdapter(this, tasks, level, lesson, type)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tasks[position].taskname
        }.attach()

        binding.viewPager.visibility = View.VISIBLE
        binding.tabLayout.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE
    }

    private fun String.normalized(): String {
        return when (this.trim().lowercase(Locale.getDefault())) {
            "vocabulary", "лексика", "lexicon" -> "vocabulary"
            "phonetics", "фонетика" -> "phonetics"
            "grammar", "грамматика" -> "grammar"
            "texts", "тексты" -> "texts"
            "test", "тест", "quiz" -> "test"
            else -> {
                ""
            }
        }
    }

    private fun setupHeader(level: String, lesson: Int, type: String) {
        binding.tvHeader.text = when (type) {
            "phonetics" -> "Фонетическое задание"
            "vocabulary" -> "Лексическое задание"
            "grammar" -> "Грамматическое задание"
            "texts" -> "Работа с текстом"
            "test" -> "Тестирование"
            else -> "Задание"
        }

        binding.tvLevel.text = "Уровень: $level"
        binding.tvLesson.text = "Урок: $lesson"
        binding.tvType.text = "Тип: ${type.replaceFirstChar { it.uppercase() }}"
    }

    private fun showEmptyState() {
        binding.apply {
            viewPager.visibility = View.GONE
            tabLayout.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
            tvEmptyState.text = "Задания для этого урока пока недоступны"
        }
    }

    private fun showErrorDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ошибка загрузки")
            .setMessage("Неподдерживаемый тип задания: ${arguments?.getString("TASK_TYPE_KEY")}")
            .setPositiveButton("OK") { _, _ -> findNavController().navigateUp() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}