package com.example.langapp.ui.gallery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.langapp.R
import com.example.langapp.data.model.Section
import com.example.langapp.data.repository.FirebaseRepository
import com.example.langapp.databinding.FragmentGalleryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GalleryViewModel by viewModels()

    private val repository = FirebaseRepository.getInstance()
    private var currentSections: List<Section> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация для всех уровней
        setupLevel(
            button = binding.btnElementary,
            grid = binding.gridElementary,
            levelName = "elementary"
        )

        setupLevel(
            button = binding.btnBasic,
            grid = binding.gridBasic,
            levelName = "basic"
        )

        setupLevel(
            button = binding.btnIntermediate,
            grid = binding.gridIntermediate,
            levelName = "intermediate"
        )

        setupLevel(
            button = binding.btnAdditional,
            grid = binding.gridAdditional,
            levelName = "additional"
        )
    }

    private fun setupLevel(button: Button, grid: GridLayout, levelName: String) {
        // Генерация кнопок заданий
        for (i in 1..15) {
            val taskButton = Button(context).apply {
                text = i.toString()
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
                setOnClickListener {
                    showTaskDetails(levelName, i)
                }
            }
            grid.addView(taskButton)
        }

        // Обработчик клика по заголовку уровня
        button.setOnClickListener {
            val isExpanded = grid.visibility == View.VISIBLE
            grid.visibility = if (isExpanded) View.GONE else View.VISIBLE
            button.setCompoundDrawablesWithIntrinsicBounds(
                0, 0,
                if (isExpanded) R.drawable.ic_expand_more else R.drawable.ic_expand_less,
                0
            )
        }
    }

    private fun showTaskDetails(level: String, taskNumber: Int) {
        val levelDisplayName = when (level) {
            "elementary" -> "Элементарный"
            "basic" -> "Средний"
            "intermediate" -> "Продвинутый"
            "additional" -> "Дополнительно"
            else -> level
        }

        // Создаем кастомный layout для диалога
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_task_selector, null)
        val titleText = dialogView.findViewById<TextView>(R.id.dialog_title)
        val loadingContainer = dialogView.findViewById<LinearLayout>(R.id.loading_container)
        val contentContainer = dialogView.findViewById<LinearLayout>(R.id.content_container)
        val tasksContainer = dialogView.findViewById<LinearLayout>(R.id.tasks_container)
        val emptyText = dialogView.findViewById<TextView>(R.id.empty_text)

        titleText.text = "$levelDisplayName • Урок $taskNumber"

        // Всегда показываем спиннер при открытии
        loadingContainer.visibility = View.VISIBLE
        contentContainer.visibility = View.GONE

        // Создаем диалог
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Показываем диалог сразу
        dialog.show()

        // Загружаем данные напрямую с использованием addListenerForSingleValueEvent
        repository.getAvailableSectionsSingle(level, taskNumber) { sections ->
            requireActivity().runOnUiThread {
                // Сохраняем секции для использования в navigateToTask
                currentSections = sections

                // Скрываем спиннер
                loadingContainer.visibility = View.GONE
                contentContainer.visibility = View.VISIBLE

                // Очищаем контейнер
                tasksContainer.removeAllViews()

                if (sections.isNotEmpty()) {
                    emptyText.visibility = View.GONE
                    tasksContainer.visibility = View.VISIBLE

                    // Добавляем кнопки для каждого раздела
                    sections.forEach { section ->
                        val button = LayoutInflater.from(requireContext()).inflate(R.layout.item_task, tasksContainer, false)
                        val taskText = button.findViewById<TextView>(R.id.task_text)
                        taskText.text = section.name

                        button.setOnClickListener {
                            dialog.dismiss()
                            navigateToTask(level, taskNumber, section)
                        }

                        tasksContainer.addView(button)
                    }
                } else {
                    emptyText.visibility = View.VISIBLE
                    tasksContainer.visibility = View.GONE
                }
            }
        }
    }

    private fun navigateToTask(level: String, taskNumber: Int, section: Section) {
        val args = Bundle().apply {
            putString("LEVEL_KEY", level)
            putInt("TASK_NUMBER_KEY", taskNumber)
            putString("SECTION_NAME_KEY", section.name)
            putString("SECTION_ID_KEY", section.id)
        }

        // Загружаем задачи через ViewModel
        viewModel.loadTasks(level, taskNumber, section.id)

        findNavController().navigate(
            R.id.action_nav_gallery_to_vocabularyFragment,
            args
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}