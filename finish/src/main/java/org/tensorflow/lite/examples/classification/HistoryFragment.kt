package org.tensorflow.lite.examples.classification

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar // ⭐ Đảm bảo có import này
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import org.tensorflow.lite.examples.classification.ui.HistoryAdapter

class HistoryFragment : Fragment() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var progressBar: ProgressBar // Biến này đã có
    private lateinit var historyAdapter: HistoryAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth
    private val historyList = mutableListOf<HistoryItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvHistory = view.findViewById(R.id.rvHistory)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)

        // ⭐ SỬA LỖI: THÊM DÒNG NÀY VÀO
        progressBar = view.findViewById(R.id.historyProgressBar)

        setupRecyclerView()
        loadHistory()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(historyList) { historyItem ->
            val intent = Intent(activity, MushroomDetailActivity::class.java)
            intent.putExtra("SCIENTIFIC_NAME", historyItem.scientificName)
            startActivity(intent)
        }
        rvHistory.adapter = historyAdapter
    }

    private fun loadHistory() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("HistoryFragment", "Không có User ID, không thể tải lịch sử")
            showEmptyState()
            return
        }
        showLoading(true)
        db.collection("users").document(userId).collection("history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { documents ->
                showLoading(false)
                if (documents.isEmpty) {
                    showEmptyState()
                } else {
                    showHistoryList()
                    historyList.clear()
                    val items = documents.toObjects(HistoryItem::class.java)
                    historyList.addAll(items)
                    historyAdapter.updateList(historyList)
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                showEmptyState()
                Log.e("HistoryFragment", "Lỗi khi tải lịch sử", exception)
            }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        // Sửa: Phải ẩn/hiện rvHistory ngược lại với progressBar
        rvHistory.visibility = if (isLoading) View.GONE else View.VISIBLE
        layoutEmpty.visibility = View.GONE
    }

    private fun showEmptyState() {
        progressBar.visibility = View.GONE
        rvHistory.visibility = View.GONE
        layoutEmpty.visibility = View.VISIBLE
    }

    private fun showHistoryList() {
        // Sửa: Khi hiển thị list, progressBar phải TẮT
        progressBar.visibility = View.GONE
        rvHistory.visibility = View.VISIBLE
        layoutEmpty.visibility = View.GONE
    }
}