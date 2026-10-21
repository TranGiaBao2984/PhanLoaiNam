package org.tensorflow.lite.examples.classification.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.tensorflow.lite.examples.classification.AssetImageLoader
import org.tensorflow.lite.examples.classification.MushroomInfo
import org.tensorflow.lite.examples.classification.R

class LibraryPreviewAdapter(
    private val onItemClick: (MushroomInfo) -> Unit
) : RecyclerView.Adapter<LibraryPreviewAdapter.ViewHolder>() {

    private var mushroomList: List<MushroomInfo> = emptyList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivMushroom: ImageView = view.findViewById(R.id.ivLibraryMushroom)
        val tvScientificName: TextView = view.findViewById(R.id.tvLibraryScientificName)
        val tvCommonName: TextView = view.findViewById(R.id.tvLibraryCommonName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_library_preview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mushroomList[position]
        holder.tvScientificName.text = item.scientificName
        holder.tvCommonName.text = item.commonName

        if (item.sampleImage.isNotEmpty()) {
            val imagePath = item.sampleImage.removePrefix("assets/")
            val bitmap = AssetImageLoader.loadBitmapFromAssets(holder.itemView.context, imagePath)
            holder.ivMushroom.setImageBitmap(bitmap)
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = mushroomList.size

    fun updateList(newList: List<MushroomInfo>) {
        mushroomList = newList
        notifyDataSetChanged()
    }
}