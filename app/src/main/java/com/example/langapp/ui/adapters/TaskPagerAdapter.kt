package com.example.langapp.ui.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.langapp.data.model.Task
import com.example.langapp.ui.tasks.TaskPageFragment

class TaskPagerAdapter(
    fragment: Fragment,
    private val tasks: List<Task>, // Список списков заданий (по 3 на каждый тип)
    private val level: String,
    private val lesson: Int,
    private val type: String
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = tasks.size

    override fun createFragment(position: Int): Fragment {
        return TaskPageFragment.newInstance(
            tasks[position],
            level,
            lesson,
            type,
            position + 1 // Номер задания (1, 2 или 3)
        )
    }
}
