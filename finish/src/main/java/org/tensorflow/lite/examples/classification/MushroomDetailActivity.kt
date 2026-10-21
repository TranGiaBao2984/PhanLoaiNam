package org.tensorflow.lite.examples.classification

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView // ⭐ THÊM IMPORT
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.graphics.toColorInt

class MushroomDetailActivity : AppCompatActivity() {

    private lateinit var mushroomHelper: MushroomDataHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mushroom_detail)

        // ⭐ ĐÃ SỬA: Thêm listener cho nút back XML
        val backButton: ImageButton = findViewById(R.id.ivBackButton)
        backButton.setOnClickListener {
            finish() // Quay lại
        }

        mushroomHelper = MushroomDataHelper(this)

        // Lấy tên nấm từ Intent
        val scientificName = intent.getStringExtra("SCIENTIFIC_NAME") ?: ""
        val mushroomInfo = mushroomHelper.getMushroomInfo(scientificName)

        if (mushroomInfo != null) {
            displayMushroomDetails(mushroomInfo)
        } else {
            findViewById<TextView>(R.id.tvDetailScientificName).text = getString(R.string.mushroom_not_found)
        }
    }

    private fun displayMushroomDetails(info: MushroomInfo) {

        // ⭐ Tải ảnh chi tiết
        val ivDetailImage: ImageView = findViewById(R.id.ivDetailImage)
        if (info.sampleImage.isNotEmpty()) {
            // SỬA: Xóa "assets/" khỏi đường dẫn trong JSON
            val imagePath = info.sampleImage.removePrefix("assets/")
            val bitmap = AssetImageLoader.loadBitmapFromAssets(this, imagePath)
            ivDetailImage.setImageBitmap(bitmap)
        } else {
            // Ẩn ImageView nếu không có ảnh
            ivDetailImage.visibility = View.GONE
        }

        // Tên nấm
        findViewById<TextView>(R.id.tvDetailScientificName).text = info.scientificName
        findViewById<TextView>(R.id.tvDetailCommonName).text = info.commonName

        // Độc tính
        val tvToxicity = findViewById<TextView>(R.id.tvDetailToxicity)
        tvToxicity.text = info.toxicityLabel

        val toxicityColor = when (info.toxicity) {
            "edible" -> "#4CAF50".toColorInt()  // Xanh lá
            "toxic" -> "#F44336".toColorInt()   // Đỏ
            else -> "#FF9800".toColorInt()      // Cam
        }
        tvToxicity.setTextColor(toxicityColor)

        findViewById<TextView>(R.id.tvDetailToxicityNote).text = info.toxicityNote

        // Mô tả
        findViewById<TextView>(R.id.tvDetailDescription).text = info.description

        // Môi trường
        findViewById<TextView>(R.id.tvDetailHabitat).text = info.habitat

        // ⭐ Triệu chứng (chỉ hiện khi có)
        val cardSymptoms = findViewById<CardView>(R.id.cardSymptoms)
        val tvSymptoms = findViewById<TextView>(R.id.tvDetailSymptoms)

        if (info.symptoms.isNotEmpty()) {
            cardSymptoms.visibility = View.VISIBLE
            tvSymptoms.text = info.symptoms
        } else {
            cardSymptoms.visibility = View.GONE
        }

        // ⭐ THÊM MỚI: Dược tính y học (chỉ hiện khi có dữ liệu)
        val cardMedicinal = findViewById<CardView>(R.id.cardMedicinal)
        val tvDetailMedicinal = findViewById<TextView>(R.id.tvDetailMedicinal)

        val medicinalText = info.medicinal
        if (!medicinalText.isNullOrEmpty()) {
            cardMedicinal.visibility = View.VISIBLE
            tvDetailMedicinal.text = medicinalText
        } else {
            // Nếu loài nấm nào trong JSON không có trường medicinal, tự động ẩn Card này
            cardMedicinal.visibility = View.GONE
        }

        // Nguồn tham khảo
        findViewById<TextView>(R.id.tvDetailReference).text = info.reference
    }
}