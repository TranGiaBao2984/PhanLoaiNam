package org.tensorflow.lite.examples.classification

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

class MushroomDataHelper(private val context: Context) {

    private var mushroomDatabase: MushroomDatabase? = null
    var labels: List<String> = emptyList()

    init {
        loadMushroomData()
        loadLabels()
    }

    private fun loadMushroomData() {
        try {
            val jsonString = context.assets.open("mushroom_vi_updated.json")
                .bufferedReader().use { it.readText() }
            mushroomDatabase = Gson().fromJson(jsonString, MushroomDatabase::class.java)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadLabels() {
        try {
            val jsonString = context.assets.open("labels.json")
                .bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<String>>() {}.type
            labels = Gson().fromJson(jsonString, listType)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getMushroomInfo(scientificName: String): MushroomInfo? {
        return mushroomDatabase?.mushrooms?.get(scientificName)
    }

    // ✅ THÊM HÀM NÀY VÀO
    fun getAllMushrooms(): List<MushroomInfo> {
        return mushroomDatabase?.mushrooms?.values?.toList() ?: emptyList()
    }
}