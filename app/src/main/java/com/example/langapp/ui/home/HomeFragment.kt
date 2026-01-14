// HomeFragment.kt
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
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment(), EditProfileDialogFragment.EditProfileListener {

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
        setupEditButton()

        return binding.root
    }

    private fun setupObservers() {
        // Наблюдаем за данными пользователя
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                binding.tvFullName.text = "Имя: ${it.fullName}"
                binding.tvGroup.text = "Группа: ${it.group}"
            } ?: run {
                binding.tvFullName.text = getString(R.string.full_name_template)
                binding.tvGroup.text = getString(R.string.group_template)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isIndeterminate = isLoading
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.VISIBLE
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.updateSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Snackbar.make(binding.root, "Профиль успешно обновлен", Snackbar.LENGTH_SHORT).show()
                viewModel.resetUpdateStatus()
                // Меню обновится автоматически через LiveData
            }
        }
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            (requireActivity() as MainActivity).signOut()
        }
    }

    private fun setupLevelSpinner() {
        val levelSpinner: Spinner = binding.levelSpinner
        val progressBar: LinearProgressIndicator = binding.progressBar
        val progressText = binding.progressText

        val levels = arrayOf("Элементарный", "Базовый", "Средний", "Дополнительно")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, levels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        levelSpinner.adapter = adapter

        levelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateProgressBar(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                progressBar.progress = 0
                progressText.text = "0/0"
            }
        }

        levelSpinner.setSelection(0)
    }

    private fun updateProgressBar(levelPosition: Int) {
        val progressBar: LinearProgressIndicator = binding.progressBar
        val progressText = binding.progressText

        when (levelPosition) {
            0 -> { progressBar.progress = 5; progressBar.max = 15; progressText.text = "5/15" }
            1 -> { progressBar.progress = 3; progressBar.max = 15; progressText.text = "3/15" }
            2 -> { progressBar.progress = 2; progressBar.max = 15; progressText.text = "2/15" }
            3 -> { progressBar.progress = 1; progressBar.max = 15; progressText.text = "1/15" }
        }
    }

    private fun setupEditButton() {
        binding.btnEdit.setOnClickListener {
            showEditProfileDialog()
        }

        binding.profileCard.setOnLongClickListener {
            showEditProfileDialog()
            true
        }
    }

    private fun showEditProfileDialog() {
        val dialog = EditProfileDialogFragment()
        dialog.setEditProfileListener(this)
        dialog.show(parentFragmentManager, "edit_profile_dialog")
    }

    override fun onProfileUpdated() {
        // Обновляем данные после редактирования профиля
        viewModel.refresh()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}