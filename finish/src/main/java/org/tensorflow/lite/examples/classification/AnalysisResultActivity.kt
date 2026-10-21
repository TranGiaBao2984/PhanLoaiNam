package org.tensorflow.lite.examples.classification

// Import thư viện GPU
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
// Firebase (Auth và Firestore)
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.util.Date
// TFLite
import org.tensorflow.lite.DataType
import org.tensorflow.lite.examples.classification.ml.MushroomModel
import org.tensorflow.lite.examples.classification.ui.RecognitionAdapter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.support.model.Model
import java.util.concurrent.Executors
import kotlin.math.exp
import java.util.UUID

// ⭐ --- IMPORT TỪ REPORTFRAGMENT.KT ---
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// ⭐ --- KẾT THÚC IMPORT ---

class AnalysisResultActivity : AppCompatActivity() {

    private lateinit var mushroomHelper: MushroomDataHelper
    private lateinit var recognitionAdapter: RecognitionAdapter
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val modelInputWidth = 512
    private val modelInputHeight = 512
    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(modelInputHeight, modelInputWidth, ResizeOp.ResizeMethod.BILINEAR))
        // Dùng Mean và Std của ImageNet nhân cho 255
        .add(NormalizeOp(
            floatArrayOf(123.675f, 116.28f, 103.53f),
            floatArrayOf(58.395f, 57.12f, 57.375f)
        ))
        .build()

    // 1. Tầng GPU
    private val mushroomModelGpu: MushroomModel? by lazy {
        val compatList = CompatibilityList()
        if(compatList.isDelegateSupportedOnThisDevice){
            Log.d("AnalysisResultActivity", "GPU is supported. Initializing GPU model...")
            val options = Model.Options.Builder().setDevice(Model.Device.GPU).build()
            try {
                MushroomModel.newInstance(this, options)
            } catch (e: Exception) {
                Log.e("AnalysisResultActivity", "Failed to initialize GPU model, will fallback to CPU.", e)
                null
            }
        } else {
            Log.d("AnalysisResultActivity", "GPU not supported on this device.")
            null
        }
    }

    // 2. Tầng CPU (Fallback)
    private val mushroomModelCpu: MushroomModel by lazy {
        Log.d("AnalysisResultActivity", "Initializing CPU fallback model.")
        val options = Model.Options.Builder().setNumThreads(4).build()
        MushroomModel.newInstance(this, options)
    }
    // ---

    private val confidenceThresholdHigh = 0.70f
    private val confidenceThresholdLow = 0.50f

    // ⭐ --- BIẾN TỪ REPORTFRAGMENT.KT ---
    private val auth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()
    private lateinit var supabaseClient: SupabaseClient
    private val bucketName = "reports"
    private var imageUri: Uri? = null
    private var imageBytesForUpload: ByteArray? = null
    // ⭐ --- KẾT THÚC BIẾN ---

    // Biến UI
    private lateinit var layoutLoading: LinearLayout
    private lateinit var tvLoadingMessage: TextView
    private lateinit var tvWarningMessage: TextView
    private lateinit var cardResults: CardView
    private lateinit var recyclerView: RecyclerView

    // ⭐ BIẾN UI CHO FORM BÁO CÁO MỚI
    private lateinit var layoutReport: LinearLayout
    private lateinit var etReportNotes: TextInputEditText
    private lateinit var btnSubmitReport: MaterialButton
    private lateinit var reportProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis_result)

        val backButton: ImageButton = findViewById(R.id.ivBackButton)
        backButton.setOnClickListener {
            finish()
        }

        mushroomHelper = MushroomDataHelper(this)
        recognitionAdapter = RecognitionAdapter(mushroomHelper)

        // Khởi tạo UI
        recyclerView = findViewById(R.id.rvAnalysisResults)
        layoutLoading = findViewById(R.id.layoutLoading)
        tvLoadingMessage = findViewById(R.id.tvLoadingMessage)
        tvWarningMessage = findViewById(R.id.tvWarningMessage)
        cardResults = findViewById(R.id.cardResults)

        // ⭐ KHỞI TẠO UI BÁO CÁO
        layoutReport = findViewById(R.id.layoutReport)
        etReportNotes = findViewById(R.id.etReportNotes)
        btnSubmitReport = findViewById(R.id.btnSubmitReport)
        reportProgressBar = findViewById(R.id.reportProgressBar)

        recyclerView.adapter = recognitionAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.itemAnimator = null

        // ⭐ KHỞI TẠO SUPABASE CLIENT
        val supabaseUrl = "https://irfderlnpxowgnakuuyq.supabase.co"
        val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImlyZmRlcmxucHhvd2duYWt1dXlxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI4OTA2NTAsImV4cCI6MjA3ODQ2NjY1MH0.6AUDeLe_7TbHnJ8tpLZxvQepumV2wZ-jBiOW3HW_3eo"

        supabaseClient = createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        ) {
            install(Storage)
            install(Auth)
            httpEngine = Android.create()
        }

        // Lấy URI và phân tích
        val uriString = intent.getStringExtra("IMAGE_URI")
        if (uriString != null) {
            imageUri = uriString.toUri()
            val bitmap = getBitmapFromUri(imageUri!!)
            if (bitmap != null) {
                val imageView: ImageView = findViewById(R.id.ivSelectedImage)
                imageView.setImageBitmap(bitmap)
                analyzeImage(bitmap)
            } else {
                showError(getString(R.string.error_load_image))
            }
        } else {
            showError(getString(R.string.error_no_uri))
        }

        // ⭐ SET ONCLICK CHO NÚT BÁO CÁO
        btnSubmitReport.setOnClickListener {
            imageUri?.let { uri ->
                submitSupabaseReport(uri, etReportNotes.text.toString())
            } ?: run {
                Toast.makeText(this, "Lỗi: Không tìm thấy ảnh để gửi.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                imageBytesForUpload = inputStream.readBytes()
            }

            if (imageBytesForUpload == null) {
                throw Exception("Không thể đọc byte array từ URI")
            }

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            Log.e("AnalysisResultActivity", "Failed to load bitmap/bytes from URI: $uri", e)
            imageBytesForUpload = null
            return null
        }
    }

    private fun analyzeImage(bitmap: Bitmap) {
        layoutLoading.visibility = View.VISIBLE
        tvWarningMessage.visibility = View.GONE
        cardResults.visibility = View.GONE
        layoutReport.visibility = View.GONE

        cameraExecutor.execute {
            try {
                var tensorImage = TensorImage(DataType.FLOAT32)
                tensorImage.load(bitmap)
                tensorImage = imageProcessor.process(tensorImage)

                // CƠ CHẾ FALLBACK 2 TẦNG: GPU -> CPU
                val outputs =
                    if (mushroomModelGpu != null) {
                        try {
                            Log.d("AnalysisResultActivity", "Running analysis on GPU...")
                            mushroomModelGpu!!.process(tensorImage.tensorBuffer)
                        } catch (e: Exception) {
                            Log.w("AnalysisResultActivity", "GPU analysis FAILED! Retrying on CPU.", e)
                            mushroomModelCpu.process(tensorImage.tensorBuffer)
                        }
                    }
                    else {
                        Log.d("AnalysisResultActivity", "Running analysis on CPU (GPU not available).")
                        mushroomModelCpu.process(tensorImage.tensorBuffer)
                    }

                val outputBuffer = outputs.outputFeature0AsTensorBuffer
                val logits = outputBuffer.floatArray

                val labels = mushroomHelper.labels

                val maxLogit = logits.maxOrNull() ?: 0.0f
                val expLogits = logits.map { exp(it - maxLogit) }
                val sumExpLogits = expLogits.sum()
                val probabilities = expLogits.map { (it / sumExpLogits) }
                val results = probabilities.mapIndexed { index, confidence ->
                    if (index < labels.size) {
                        Category(labels[index], confidence)
                    } else {
                        Category("Unknown", 0f)
                    }
                }.sortedByDescending { it.score }
                    .take(1)

                val topResult = results.firstOrNull()

                runOnUiThread {
                    layoutLoading.visibility = View.GONE

                    when {
                        // Ngưỡng 3: Rất Không Chắc -> ⭐ HIỂN THỊ FORM BÁO CÁO
                        topResult == null || topResult.score < confidenceThresholdLow -> {
                            showError(getString(R.string.error_no_recognition))
                        }

                        // Ngưỡng 2: Khá Tự Tin
                        topResult.score < confidenceThresholdHigh -> {
                            tvWarningMessage.text = getString(R.string.warning_low_confidence, topResult.label)
                            tvWarningMessage.visibility = View.VISIBLE
                            cardResults.visibility = View.VISIBLE
                            layoutReport.visibility = View.GONE
                            recognitionAdapter.submitList(results)
                            saveResultToHistory(topResult)
                        }

                        // Ngưỡng 1: Rất Tự Tin
                        else -> {
                            tvWarningMessage.visibility = View.GONE
                            cardResults.visibility = View.VISIBLE
                            layoutReport.visibility = View.GONE
                            recognitionAdapter.submitList(results)
                            saveResultToHistory(topResult)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AnalysisResultActivity", "FATAL: Analysis failed completely (even on CPU).", e)
                runOnUiThread {
                    showError(getString(R.string.error_analysis_failed))
                }
            }
        }
    }

    private fun saveResultToHistory(result: Category) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("AnalysisResult", "Không có User ID, không thể lưu lịch sử")
            return
        }

        val mushroomInfo = mushroomHelper.getMushroomInfo(result.label)

        val historyEntry = HistoryItem(
            scientificName = result.label,
            commonName = mushroomInfo?.commonName ?: "Không rõ",
            confidence = result.score,
            sampleImage = mushroomInfo?.sampleImage ?: "",
            timestamp = Date()
        )
        db.collection("users").document(userId)
            .collection("history")
            .add(historyEntry)
            .addOnSuccessListener {
                Log.d("AnalysisResult", "Đã lưu kết quả vào lịch sử: ${it.id}")
            }
            .addOnFailureListener { e ->
                Log.w("AnalysisResult", "Lỗi khi lưu lịch sử", e)
            }
    }

    // ⭐ --- BẮT ĐẦU CÁC HÀM ĐƯỢC DI CHUYỂN TỪ REPORTFRAGMENT ---

    private fun submitSupabaseReport(imageUri: Uri, notes: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Lỗi: Chưa đăng nhập", Toast.LENGTH_SHORT).show()
            return
        }

        showReportLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val filename = UUID.randomUUID().toString() + ".jpg"
                val filePath = "reports/$userId/$filename"

                val imageBytes =
                    imageBytesForUpload ?:
                    throw Exception("Không thể đọc file ảnh (byte array rỗng)")

                val bucket = supabaseClient.storage.from(bucketName)

                bucket.upload(filePath, imageBytes) {
                    upsert = false
                }

                val publicUrl = bucket.publicUrl(filePath)

                withContext(Dispatchers.Main) {
                    saveReportToFirestore(publicUrl, notes, userId)
                }

            } catch (e: Exception) {
                Log.w("AnalysisResult", "Tải ảnh thất bại", e)
                withContext(Dispatchers.Main) {
                    showReportLoading(false)
                    Toast.makeText(this@AnalysisResultActivity, "Tải ảnh thất bại: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveReportToFirestore(imageUrl: String, notes: String, userId: String) {
        val reportData = hashMapOf(
            "userId" to userId,
            "imageUrl" to imageUrl,
            "notes" to notes,
            "timestamp" to Date(),
            "status" to "pending"
        )

        db.collection("reports")
            .add(reportData)
            .addOnSuccessListener {
                showReportLoading(false)
                Toast.makeText(this, "Gửi báo cáo thành công!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.w("AnalysisResult", "Lưu báo cáo thất bại", e)
                showReportLoading(false)
                Toast.makeText(this, "Lưu báo cáo thất bại", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showReportLoading(isLoading: Boolean) {
        reportProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSubmitReport.isEnabled = !isLoading
        etReportNotes.isEnabled = !isLoading
    }

    // ⭐ --- KẾT THÚC CÁC HÀM ĐƯỢC DI CHUYỂN TỪ REPORTFRAGMENT ---

    private fun showError(message: String) {
        layoutLoading.visibility = View.GONE
        cardResults.visibility = View.GONE
        tvWarningMessage.text = message
        tvWarningMessage.visibility = View.VISIBLE
        layoutReport.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        mushroomModelGpu?.close()
        mushroomModelCpu.close()
    }
}