package org.tensorflow.lite.examples.classification

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout

class MushroomLibraryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SEARCH_QUERY = "EXTRA_SEARCH_QUERY"
    }

    private lateinit var mushroomHelper: MushroomDataHelper
    private lateinit var adapter: MushroomLibraryAdapter
    private var allMushrooms: List<MushroomInfo> = emptyList()

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView

    private var currentSearchQuery: String = ""
    private var currentSelectedTab: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mushroom_library)

        supportActionBar?.hide()

        // Nút Back
        val backButton: ImageButton = findViewById(R.id.ivBackButton)
        backButton.setOnClickListener {
            finish()
        }

        mushroomHelper = MushroomDataHelper(this)
        allMushrooms = mushroomHelper.getAllMushrooms().sortedBy { it.commonName }

        recyclerView = findViewById(R.id.rvMushroomLibrary)
        adapter = MushroomLibraryAdapter(allMushrooms) { mushroomInfo ->
            val intent = Intent(this, MushroomDetailActivity::class.java)
            intent.putExtra("SCIENTIFIC_NAME", mushroomInfo.scientificName)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        currentSelectedTab = getString(R.string.tab_all)

        searchView = findViewById(R.id.searchView)
        tabLayout = findViewById(R.id.tabLayout)

        setupSearchView()
        setupTabs()

        // Lấy query ban đầu từ Intent (Home)
        val initialQuery =
            intent.getStringExtra(EXTRA_SEARCH_QUERY)
                ?: intent.getStringExtra("SEARCH_QUERY") // fallback cho code cũ
                ?: intent.getStringExtra("searchQuery")  // fallback thêm
                ?: ""

        if (initialQuery.isNotEmpty()) {
            currentSearchQuery = initialQuery
            searchView.isIconified = false
            searchView.setQuery(initialQuery, false)
        }

        performFiltering()
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchQuery = query.orEmpty()
                performFiltering()
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText.orEmpty()
                performFiltering()
                return true
            }
        })
    }

    private fun setupTabs() {
        val tabAll = tabLayout.newTab().setText(getString(R.string.tab_all))
        val tabEdible = tabLayout.newTab().setText(getString(R.string.tab_edible))
        val tabToxic = tabLayout.newTab().setText(getString(R.string.tab_toxic))
        val tabUnknown = tabLayout.newTab().setText(getString(R.string.tab_unknown))

        tabLayout.addTab(tabAll)
        tabLayout.addTab(tabEdible)
        tabLayout.addTab(tabToxic)
        tabLayout.addTab(tabUnknown)

        updateTabColors(tabAll, true)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    updateTabColors(it, true)
                    currentSelectedTab = it.text?.toString() ?: getString(R.string.tab_all)
                    performFiltering()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.let { updateTabColors(it, false) }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                tab?.let {
                    currentSelectedTab = it.text?.toString() ?: getString(R.string.tab_all)
                    performFiltering()
                }
            }
        })
    }

    private fun updateTabColors(tab: TabLayout.Tab, isSelected: Boolean) {
        if (isSelected) {
            tab.view.background =
                ContextCompat.getDrawable(this, R.drawable.tab_background_selected)
        } else {
            tab.view.background =
                ContextCompat.getDrawable(this, R.drawable.tab_background_unselected)
        }
    }

    private fun performFiltering() {
        val tabFilteredList = when (currentSelectedTab) {
            getString(R.string.tab_edible) -> allMushrooms.filter { it.toxicity == "edible" }
            getString(R.string.tab_toxic) -> allMushrooms.filter { it.toxicity == "toxic" }
            getString(R.string.tab_unknown) -> allMushrooms.filter { it.toxicity == "unknown" }
            else -> allMushrooms
        }

        val finalFilteredList = if (currentSearchQuery.isBlank()) {
            tabFilteredList
        } else {
            tabFilteredList.filter { mushroom ->
                mushroom.commonName.contains(currentSearchQuery, ignoreCase = true) ||
                        mushroom.scientificName.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        adapter.updateList(finalFilteredList)
        recyclerView.scrollToPosition(0)
    }
}
