package com.example.langapp.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.example.langapp.R

object ThemeHelper {
    fun applyTheme(context: Context) {
        val themeResId = getThemeResId(context)
        context.setTheme(themeResId)
    }

    private fun getThemeResId(context: Context): Int {
        val isDarkTheme = isSystemInDarkTheme(context)
        val useDynamicColors = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

        return when {
            //useDynamicColors && isDarkTheme -> R.style.Theme_LangApp_Dynamic_Dark
            //useDynamicColors && !isDarkTheme -> R.style.Theme_LangApp_Dynamic_Light
            isDarkTheme -> R.style.Theme_LangApp_Dark
            else -> R.style.Theme_LangApp_Light
        }
    }

    private fun isSystemInDarkTheme(context: Context): Boolean {
        return context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
}
