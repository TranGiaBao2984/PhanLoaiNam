package org.tensorflow.lite.examples.classification

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity này là "trung gian", không có layout.
 * Nhiệm vụ duy nhất của nó là mở trình chọn ảnh,
 * nhận kết quả, gửi nó đến AnalysisResultActivity, rồi tự đóng lại.
 */
class SelectImageActivity : AppCompatActivity() {

    // Khởi tạo trình chọn ảnh
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Nếu người dùng chọn ảnh, gửi URI đến AnalysisResultActivity
            val intent = Intent(this, AnalysisResultActivity::class.java)
            intent.putExtra("IMAGE_URI", uri.toString())
            startActivity(intent)
        }
        // Dù người dùng có chọn hay không, cũng đóng Activity trung gian này
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ngay khi Activity mở, chạy trình chọn ảnh
        galleryLauncher.launch("image/*")
    }
}