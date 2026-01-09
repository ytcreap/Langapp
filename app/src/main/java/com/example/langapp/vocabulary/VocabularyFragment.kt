package com.example.langapp.vocabulary

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.langapp.R
import com.example.langapp.data.model.Task
import com.example.langapp.databinding.FragmentVocabularyBinding
import com.example.langapp.ui.adapters.TaskPagerAdapter
import com.example.langapp.ui.gallery.GalleryViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class VocabularyFragment : Fragment() {
    private var _binding: FragmentVocabularyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GalleryViewModel by viewModels()
    private lateinit var pagerAdapter: TaskPagerAdapter
    private var fireworkAnimators: MutableList<ValueAnimator> = mutableListOf()

    private lateinit var level: String
    private var lesson: Int = 0
    private lateinit var sectionName: String
    private lateinit var sectionId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVocabularyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments()
        level = args.getString("LEVEL_KEY") ?: ""
        lesson = args.getInt("TASK_NUMBER_KEY")
        sectionName = args.getString("SECTION_NAME_KEY") ?: ""
        sectionId = args.getString("SECTION_ID_KEY") ?: ""

        if (sectionId.isEmpty()) {
            showErrorDialog()
            return
        }

        viewModel.loadTasks(level, lesson, sectionId)
        setupObservers()
        setupHeader()
        setupNextButton()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fireworkAnimators.forEach { it.cancel() }
        fireworkAnimators.clear()
        _binding = null
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.tasks.collectLatest { tasks ->
                if (tasks.isNotEmpty()) {
                    setupViewPager(tasks)
                } else {
                    showEmptyState()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                if (isLoading) {
                    binding.viewPager.visibility = View.GONE
                    binding.tabLayout.visibility = View.GONE
                    binding.btnNext.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.GONE
                    binding.cardEmptyState.visibility = View.GONE
                }
            }
        }
    }

    private fun setupViewPager(tasks: List<Task>) {
        pagerAdapter = TaskPagerAdapter(this, tasks, level, lesson, sectionName)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tasks[position].taskname
        }.attach()

        if (tasks.size <= 3) {
            binding.tabLayout.tabMode = TabLayout.MODE_FIXED
            binding.tabLayout.tabGravity = TabLayout.GRAVITY_CENTER
        } else {
            binding.tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
            binding.tabLayout.isTabIndicatorFullWidth = false
        }

        updateNextButtonState()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateNextButtonState()
            }
        })

        binding.viewPager.visibility = View.VISIBLE
        binding.tabLayout.visibility = View.VISIBLE
        binding.btnNext.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE
        binding.cardEmptyState.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
    }

    private fun setupHeader() {
        binding.tvHeader.text = sectionName
        binding.tvHeader.textSize = 18f
    }

    private fun setupNextButton() {
        binding.btnNext.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            val totalItems = pagerAdapter.itemCount

            if (currentItem < totalItems - 1) {
                binding.viewPager.setCurrentItem(currentItem + 1, true)
            } else {
                showCompletionDialog()
            }
        }

        binding.btnNext.textAlignment = View.TEXT_ALIGNMENT_CENTER
    }

    private fun showCompletionDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_section_completion, null)

        val imageView = dialogView.findViewById<android.widget.ImageView>(R.id.iv_cheburashka)
        val firework1 = dialogView.findViewById<android.widget.ImageView>(R.id.iv_firework1)
        val firework2 = dialogView.findViewById<android.widget.ImageView>(R.id.iv_firework2)
        val firework3 = dialogView.findViewById<android.widget.ImageView>(R.id.iv_firework3)
        val firework4 = dialogView.findViewById<android.widget.ImageView>(R.id.iv_firework4)
        val firework5 = dialogView.findViewById<android.widget.ImageView>(R.id.iv_firework5)
        val firework6 = dialogView.findViewById<android.widget.ImageView>(R.id.iv_firework6)
        val buttonView = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_continue)

        imageView.setImageResource(R.drawable.cheburashka_happy)

        startSimpleFireworks(firework1, 0)
        startSimpleFireworks(firework2, 200)
        startSimpleFireworks(firework3, 400)
        startSimpleFireworks(firework4, 600)
        startSimpleFireworks(firework5, 800)
        startSimpleFireworks(firework6, 1000)

        val dialog = android.app.AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        buttonView.setOnClickListener {
            fireworkAnimators.forEach { it.cancel() }
            fireworkAnimators.clear()
            dialog.dismiss()
            navigateToNextLesson()
        }

        dialog.show()

        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(R.drawable.dialog_background)
            window.setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                android.view.WindowManager.LayoutParams.WRAP_CONTENT
            )
            val params = window.attributes
            params.gravity = android.view.Gravity.CENTER
            window.attributes = params
        }
    }

    private fun navigateToNextLesson() {
        val nextLesson = lesson + 1

        // Проверяем, существует ли следующий урок
        viewModel.checkIfLessonExists(level, nextLesson) { exists ->
            if (exists) {
                // Получаем первый раздел следующего урока
                getFirstSectionOfNextLesson(nextLesson)
            } else {
                // Урок не существует - показываем завершение
                showAllLessonsCompletedDialog()
            }
        }
    }

    private fun getFirstSectionOfNextLesson(nextLesson: Int) {
        lifecycleScope.launch {
            viewModel.getFirstSectionOfLesson(level, nextLesson).collectLatest { section ->
                if (section != null) {
                    // Переходим к первому разделу следующего урока
                    navigateToSection(nextLesson, section)
                } else {
                    // Разделы не найдены - показываем сообщение
                    showNoSectionsDialog(nextLesson)
                }
            }
        }
    }

    private fun navigateToSection(nextLesson: Int, section: com.example.langapp.data.model.Section) {
        val bundle = Bundle().apply {
            putString("LEVEL_KEY", level)
            putInt("TASK_NUMBER_KEY", nextLesson)
            putString("SECTION_NAME_KEY", section.name)
            putString("SECTION_ID_KEY", section.id)
        }

        try {
            findNavController().navigate(R.id.vocabularyFragment, bundle)
        } catch (e: Exception) {
            e.printStackTrace()
            showNavigationErrorDialog()
        }
    }

    private fun showAllLessonsCompletedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Поздравляем!")
            .setMessage("Вы завершили все задания этого урока!")
            .setPositiveButton("Вернуться к урокам") { dialog, _ ->
                dialog.dismiss()
                findNavController().popBackStack(R.id.nav_gallery, false)
            }
            .setCancelable(false)
            .show()
    }

    private fun showNoSectionsDialog(nextLesson: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Внимание")
            .setMessage("В уроке $nextLesson пока нет разделов с заданиями")
            .setPositiveButton("Вернуться к урокам") { dialog, _ ->
                dialog.dismiss()
                findNavController().popBackStack(R.id.nav_gallery, false)
            }
            .setCancelable(false)
            .show()
    }

    private fun showNavigationErrorDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ошибка")
            .setMessage("Не удалось перейти к следующему уроку")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                findNavController().popBackStack(R.id.nav_gallery, false)
            }
            .show()
    }

    private fun startSimpleFireworks(view: android.widget.ImageView, startDelay: Long = 0) {
        view.postDelayed({
            val animator = ValueAnimator.ofFloat(0f, 1f, 0f)
            animator.duration = 1200
            animator.repeatCount = ValueAnimator.INFINITE
            animator.repeatMode = ValueAnimator.RESTART

            animator.addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                view.scaleX = 0.8f + value * 0.4f
                view.scaleY = 0.8f + value * 0.4f
                view.alpha = value
                view.rotation = value * 45f
            }

            animator.start()
            fireworkAnimators.add(animator)
        }, startDelay)
    }

    private fun updateNextButtonState() {
        val currentItem = binding.viewPager.currentItem
        val totalItems = pagerAdapter.itemCount

        if (currentItem < totalItems - 1) {
            binding.btnNext.text = "    Дальше"
            binding.btnNext.isEnabled = true
        } else {
            binding.btnNext.text = "    Завершить"
            binding.btnNext.isEnabled = true
        }
    }

    private fun showEmptyState() {
        binding.apply {
            viewPager.visibility = View.GONE
            tabLayout.visibility = View.GONE
            btnNext.visibility = View.GONE
            cardEmptyState.visibility = View.VISIBLE
            tvEmptyState.text = "Задания для этого раздела пока недоступны"
            tvEmptyState.textSize = 16f
            progressBar.visibility = View.GONE
        }
    }

    private fun showErrorDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ошибка загрузки")
            .setMessage("Раздел не найден")
            .setPositiveButton("OK") { _, _ -> findNavController().navigateUp() }
            .show()
    }
}