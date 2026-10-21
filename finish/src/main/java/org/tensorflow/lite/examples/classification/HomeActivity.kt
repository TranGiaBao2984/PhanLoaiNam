// HomeActivity.kt
package org.tensorflow.lite.examples.classification

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.util.Date

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val homeFragment = HomeFragment()
    private val historyFragment = HistoryFragment()
    private val profileFragment = ProfileFragment()

    private lateinit var navHomeLayout: LinearLayout
    private lateinit var navLibraryLayout: LinearLayout
    private lateinit var navHistoryLayout: LinearLayout
    private lateinit var navProfileLayout: LinearLayout

    private var currentSelectedNav = R.id.nav_home_layout

    override fun onCreate(savedInstanceState: Bundle?) {
        // Áp dụng trạng thái sáng/tối đã lưu
        ThemeUtils.applySavedTheme(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        updateLoginTime()

        navHomeLayout = findViewById(R.id.nav_home_layout)
        navLibraryLayout = findViewById(R.id.nav_library_layout)
        navHistoryLayout = findViewById(R.id.nav_history_layout)
        navProfileLayout = findViewById(R.id.nav_profile_layout)
        val fabCamera: FloatingActionButton = findViewById(R.id.fabCamera)

        loadFragment(homeFragment)
        selectNavItem(R.id.nav_home_layout)

        navHomeLayout.setOnClickListener {
            loadFragment(homeFragment)
            selectNavItem(R.id.nav_home_layout)
        }

        navLibraryLayout.setOnClickListener {
            openMushroomLibraryWithQuery(null)
        }

        navHistoryLayout.setOnClickListener {
            loadFragment(this.historyFragment)
            selectNavItem(R.id.nav_history_layout)
        }

        navProfileLayout.setOnClickListener {
            loadFragment(profileFragment)
            selectNavItem(R.id.nav_profile_layout)
        }

        fabCamera.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    fun openMushroomLibraryWithQuery(query: String?) {
        val intent = Intent(this, MushroomLibraryActivity::class.java)
        intent.putExtra(MushroomLibraryActivity.EXTRA_SEARCH_QUERY, query.orEmpty())
        startActivity(intent)
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    private fun selectNavItem(selectedId: Int) {
        currentSelectedNav = selectedId

        setNavItemState(navHomeLayout, R.id.nav_home_icon, R.id.nav_home_text, false)
        setNavItemState(navLibraryLayout, R.id.nav_library_icon, R.id.nav_library_text, false)
        setNavItemState(navHistoryLayout, R.id.nav_history_icon, R.id.nav_history_text, false)
        setNavItemState(navProfileLayout, R.id.nav_profile_icon, R.id.nav_profile_text, false)

        when (selectedId) {
            R.id.nav_home_layout -> setNavItemState(
                navHomeLayout,
                R.id.nav_home_icon,
                R.id.nav_home_text,
                true
            )
            R.id.nav_library_layout -> setNavItemState(
                navLibraryLayout,
                R.id.nav_library_icon,
                R.id.nav_library_text,
                true
            )
            R.id.nav_history_layout -> setNavItemState(
                navHistoryLayout,
                R.id.nav_history_icon,
                R.id.nav_history_text,
                true
            )
            R.id.nav_profile_layout -> setNavItemState(
                navProfileLayout,
                R.id.nav_profile_icon,
                R.id.nav_profile_text,
                true
            )
        }
    }

    private fun setNavItemState(
        layout: LinearLayout,
        iconId: Int,
        textId: Int,
        isSelected: Boolean
    ) {
        val icon = layout.findViewById<ImageView>(iconId)
        val text = layout.findViewById<TextView>(textId)

        val color = if (isSelected) {
            ContextCompat.getColor(this, android.R.color.holo_orange_dark)
        } else {
            ContextCompat.getColor(this, android.R.color.darker_gray)
        }

        icon.setColorFilter(color)
        text.setTextColor(color)
    }

    fun navigateToTab(navId: Int) {
        when (navId) {
            R.id.nav_home_layout -> {
                loadFragment(homeFragment)
                selectNavItem(R.id.nav_home_layout)
            }
            R.id.nav_history_layout -> {
                loadFragment(historyFragment)
                selectNavItem(R.id.nav_history_layout)
            }
            R.id.nav_profile_layout -> {
                loadFragment(profileFragment)
                selectNavItem(R.id.nav_profile_layout)
            }
        }
    }

    private fun updateLoginTime() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val currentCount = document.getLong("loginCount") ?: 0
                userRef.update(
                    mapOf(
                        "lastLoginAt" to Date(),
                        "loginCount" to currentCount + 1
                    )
                )
            } else {
                val user = auth.currentUser!!
                val newUserProfile = hashMapOf(
                    "userId" to userId,
                    "displayName" to user.displayName,
                    "email" to user.email,
                    "photoUrl" to user.photoUrl.toString(),
                    "createdAt" to Date(),
                    "lastLoginAt" to Date(),
                    "loginCount" to 1
                )
                userRef.set(newUserProfile)
            }
        }
    }
}
