package com.example.langapp.ui.tasks

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.langapp.data.model.Task
import com.example.langapp.databinding.FragmentTaskPageBinding
import com.example.langapp.ui.adapters.UniversalTaskAdapter

class TaskPageFragment : Fragment() {
    private var _binding: FragmentTaskPageBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(
            task: Task,  // Изменили параметр на одиночный Task
            level: String,
            lesson: Int,
            type: String,
            taskNumber: Int,
            taskName: String
        ): TaskPageFragment {
            return TaskPageFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("TASK", task)  // Используем putParcelable вместо ArrayList
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

        val task = arguments?.getParcelable<Task>("TASK") ?: return  // Получаем одиночный Task
        val level = arguments?.getString("LEVEL") ?: ""
        val lesson = arguments?.getInt("LESSON") ?: 0
        val type = arguments?.getString("TYPE") ?: ""
        val taskNumber = arguments?.getInt("TASK_NUMBER") ?: 1
        val taskName = task.taskname

        binding.tvTaskInfo.text = "Уровень: $level\nУрок: $lesson\nТип: ${type.replaceFirstChar { it.uppercase() }}\nЗадание: $taskNumber\nНазвание: $taskName"
        val adapter = UniversalTaskAdapter(listOf(task)) { clickedTask ->
            // Обработка клика, если нужно
        }
        binding.rvTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTasks.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
