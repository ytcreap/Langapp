package com.example.langapp.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
            levelName = "Элементарный"
        )

        setupLevel(
            button = binding.btnBasic,
            grid = binding.gridBasic,
            levelName = "Базовый"
        )

        setupLevel(
            button = binding.btnIntermediate,
            grid = binding.gridIntermediate,
            levelName = "Средний"
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
        val items = arrayOf(
            "1. Лексика",
            "2. Фонетика",
            "3. Грамматика",
            "4. Тексты",
            "5. Тест"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("$level: Задание $taskNumber")
            .setItems(items) { _, which ->
                val type = when (which) {
                    0 -> "Лексика"
                    1 -> "Фонетика"
                    2 -> "Грамматика"
                    3 -> "Тексты"
                    else -> "Тест"
                }
                navigateToVocabulary(level, taskNumber, type)
            }
            .setPositiveButton("Закрыть", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    fun navigateToVocabulary(level: String, taskNumber: Int, taskType: String) {
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
}
