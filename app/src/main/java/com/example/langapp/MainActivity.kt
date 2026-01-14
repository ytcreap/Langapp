// MainActivity.kt
package com.example.langapp

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.langapp.databinding.ActivityMainBinding
import com.example.langapp.utils.UserProfileManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController

    // Views для навигационного меню
    private lateinit var tvUserName: TextView
    private lateinit var tvUserGroup: TextView

    companion object {
        const val PREFS_NAME = "theme_prefs"
        const val KEY_THEME = "app_theme"
        const val THEME_LIGHT = 0
        const val THEME_DARK = 1
        const val THEME_SYSTEM = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Устанавливаем тему перед super.onCreate()
        applySavedTheme()
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_content_main)

        // Находим Views в заголовке навигационного меню
        val headerView = navView.getHeaderView(0)
        tvUserName = headerView.findViewById(R.id.tvUserName)
        tvUserGroup = headerView.findViewById(R.id.tvUserGroup)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow, R.id.nav_logout
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Наблюдаем за изменениями данных пользователя
        setupUserProfileObserver()

        // Обработка нажатия кнопки выхода и других пунктов
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    signOut()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> {
                    // Проверяем авторизацию перед навигацией
                    if (auth.currentUser != null) {
                        // Делегируем обработку Navigation Component
                        val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
                        drawerLayout.closeDrawer(GravityCompat.START)
                        handled
                    } else {
                        // Показываем сообщение о необходимости авторизации
                        Snackbar.make(binding.root, "Для доступа необходимо войти в систему", Snackbar.LENGTH_SHORT).show()
                        drawerLayout.closeDrawer(GravityCompat.START)
                        false
                    }
                }
            }
        }

        // Запускаем навигацию после настройки всего остального
        navigateBasedOnAuthState()
    }

    private fun setupUserProfileObserver() {
        UserProfileManager.userProfile.observe(this, Observer { profile ->
            if (profile != null) {
                tvUserName.text = profile.fullName
                tvUserGroup.text = profile.group
            } else {
                // Пользователь не авторизован
                tvUserName.text = getString(R.string.full_name_template)
                tvUserGroup.text = getString(R.string.group_template)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        // Вся логика проверки авторизации теперь в navigateBasedOnAuthState()
        // которая вызывается в onCreate
    }

    private fun navigateBasedOnAuthState() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            // Пользователь не вошел в систему - остаемся на loginFragment
            // Проверяем, что мы не уже на loginFragment
            val currentDestination = navController.currentDestination?.id
            if (currentDestination != R.id.nav_log) {
                navController.navigate(R.id.nav_log)
            }
        } else {
            // Пользователь авторизован - переходим на galleryFragment
            // Проверяем, что мы не уже на galleryFragment
            val currentDestination = navController.currentDestination?.id
            if (currentDestination != R.id.nav_gallery && currentDestination != R.id.nav_home) {
                navigateToGallery()
            }
        }
    }

    private fun navigateToGallery() {
        try {
            // Используем popUpTo чтобы очистить стек навигации
            navController.navigate(R.id.nav_gallery) {
                // Очищаем стек навигации до loginFragment
                popUpTo(R.id.nav_log) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        } catch (e: Exception) {
            // Если произошла ошибка, пробуем простую навигацию
            navController.navigate(R.id.nav_gallery)
        }
    }

    public fun signOut() {
        // Выход из Firebase
        auth.signOut()

        // Если пользователь вошел через Google, выход из Google Sign-In
        val googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)
        googleSignInClient.signOut().addOnCompleteListener {
            // Очищаем данные в меню через менеджер
            UserProfileManager.clearUserData()
            // Переход на экран входа
            navController.navigate(R.id.nav_log)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_theme -> {
                showThemeDialog()
                true
            }
            R.id.action_settings -> {
                // Проверяем авторизацию для доступа к настройкам
                if (auth.currentUser != null) {
                    Snackbar.make(binding.root, "Настройки", Snackbar.LENGTH_SHORT).show()
                    true
                } else {
                    Snackbar.make(binding.root, "Для доступа к настройкам необходимо войти в систему", Snackbar.LENGTH_SHORT).show()
                    false
                }
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun showThemeDialog() {
        val themes = arrayOf("Светлая тема", "Тёмная тема", "Системная тема")
        val currentTheme = getSavedTheme()

        AlertDialog.Builder(this)
            .setTitle("Выберите тему")
            .setSingleChoiceItems(themes, currentTheme) { dialog, which ->
                saveTheme(which)
                applyTheme(which)
                dialog.dismiss()
                recreate()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun applySavedTheme() {
        applyTheme(getSavedTheme())
    }

    private fun applyTheme(theme: Int) {
        when (theme) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun getSavedTheme(): Int {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME, THEME_SYSTEM)
    }

    private fun saveTheme(theme: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME, theme).apply()
    }
}