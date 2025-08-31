package com.example.langapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.langapp.data.model.MultipleChoiceTask
import com.example.langapp.databinding.FragmentMultipleChoiceTaskBinding

class MultipleChoiceTaskFragment : Fragment() {
    private var _binding: FragmentMultipleChoiceTaskBinding? = null
    private val binding get() = _binding!!
    private lateinit var task: MultipleChoiceTask
    private var selectedAnswerIndex: Int = -1

    companion object {
        fun newInstance(task: MultipleChoiceTask) = MultipleChoiceTaskFragment().apply {
            arguments = Bundle().apply {
                putParcelable("task", task)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMultipleChoiceTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        task = requireArguments().getParcelable<MultipleChoiceTask>("task")
            ?: throw IllegalStateException("Task argument is missing")

        setupUI()
    }

    private fun setupUI() {
        with(binding) {
            // Устанавливаем вопрос
            questionText.text = task.question

            // Создаем варианты ответов
            createOptions()

            // Настраиваем кнопку отправки
            submitButton.setOnClickListener {
                checkAnswer()
            }
        }
    }

    private fun createOptions() {
        val optionsContainer = binding.optionsContainer
        optionsContainer.removeAllViews()

        val radioGroup = RadioGroup(requireContext())
        radioGroup.orientation = RadioGroup.VERTICAL

        task.options.forEachIndexed { index, option ->
            val radioButton = RadioButton(requireContext()).apply {
                text = option
                id = index
                layoutParams = RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8.dpToPx(), 0, 0)
                }
            }
            radioGroup.addView(radioButton)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedAnswerIndex = checkedId
        }

        optionsContainer.addView(radioGroup)
    }

    private fun checkAnswer() {
        if (selectedAnswerIndex == -1) {
            Toast.makeText(requireContext(), "Пожалуйста, выберите ответ", Toast.LENGTH_SHORT).show()
            return
        }

        val isCorrect = selectedAnswerIndex == task.correctAnswerIndex

        with(binding) {
            resultText.isVisible = true
            if (isCorrect) {
                resultText.text = "Правильно! ✅"
                resultText.setTextColor(ContextCompat.getColor(requireContext(), R.color.correct_answer))
            } else {
                resultText.text = "Неправильно! ❌\nПравильный ответ: ${task.options[task.correctAnswerIndex]}"
                resultText.setTextColor(ContextCompat.getColor(requireContext(), R.color.wrong_answer))

                // Показываем объяснение, если есть
                task.explanation?.let {
                    resultText.append("\n\nОбъяснение: $it")
                }
            }

            // Делаем кнопку неактивной после ответа
            submitButton.isEnabled = false
        }
    }

    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}