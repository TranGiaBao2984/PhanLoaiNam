package org.tensorflow.lite.examples.classification

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

object ThemeUtils {

    private const val PREFS_NAME = "app_settings"
    private const val KEY_DARK_MODE = "dark_mode"

    // Sửa ở đây: Bỏ 'private' và đổi tên 'isDarkModeInternal' -> 'isDarkMode'
    fun isDarkMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun applySavedTheme(context: Context) {
        // Giờ chúng ta gọi hàm đã sửa
        val isDark = isDarkMode(context)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun toggleTheme(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Giờ chúng ta gọi hàm đã sửa
        val current = isDarkMode(context)
        val newValue = !current

        prefs.edit { putBoolean(KEY_DARK_MODE, newValue) }

        AppCompatDelegate.setDefaultNightMode(
            if (newValue) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}