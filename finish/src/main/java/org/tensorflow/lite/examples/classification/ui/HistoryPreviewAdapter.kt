package org.tensorflow.lite.examples.classification.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.tensorflow.lite.examples.classification.AssetImageLoader
import org.tensorflow.lite.examples.classification.HistoryItem
import org.tensorflow.lite.examples.classification.R

class HistoryPreviewAdapter(
    private val onItemClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryPreviewAdapter.ViewHolder>() {

    private var historyList: List<HistoryItem> = emptyList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivMushroom: ImageView = view.findViewById(R.id.ivHistoryMushroom)
        val tvScientificName: TextView = view.findViewById(R.id.tvHistoryScientificName)
        val tvCommonName: TextView = view.findViewById(R.id.tvHistoryCommonName)
        val tvToxicity: TextView = view.findViewById(R.id.tvHistoryToxicity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_preview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyList[position]
        holder.tvScientificName.text = item.scientificName
        holder.tvCommonName.text = item.commonName
        holder.tvToxicity.text = String.format("%.1f%% tin cậy", item.confidence * 100)

        if (item.sampleImage.isNotEmpty()) {
            val imagePath = item.sampleImage.removePrefix("assets/")
            val bitmap = AssetImageLoader.loadBitmapFromAssets(holder.itemView.context, imagePath)
            holder.ivMushroom.setImageBitmap(bitmap)
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = historyList.size

    fun updateList(newList: List<HistoryItem>) {
        historyList = newList
        notifyDataSetChanged()
    }
}