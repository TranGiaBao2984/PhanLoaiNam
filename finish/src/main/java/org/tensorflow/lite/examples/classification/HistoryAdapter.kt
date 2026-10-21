package org.tensorflow.lite.examples.classification.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.tensorflow.lite.examples.classification.AssetImageLoader
import org.tensorflow.lite.examples.classification.HistoryItem
import org.tensorflow.lite.examples.classification.R
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(
    private var historyList: List<HistoryItem>,
    // ⭐ SỬA: Thêm lambda click listener
    private val onItemClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private lateinit var context: Context
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    // ⭐ SỬA: Thêm tvHistoryConfidence
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivMushroom: ImageView = view.findViewById(R.id.ivHistoryMushroom)
        val tvCommonName: TextView = view.findViewById(R.id.tvHistoryCommonName)
        val tvScientificName: TextView = view.findViewById(R.id.tvHistoryScientificName)
        val tvTimestamp: TextView = view.findViewById(R.id.tvHistoryTimestamp)
        val tvHistoryConfidence: TextView = view.findViewById(R.id.tvHistoryConfidence) // ⭐ Thêm
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyList[position]

        holder.tvCommonName.text = item.commonName.ifEmpty { item.scientificName }
        holder.tvScientificName.text = item.scientificName

        item.timestamp?.let {
            holder.tvTimestamp.text = dateFormatter.format(it)
        }

        // ⭐ THÊM: Set text cho confidence (định dạng %)
        val confidenceText = String.format(Locale.US, "%.1f%% tin cậy", item.confidence * 100)
        holder.tvHistoryConfidence.text = confidenceText

        // Tải ảnh (giữ nguyên)
        if (item.sampleImage.isNotEmpty()) {
            val imagePath = item.sampleImage.removePrefix("assets/")
            val bitmap = AssetImageLoader.loadBitmapFromAssets(context, imagePath)
            holder.ivMushroom.setImageBitmap(bitmap)
        } else {
            holder.ivMushroom.setImageResource(R.drawable.ic_launcher_foreground)
        }

        // ⭐ THÊM: Set click listener
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = historyList.size

    fun updateList(newList: List<HistoryItem>) {
        historyList = newList
        notifyDataSetChanged()
    }
}