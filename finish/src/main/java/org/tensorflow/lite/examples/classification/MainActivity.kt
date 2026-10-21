package org.tensorflow.lite.examples.classification

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Activity này giờ CHỈ dùng cho chức năng CHỤP ẢNH
 * Nó không còn phân tích real-time
 */
class MainActivity : AppCompatActivity() {

    // Thay thế executor cũ bằng ExecutorService cho ImageCapture
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture // ⭐ THAY THẾ: ImageAnalysis -> ImageCapture
    private lateinit var camera: Camera
    private var cameraProvider: ProcessCameraProvider? = null

    private val viewFinder by lazy {
        findViewById<PreviewView>(R.id.viewFinder)
    }
    private val shutterButton by lazy {
        findViewById<ImageButton>(R.id.ivShutterButton)
    }
    private val backButton by lazy {
        findViewById<ImageButton>(R.id.ivBackButton)
    }
    // ⭐ NÚT CHỌN ẢNH TỪ THƯ VIỆN
    private val galleryButton by lazy {
        findViewById<ImageButton>(R.id.ivGalleryButton)
    }
    private val progressBar by lazy {
        findViewById<ProgressBar>(R.id.captureProgressBar)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Tắt thanh tiêu đề (ActionBar)
        supportActionBar?.hide()

        // Khởi tạo camera executor mới
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // ⭐ ĐÃ SỬA: Xóa logic RecyclerView

        // Thêm listener cho nút chụp ảnh
        shutterButton.setOnClickListener {
            takePhoto()
        }

        // Thêm listener cho nút quay lại
        backButton.setOnClickListener {
            finish()
        }

        // ⭐ THÊM VÀO ĐÂY: Listener cho nút chọn ảnh
        galleryButton.setOnClickListener {
            // Khởi chạy SelectImageActivity (đã có sẵn)
            val intent = Intent(this, SelectImageActivity::class.java)
            startActivity(intent)
        }
    }

    private fun takePhoto() {
        // Hiển thị loading và ẩn nút chụp
        runOnUiThread {
            shutterButton.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
        }

        // Tạo tên tệp dựa trên thời gian
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        // Cấu hình thông tin tệp ảnh
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MushroomApp-Images")
            }
        }

        // Cấu hình output options
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // Chụp ảnh
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor, // Chạy trên luồng riêng
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Chụp ảnh thất bại: ${exc.message}", exc)
                    runOnUiThread {
                        Toast.makeText(baseContext, "Lỗi khi chụp ảnh", Toast.LENGTH_SHORT).show()
                        // Hiện lại nút chụp
                        shutterButton.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
                    }
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri
                    Log.d(TAG, "Ảnh đã được lưu: $savedUri")

                    // ⭐ GỬI KẾT QUẢ SANG AnalysisResultActivity
                    runOnUiThread {
                        // Tắt loading
                        shutterButton.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE

                        // Tạo intent
                        val intent = Intent(this@MainActivity, AnalysisResultActivity::class.java)
                        // Gửi URI của ảnh vừa chụp
                        intent.putExtra("IMAGE_URI", savedUri.toString())
                        startActivity(intent)
                    }
                }
            }
        )
    }


    private fun allPermissionsGranted(): Boolean = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.permission_deny_text),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    // ⭐ ĐÃ SỬA: startCamera (chỉ còn Preview và ImageCapture)
    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            this.cameraProvider = cameraProvider

            preview = Preview.Builder().build()

            // Khởi tạo ImageCapture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture // BIND ImageCapture
                )
                preview.setSurfaceProvider(viewFinder.surfaceProvider)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                Toast.makeText(this, "Không thể khởi động camera.", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // --- ĐÃ XÓA: Toàn bộ lớp `private class ImageAnalyzer` ---
    // --- ĐÃ XÓA: Hàm `runClassification` ---
    // --- ĐÃ XÓA: Hàm `showResults` ---
    // --- ĐÃ XÓA: Logic Model GPU/CPU (đã chuyển sang AnalysisResultActivity) ---

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        // --- ĐÃ XÓA: Logic đóng model ---
    }

    companion object {
        // --- ĐÃ XÓA: MAX_RESULT_DISPLAY ---
        private const val TAG = "TFL Classify"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 999
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}