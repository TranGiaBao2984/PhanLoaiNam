package org.tensorflow.lite.examples.classification.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.tensorflow.lite.examples.classification.MushroomDataHelper
import org.tensorflow.lite.examples.classification.R
import org.tensorflow.lite.support.label.Category
import android.content.Intent
import org.tensorflow.lite.examples.classification.MushroomDetailActivity
class RecognitionAdapter(
    private val mushroomHelper: MushroomDataHelper
) : ListAdapter<Category, RecognitionAdapter.ViewHolder>(RecognitionDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvScientificName: TextView = view.findViewById(R.id.tvScientificName)
        val tvCommonName: TextView = view.findViewById(R.id.tvCommonName)
        val tvToxicity: TextView = view.findViewById(R.id.tvToxicity)
        val tvConfidence: TextView = view.findViewById(R.id.tvConfidence)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvHabitat: TextView = view.findViewById(R.id.tvHabitat)
        val tvToxicityNote: TextView = view.findViewById(R.id.tvToxicityNote)
        val tvSymptoms: TextView = view.findViewById(R.id.tvSymptoms)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recognition_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recognition = getItem(position)
        val scientificName = recognition.label
        val confidence = recognition.score

        val mushroomInfo = mushroomHelper.getMushroomInfo(scientificName)
        val context = holder.itemView.context

        holder.tvScientificName.text = scientificName
        holder.tvConfidence.text = context.getString(R.string.label_confidence, confidence * 100)

        mushroomInfo?.let { info ->
            holder.tvCommonName.text = info.commonName
            holder.tvToxicity.text = info.toxicityLabel
            holder.tvDescription.text = context.getString(R.string.label_description, info.description)
            holder.tvHabitat.text = context.getString(R.string.label_habitat, info.habitat)
            holder.tvToxicityNote.text = context.getString(R.string.label_toxicity_note, info.toxicityNote)
            holder.tvSymptoms.text = context.getString(R.string.label_symptoms, info.symptoms)

            val toxicityColor = when (info.toxicity) {
                "edible" -> "#4CAF50".toColorInt()
                "toxic" -> "#F44336".toColorInt()
                else -> "#FF9800".toColorInt()
            }
            holder.tvToxicity.setTextColor(toxicityColor)
        }
        holder.itemView.setOnClickListener {
            val intent = Intent(context, MushroomDetailActivity::class.java)
            intent.putExtra("SCIENTIFIC_NAME", scientificName)
            context.startActivity(intent)
        }
    }
}

private class RecognitionDiffCallback : DiffUtil.ItemCallback<Category>() {
    override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
        return oldItem.label == newItem.label
    }

    override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
        return oldItem.score == newItem.score
    }
}