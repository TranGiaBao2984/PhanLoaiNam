package org.tensorflow.lite.examples.classification

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Sửa tên class và kế thừa từ Fragment
class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Khai báo View
    private lateinit var ivBackButton: ImageButton
    private lateinit var ivUserPhoto: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserId: TextView
    private lateinit var tvCreatedDate: TextView
    private lateinit var tvLastLogin: TextView
    private lateinit var tvLoginCount: TextView
    private lateinit var btnLogout: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo Firebase
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()

        // Ánh xạ View (phải dùng 'view.findViewById')
        ivBackButton = view.findViewById(R.id.ivBackButton)
        ivUserPhoto = view.findViewById(R.id.ivUserPhoto)
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        tvUserId = view.findViewById(R.id.tvUserId)
        tvCreatedDate = view.findViewById(R.id.tvCreatedDate)
        tvLastLogin = view.findViewById(R.id.tvLastLogin)
        tvLoginCount = view.findViewById(R.id.tvLoginCount)
        btnLogout = view.findViewById(R.id.btnLogout)

        // Xử lý nút Back - (Trong Fragment, chúng ta không 'finish()')
        // Chúng ta sẽ ẩn nó đi vì đã có BottomNav
        ivBackButton.visibility = View.GONE

        // Lấy thông tin user
        val currentUser = auth.currentUser
        if (currentUser != null) {
            displayUserInfo(currentUser.uid)
        } else {
            // Không có user, quay về LoginActivity
            // Phải dùng 'requireActivity()' để gọi context
            startActivity(Intent(requireActivity(), LoginActivity::class.java))
            requireActivity().finish()
            return
        }

        // Xử lý nút đăng xuất
        btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    // Không cần hàm navigateToHome() nữa

    private fun displayUserInfo(userId: String) {
        val user = auth.currentUser
        user?.let {
            tvUserName.text = it.displayName ?: "Người dùng"
            tvUserEmail.text = it.email ?: "Không có email"
            tvUserId.text = userId

            // Lấy thông tin từ Firestore
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val loginCount = document.getLong("loginCount") ?: 0
                        tvLoginCount.text = "$loginCount lần"
                        val createdAt = document.getDate("createdAt")
                        if (createdAt != null) {
                            tvCreatedDate.text = formatDate(createdAt)
                        }
                        val lastLoginAt = document.getDate("lastLoginAt")
                        if (lastLoginAt != null) {
                            tvLastLogin.text = formatDateTime(lastLoginAt)
                        }
                    } else {
                        createUserProfile(userId)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("UserProfile", "Lỗi khi lấy dữ liệu: ${e.message}")
                    tvLoginCount.text = "Không có dữ liệu"
                }
        }
    }

    private fun createUserProfile(userId: String) {
        val user = auth.currentUser ?: return
        val userProfile = hashMapOf(
            "userId" to userId,
            "displayName" to user.displayName,
            "email" to user.email,
            "photoUrl" to user.photoUrl.toString(),
            "createdAt" to Date(),
            "lastLoginAt" to Date(),
            "loginCount" to 1
        )
        db.collection("users").document(userId)
            .set(userProfile)
            .addOnSuccessListener {
                Log.d("UserProfile", "Đã tạo profile mới")
                displayUserInfo(userId)
            }
            .addOnFailureListener { e ->
                Log.e("UserProfile", "Lỗi khi tạo profile: ${e.message}")
            }
    }

    private fun showLogoutDialog() {
        // Phải dùng 'requireContext()'
        AlertDialog.Builder(requireContext())
            .setTitle("Đăng Xuất")
            .setMessage("Bạn có chắc muốn đăng xuất?")
            .setPositiveButton("Đăng xuất") { dialog, _ ->
                auth.signOut()
                Toast.makeText(requireContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show()

                // Chuyển về LoginActivity
                val intent = Intent(requireActivity(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Các hàm helper (giữ nguyên)
    private fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formatter.format(date)
    }

    private fun formatDateTime(date: Date): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return formatter.format(date)
    }
}