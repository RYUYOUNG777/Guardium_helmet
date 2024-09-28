package com.example.hel_ver2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DistanceAdapter(private val distances: List<Int>) : RecyclerView.Adapter<DistanceAdapter.DistanceViewHolder>() {

    class DistanceViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val distanceTextView: TextView = view.findViewById(R.id.distance_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DistanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_distance, parent, false)
        return DistanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DistanceViewHolder, position: Int) {
        holder.distanceTextView.text = "${distances[position]} cm"
    }

    override fun getItemCount() = distances.size
}
