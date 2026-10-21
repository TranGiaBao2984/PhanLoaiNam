package org.tensorflow.lite.examples.classification

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    private lateinit var btnGoogleSignIn: MaterialButton
    private lateinit var progressBar: ProgressBar

    // Hằng số TAG để log
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Khởi tạo Firebase Auth
        auth = Firebase.auth

        // 2. ⭐ KIỂM TRA TÌNH TRẠNG ĐĂNG NHẬP NGAY LẬP TỨC
        if (auth.currentUser != null) {
            // Nếu người dùng đã đăng nhập (phiên được lưu từ lần trước),
            // đi thẳng vào HomeActivity
            Log.d(TAG, "Người dùng đã đăng nhập (UID: ${auth.currentUser!!.uid}), chuyển sang HomeActivity.")
            goToHomeActivity()
            return // Rất quan trọng: Dừng thực thi onCreate tại đây
        }

        // 3. Nếu CHƯA đăng nhập, MỚI hiển thị layout đăng nhập
        setContentView(R.layout.activity_login)
        Log.d(TAG, "Chưa đăng nhập, hiển thị màn hình Login.")

        // Ánh xạ views
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        progressBar = findViewById(R.id.loginProgressBar)

        // 4. Cấu hình Google Sign-In
        // Lấy web client ID từ file google-services.json (thường tự động)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Cần file strings.xml hoặc google-services.json
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 5. Đăng ký ActivityResultLauncher để nhận kết quả từ Google Sign-In
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    // Google Sign In thành công, bây giờ xác thực với Firebase
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d(TAG, "Google Sign In thành công, xác thực với Firebase...")
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    // Google Sign In thất bại
                    Log.w(TAG, "Google Sign In thất bại", e)
                    Toast.makeText(this, "Đăng nhập Google thất bại", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            } else {
                // Người dùng có thể đã hủy bỏ
                Log.w(TAG, "Google Sign In bị hủy hoặc thất bại, resultCode: ${result.resultCode}")
                Toast.makeText(this, "Đăng nhập đã bị hủy", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }

        // 6. Xử lý sự kiện click nút đăng nhập
        btnGoogleSignIn.setOnClickListener {
            Log.d(TAG, "Nút đăng nhập Google được nhấn.")
            showLoading(true)
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        // Bắt đầu quy trình đăng nhập Google
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Đăng nhập Firebase thành công, chuyển sang HomeActivity
                    Log.d(TAG, "Firebase Auth với Google: thành công")
                    val user = auth.currentUser
                    // (Bạn có thể lưu thông tin user vào Firestore tại đây nếu cần)

                    showLoading(false)
                    goToHomeActivity()
                } else {
                    // Đăng nhập Firebase thất bại
                    Log.w(TAG, "Firebase Auth với Google: thất bại", task.exception)
                    Toast.makeText(this, "Xác thực Firebase thất bại.", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            }
    }

    private fun goToHomeActivity() {
        // Chuyển sang HomeActivity
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)

        // Rất quan trọng: Đóng LoginActivity để người dùng
        // không thể nhấn "Back" quay lại màn hình này
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            btnGoogleSignIn.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            btnGoogleSignIn.isEnabled = true
        }
    }
}