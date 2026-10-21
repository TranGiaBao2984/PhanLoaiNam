package org.tensorflow.lite.examples.classification

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class MushroomApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Tải theme đã lưu khi ứng dụng khởi động
        val sharedPreferences = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        val nightMode = sharedPreferences.getInt("NightMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}