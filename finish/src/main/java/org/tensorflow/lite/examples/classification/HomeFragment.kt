package org.tensorflow.lite.examples.classification

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import org.tensorflow.lite.examples.classification.ui.HistoryPreviewAdapter
import org.tensorflow.lite.examples.classification.ui.LibraryPreviewAdapter

class HomeFragment : Fragment() {

    private lateinit var tvUserName: TextView
    private lateinit var ivThemeToggle: ImageView
    private lateinit var etSearch: EditText
    private lateinit var cardScan: CardView
    private lateinit var tvHistorySeeAll: TextView
    private lateinit var tvLibrarySeeAll: TextView
    private lateinit var cardHistoryEmpty: CardView
    private lateinit var rvHistoryPreview: RecyclerView
    private lateinit var rvLibraryPreview: RecyclerView
    private lateinit var chipGroupFilters: ChipGroup

    private lateinit var mushroomHelper: MushroomDataHelper
    private lateinit var historyAdapter: HistoryPreviewAdapter
    private lateinit var libraryAdapter: LibraryPreviewAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth
    private var allMushrooms: List<MushroomInfo> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("HomeFragment", "onViewCreated() called")

        // Khởi tạo MushroomDataHelper
        mushroomHelper = MushroomDataHelper(requireContext())
        allMushrooms = mushroomHelper.getAllMushrooms().sortedBy { it.commonName }

        // Ánh xạ View
        tvUserName = view.findViewById(R.id.tvUserName)
        ivThemeToggle = view.findViewById(R.id.ivThemeToggle)
        etSearch = view.findViewById(R.id.etSearch)
        cardScan = view.findViewById(R.id.cardScan)
        tvHistorySeeAll = view.findViewById(R.id.tvHistorySeeAll)
        tvLibrarySeeAll = view.findViewById(R.id.tvLibrarySeeAll)
        cardHistoryEmpty = view.findViewById(R.id.cardHistoryEmpty)
        rvHistoryPreview = view.findViewById(R.id.rvHistoryPreview)
        rvLibraryPreview = view.findViewById(R.id.rvLibraryPreview)
        chipGroupFilters = view.findViewById(R.id.chipGroupFilters)

        // RecyclerView
        setupHistoryRecyclerView()
        setupLibraryRecyclerView()

        // Dữ liệu
        displayUserInfo()
        loadHistoryPreview()
        loadLibraryPreview()

        updateThemeIcon()

