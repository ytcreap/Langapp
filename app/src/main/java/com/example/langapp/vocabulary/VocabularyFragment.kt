package com.example.langapp.vocabulary

import android.os.Bundle
import android.util.Log
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
        val sectionName = args.getString("SECTION_NAME_KEY") ?: ""
        val sectionId = args.getString("SECTION_ID_KEY") ?: ""

        if (sectionId.isEmpty()) {
            showErrorDialog()
            return
        }

        // Загружаем задачи из раздела по ID
        viewModel.loadTasks(level, lesson, sectionId)
        setupObservers(level, lesson, sectionName)
        setupHeader(level, lesson, sectionName)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupObservers(level: String, lesson: Int, sectionName: String) {
        lifecycleScope.launch {
            viewModel.tasks.collectLatest { tasks ->
                if (tasks.isNotEmpty()) {
                    setupViewPager(tasks, level, lesson, sectionName)
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
                    binding.cardEmptyState.visibility = View.GONE
                }
            }
        }
    }

    private fun setupViewPager(tasks: List<Task>, level: String, lesson: Int, sectionName: String) {
        val pagerAdapter = TaskPagerAdapter(this, tasks, level, lesson, sectionName)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = "Задание ${position + 1}"
        }.attach()

        binding.viewPager.visibility = View.VISIBLE
        binding.tabLayout.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE
        binding.cardEmptyState.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
    }

    private fun setupHeader(level: String, lesson: Int, sectionName: String) {
        // Устанавливаем заголовок
        binding.tvHeader.text = sectionName

        // Устанавливаем информацию о задании
        binding.tvLevel.text = level.uppercase()
        binding.tvLesson.text = lesson.toString()
        binding.tvType.text = sectionName
    }

    private fun showEmptyState() {
        binding.apply {
            viewPager.visibility = View.GONE
            tabLayout.visibility = View.GONE
            cardEmptyState.visibility = View.VISIBLE
            tvEmptyState.text = "Задания для этого раздела пока недоступны"
            progressBar.visibility = View.GONE
        }
    }

    private fun showErrorDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ошибка загрузки")
            .setMessage("Раздел не найден")
            .setPositiveButton("OK") { _, _ -> findNavController().navigateUp() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}