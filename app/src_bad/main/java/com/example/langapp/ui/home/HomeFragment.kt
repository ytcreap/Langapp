package com.example.langapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.langapp.databinding.FragmentHomeBinding
import com.example.langapp.R

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

        return binding.root
    }

    private fun setupObservers() {
        viewModel.userData.observe(viewLifecycleOwner) { profile ->
            binding.tvFullName.text = getString(R.string.full_name_template).replaceAfter(":", " ${profile.fullName}")
            binding.tvGroup.text = getString(R.string.group_template).replaceAfter(":", " ${profile.group}")

            binding.tvElementary.text = getString(
                R.string.elementary_progress,
                profile.elementaryProgress
            )

            binding.tvBasic.text = getString(
                R.string.basic_progress,
                profile.basicProgress
            )

            binding.tvIntermediate.text = getString(
                R.string.intermediate_progress,
                profile.intermediateProgress
            )
        }
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            // Реализация выхода
            requireActivity().finishAffinity()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}