        // Listeners
        setupListeners()
    }

    private fun setupHistoryRecyclerView() {
        historyAdapter = HistoryPreviewAdapter { historyItem ->
            val intent = Intent(activity, MushroomDetailActivity::class.java)
            intent.putExtra("SCIENTIFIC_NAME", historyItem.scientificName)
            startActivity(intent)
        }
        rvHistoryPreview.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvHistoryPreview.adapter = historyAdapter
    }

    private fun setupLibraryRecyclerView() {
        libraryAdapter = LibraryPreviewAdapter { mushroomInfo ->
            val intent = Intent(activity, MushroomDetailActivity::class.java)
            intent.putExtra("SCIENTIFIC_NAME", mushroomInfo.scientificName)
            startActivity(intent)
        }
        rvLibraryPreview.layoutManager = GridLayoutManager(context, 2)
        rvLibraryPreview.adapter = libraryAdapter
    }

    private fun displayUserInfo() {
        auth.currentUser?.let { user ->
            tvUserName.text = user.displayName ?: "Người dùng!"
        }
    }

    private fun loadHistoryPreview() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    cardHistoryEmpty.visibility = View.VISIBLE
                    rvHistoryPreview.visibility = View.GONE
                } else {
                    cardHistoryEmpty.visibility = View.GONE
                    rvHistoryPreview.visibility = View.VISIBLE
                    val items = documents.toObjects(HistoryItem::class.java)
                    historyAdapter.updateList(items)
                }
            }
            .addOnFailureListener {
                cardHistoryEmpty.visibility = View.VISIBLE
                rvHistoryPreview.visibility = View.GONE
            }
    }

    private fun loadLibraryPreview(filter: String = "all") {
        val filteredList = when (filter) {
            "edible" -> allMushrooms.filter { it.toxicity == "edible" }
            "toxic" -> allMushrooms.filter { it.toxicity == "toxic" }
            "unknown" -> allMushrooms.filter { it.toxicity == "unknown" }
            else -> allMushrooms
        }
        libraryAdapter.updateList(filteredList.take(4))
    }

    /**
     * Cập nhật icon theme toggle dựa trên theme hiện tại
     * Logic: Hiển thị icon ngược lại với mode hiện tại
     * - Dark mode → hiển thị icon mặt trời (light)
     * - Light mode → hiển thị icon mặt trăng (dark)
     */
    private fun updateThemeIcon() {
        val isDarkMode = ThemeUtils.isDarkMode(requireContext())
        Log.d("HomeFragment", "updateThemeIcon() - isDarkMode: $isDarkMode")

        ivThemeToggle.setImageResource(
            if (isDarkMode) R.drawable.ic_light_mode // Đang tối → hiển thị icon sáng
            else R.drawable.ic_dark_mode // Đang sáng → hiển thị icon tối
        )
    }

    private fun setupListeners() {
        val homeActivity = activity as? HomeActivity

        // Nút chụp ảnh
        cardScan.setOnClickListener {
            startActivity(Intent(activity, MainActivity::class.java))
        }

        // Nút xem tất cả Lịch sử
        tvHistorySeeAll.setOnClickListener {
            homeActivity?.navigateToTab(R.id.nav_history_layout)
        }

        // Nút xem tất cả Thư viện → mở Library không có query
        tvLibrarySeeAll.setOnClickListener {
            homeActivity?.openMushroomLibraryWithQuery(null)
        }

        // Thanh tìm kiếm ở Home → mở Library với query
        // Thanh tìm kiếm ở Home → mở Library với query
        etSearch.setOnEditorActionListener { v, actionId, _ ->
            // Kiểm tra xem người dùng có nhấn nút "Search" (Tìm kiếm) trên bàn phím không
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {

                // Lấy nội dung text từ EditText
                val q = etSearch.text.toString().trim()

                if (q.isNotEmpty()) {
                    homeActivity?.openMushroomLibraryWithQuery(q)
                } else {
                    homeActivity?.openMushroomLibraryWithQuery(null)
                }

                // Ẩn bàn phím sau khi tìm kiếm
                val imm = context?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.hideSoftInputFromWindow(v.windowToken, 0)

                etSearch.clearFocus() // Xóa focus khỏi EditText
                true // Trả về true: sự kiện đã được xử lý
            } else {
                false // Trả về false: sự kiện chưa được xử lý
            }
        }

        // Bộ lọc Chip (chỉ ảnh hưởng preview ở Home)
        chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            // checkedIds là List<Int> chứa các ID của chips được chọn
            // Trong single selection mode, chỉ có 1 ID
            val checkedId = checkedIds.firstOrNull() ?: R.id.chipAll

            when (checkedId) {
                R.id.chipAll -> loadLibraryPreview("all")
                R.id.chipEdible -> loadLibraryPreview("edible")
                R.id.chipToxic -> loadLibraryPreview("toxic")
                R.id.chipUnknown -> loadLibraryPreview("unknown")
            }
        }

        // ⭐ NÚT ĐỔI THEME SÁNG / TỐI
        // QUAN TRỌNG: Clear listener cũ trước để tránh gọi nhiều lần
        ivThemeToggle.setOnClickListener(null)
        ivThemeToggle.setOnClickListener {
            Log.d("HomeFragment", "Theme toggle clicked!")
            // Toggle theme và tự động recreate Activity
            ThemeUtils.toggleTheme(requireContext())
            // Không cần update icon ở đây vì Activity sẽ recreate
            // và onViewCreated() sẽ gọi lại updateThemeIcon()
        }
    }
}