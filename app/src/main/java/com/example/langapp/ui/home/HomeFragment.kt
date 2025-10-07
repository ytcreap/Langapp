package com.example.langapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.langapp.MainActivity
import com.example.langapp.databinding.FragmentHomeBinding
import com.example.langapp.R
import com.google.android.material.progressindicator.LinearProgressIndicator


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupObservers()
        setupLogoutButton()
        setupLevelSpinner()

        return binding.root
    }

    private fun setupObservers() {
        viewModel.userData.observe(viewLifecycleOwner) { profile ->
            binding.tvFullName.text = getString(R.string.full_name_template).replaceAfter(":", " ${profile.fullName}")
            binding.tvGroup.text = getString(R.string.group_template).replaceAfter(":", " ${profile.group}")

            // Обновляем прогресс-бар при загрузке данных
            updateProgressBar(profile, 0) // Начинаем с Elementary
        }
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            // Реализация выхода
            (requireActivity() as MainActivity).signOut()
        }
    }

    private fun setupLevelSpinner() {
        val levelSpinner: Spinner = binding.levelSpinner
        val progressBar: LinearProgressIndicator = binding.progressBar
        val progressText = binding.progressText

        // Настройка адаптера для Spinner
        val levels = arrayOf("Элементарный", "Базовый", "Средний")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, levels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        levelSpinner.adapter = adapter

        // Обработчик выбора уровня
        levelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.userData.value?.let { profile ->
                    updateProgressBar(profile, position)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Скрыть прогресс, если ничего не выбрано
                progressBar.progress = 0
                progressText.text = "0/0"
            }
        }

        // Установить начальное значение
        levelSpinner.setSelection(0)
    }

    private fun updateProgressBar(profile: HomeViewModel.UserProfile, levelPosition: Int) {
        val progressBar: LinearProgressIndicator = binding.progressBar
        val progressText = binding.progressText

        when (levelPosition) {
            0 -> { // Elementary
                //val progress = profile.elementaryProgress
                val progress = 3 //дебаг строка
                val max = 15 // Максимальное количество заданий для Elementary
                progressBar.max = max
                progressBar.progress = progress
                progressText.text = "$progress/$max"
            }
            1 -> { // Basic
                val progress = profile.basicProgress
                val max = 15 // Максимальное количество заданий для Basic
                progressBar.max = max
                progressBar.progress = progress
                progressText.text = "$progress/$max"
            }
            2 -> { // Intermediate
                val progress = profile.intermediateProgress
                val max = 15 // Максимальное количество заданий для Intermediate
                progressBar.max = max
                progressBar.progress = progress
                progressText.text = "$progress/$max"
            }
        }
    }
}