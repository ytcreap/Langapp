package com.example.langapp.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.langapp.R
import com.example.langapp.databinding.FragmentGalleryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

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
            else -> level
        }

        val items = arrayOf(
            "Лексика",
            "Фонетика",
            "Грамматика",
            "Тексты",
            "Тест"
        )

        // Создаем кастомный layout для диалога
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_task_selector, null)
        val titleText = dialogView.findViewById<TextView>(R.id.dialog_title)
        val container = dialogView.findViewById<LinearLayout>(R.id.tasks_container)

        titleText.text = "$levelDisplayName • Урок $taskNumber"

        // Создаем диалог и сохраняем ссылку
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        // Создаем кнопки для каждого типа задания
        items.forEachIndexed { index, item ->
            val button = LayoutInflater.from(requireContext()).inflate(R.layout.item_task, container, false)
            val taskText = button.findViewById<TextView>(R.id.task_text)
            taskText.text = item

            button.setOnClickListener {
                val type = when (index) {
                    0 -> "vocabulary"
                    1 -> "phonetics"
                    2 -> "grammar"
                    3 -> "texts"
                    else -> "test"
                }
                // Закрываем диалог перед навигацией
                dialog.dismiss()
                navigateToTask(level, taskNumber, type)
            }

            container.addView(button)
        }

        dialog.show()
    }

    private fun navigateToTask(level: String, taskNumber: Int, taskType: String) {
        val args = Bundle().apply {
            putString("LEVEL_KEY", level)
            putInt("TASK_NUMBER_KEY", taskNumber)
            putString("TASK_TYPE_KEY", taskType)
        }
        findNavController().navigate(
            R.id.action_gallery_to_vocabulary,
            args
        )
    }
    /*
        private fun openTaskContainer(lessonId: String) {
            findNavController().navigate(
                R.id.action_gallery_to_taskContainer,
                bundleOf("lessonId" to lessonId)
            )
        }
     */

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }
}
