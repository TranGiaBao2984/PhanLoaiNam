package org.tensorflow.lite.examples.classification

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class MushroomLibraryAdapter(
    private var mushroomList: List<MushroomInfo>,
    private val onItemClick: (MushroomInfo) -> Unit
) : RecyclerView.Adapter<MushroomLibraryAdapter.ViewHolder>() {

    private lateinit var context: Context

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCommonName: TextView = view.findViewById(R.id.tvCommonName)
        val tvScientificName: TextView = view.findViewById(R.id.tvScientificName)
        val tvToxicityTag: TextView = view.findViewById(R.id.tvToxicityText)
        val ivMushroomThumbnail: ImageView = view.findViewById(R.id.ivMushroomThumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_mushroom_library, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mushroom = mushroomList[position]

        holder.tvCommonName.text = mushroom.commonName
        holder.tvScientificName.text = mushroom.scientificName
        holder.tvToxicityTag.text = mushroom.toxicityLabel

        val toxicityColorRes = when (mushroom.toxicity) {
            "edible" -> R.color.md_theme_edible
            "toxic" -> R.color.md_theme_error
            else -> R.color.md_theme_tertiary
        }
        holder.tvToxicityTag.setTextColor(ContextCompat.getColor(context, toxicityColorRes))

        if (mushroom.sampleImage.isNotEmpty()) {
            val imagePath = mushroom.sampleImage.removePrefix("assets/")
            val bitmap = AssetImageLoader.loadBitmapFromAssets(context, imagePath)
            holder.ivMushroomThumbnail.setImageBitmap(bitmap)
        } else {
            holder.ivMushroomThumbnail.setImageDrawable(null)
        }

        holder.itemView.setOnClickListener { onItemClick(mushroom) }
    }

    override fun getItemCount(): Int = mushroomList.size

    fun updateList(newList: List<MushroomInfo>) {
        val diffCallback = MushroomDiffCallback(mushroomList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        mushroomList = newList
        diffResult.dispatchUpdatesTo(this)
    }

    private class MushroomDiffCallback(
        private val oldList: List<MushroomInfo>,
        private val newList: List<MushroomInfo>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem.scientificName == newItem.scientificName
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem == newItem
        }
    }
}
