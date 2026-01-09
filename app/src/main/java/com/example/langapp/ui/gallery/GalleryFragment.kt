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
import com.example.langapp.databinding.FragmentGalleryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GalleryViewModel by viewModels()

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

        // Загружаем доступные разделы для этого урока
        viewModel.loadAvailableSections(level, taskNumber)

        // Создаем кастомный layout для диалога
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_task_selector, null)
        val titleText = dialogView.findViewById<TextView>(R.id.dialog_title)
        val container = dialogView.findViewById<LinearLayout>(R.id.tasks_container)

        titleText.text = "$levelDisplayName • Урок $taskNumber"

        // Создаем диалог
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        // Наблюдаем за доступными разделами
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.availableSections.collect { sections ->
                Log.d("GalleryFragment", "Loaded sections: ${sections.map { it.name }}")

                // Очищаем контейнер
                container.removeAllViews()

                // Добавляем кнопки для каждого раздела
                sections.forEach { section ->
                    val button = LayoutInflater.from(requireContext()).inflate(R.layout.item_task, container, false)
                    val taskText = button.findViewById<TextView>(R.id.task_text)
                    taskText.text = section.name

                    // Можно добавить описание, если нужно
                    // val taskDesc = button.findViewById<TextView>(R.id.task_description)
                    // taskDesc.text = section.description

                    button.setOnClickListener {
                        dialog.dismiss()
                        navigateToTask(level, taskNumber, section.id) // Используем section.id для навигации
                    }

                    container.addView(button)
                }

                // Если разделов нет, показываем сообщение
                if (sections.isEmpty()) {
                    val emptyView = TextView(requireContext()).apply {
                        text = "Разделы пока не добавлены"
                        setPadding(50, 50, 50, 50)
                        textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    }
                    container.addView(emptyView)
                }
            }
        }

        dialog.show()
    }

    private fun navigateToTask(level: String, taskNumber: Int, sectionId: String) {
        // Нужно также получить название раздела для отображения
        val section = viewModel.availableSections.value.find { it.id == sectionId }
        val sectionName = section?.name ?: sectionId

        val args = Bundle().apply {
            putString("LEVEL_KEY", level)
            putInt("TASK_NUMBER_KEY", taskNumber)
            putString("SECTION_NAME_KEY", sectionName)
            putString("SECTION_ID_KEY", sectionId)
        }
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