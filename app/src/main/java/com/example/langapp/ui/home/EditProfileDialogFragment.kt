// EditProfileDialogFragment.kt
package com.example.langapp.ui.home

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.langapp.R
import com.example.langapp.databinding.DialogEditProfileBinding
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class EditProfileDialogFragment : DialogFragment() {

    private lateinit var binding: DialogEditProfileBinding
    private val viewModel: HomeViewModel by activityViewModels()

    interface EditProfileListener {
        fun onProfileUpdated()
    }

    private var listener: EditProfileListener? = null

    fun setEditProfileListener(listener: EditProfileListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogEditProfileBinding.inflate(LayoutInflater.from(context))

        // Загружаем текущие данные пользователя
        viewModel.userProfile.value?.let { profile ->
            binding.etFullName.setText(profile.fullName)
            binding.etGroup.setText(profile.group)
        }

        // Устанавливаем текущий email
        val currentEmail = viewModel.getCurrentEmail()
        binding.etEmail.setText(currentEmail)

        // Проверяем, является ли пользователь Google-пользователем
        val isGoogleUser = viewModel.isGoogleUser()
        if (isGoogleUser) {
            // Для Google-пользователей отключаем редактирование email и пароля
            binding.tilEmail.visibility = View.GONE
            binding.tilCurrentPassword.visibility = View.GONE
            binding.tilNewPassword.visibility = View.GONE
            binding.tilConfirmPassword.visibility = View.GONE
            binding.tvEmailNote.visibility = View.VISIBLE
            binding.tvEmailNote.text = "Email и пароль управляются через Google аккаунт"
        } else {
            // Для обычных пользователей показываем поля для смены email/пароля
            binding.tilEmail.visibility = View.VISIBLE
            binding.tvEmailNote.visibility = View.GONE

            // Кнопка смены email
            binding.btnChangeEmail.setOnClickListener {
                showChangeEmailDialog(currentEmail)
            }

            // Валидация паролей
            setupPasswordValidation()
        }

        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Редактирование профиля")
            .setView(binding.root)
            .setPositiveButton("Сохранить") { dialog, _ ->
                saveProfileChanges()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }

        return builder.create()
    }

    private fun setupPasswordValidation() {
        val passwordWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePasswords()
            }
        }

        binding.etNewPassword.addTextChangedListener(passwordWatcher)
        binding.etConfirmPassword.addTextChangedListener(passwordWatcher)
    }

    private fun validatePasswords(): Boolean {
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        return when {
            newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword -> {
                binding.tilConfirmPassword.error = "Пароли не совпадают"
                false
            }
            newPassword.isNotEmpty() && newPassword.length < 6 -> {
                binding.tilNewPassword.error = "Пароль должен содержать минимум 6 символов"
                false
            }
            else -> {
                binding.tilNewPassword.error = null
                binding.tilConfirmPassword.error = null
                true
            }
        }
    }

    private fun showChangeEmailDialog(currentEmail: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_change_email, null)
        val etCurrentEmail = dialogView.findViewById<EditText>(R.id.etCurrentEmail)
        val etNewEmail = dialogView.findViewById<EditText>(R.id.etNewEmail)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)

        // Устанавливаем текущий email
        etCurrentEmail.setText(currentEmail)

        AlertDialog.Builder(requireContext())
            .setTitle("Смена email")
            .setView(dialogView)
            .setPositiveButton("Изменить") { dialog, _ ->
                val newEmail = etNewEmail.text.toString()
                val password = etPassword.text.toString()

                if (newEmail.isNotEmpty() && password.isNotEmpty()) {
                    lifecycleScope.launch {
                        val success = viewModel.updateEmail(currentEmail, newEmail, password)
                        if (success) {
                            Toast.makeText(context, "Email успешно изменен", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Ошибка при смене email", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun saveProfileChanges() {
        val fullName = binding.etFullName.text.toString().trim()
        val group = binding.etGroup.text.toString().trim()

        if (fullName.isEmpty()) {
            binding.tilFullName.error = "Введите ФИО"
            return
        }

        if (group.isEmpty()) {
            binding.tilGroup.error = "Введите группу"
            return
        }

        // Обновляем профиль
        viewModel.updateProfile(fullName, group)

        // Если нужно обновить пароль
        if (!viewModel.isGoogleUser()) {
            val currentPassword = binding.etCurrentPassword.text.toString()
            val newPassword = binding.etNewPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (newPassword.isNotEmpty()) {
                if (validatePasswords()) {
                    lifecycleScope.launch {
                        val currentEmail = viewModel.getCurrentEmail()
                        if (currentPassword.isNotEmpty()) {
                            val success = viewModel.updatePassword(currentEmail, currentPassword, newPassword)
                            if (success) {
                                Toast.makeText(context, "Пароль успешно изменен", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Ошибка при смене пароля", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    return
                }
            }
        }

        listener?.onProfileUpdated()
        dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        listener = null
    }
}