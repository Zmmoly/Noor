package com.noor.recovery.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noor.recovery.databinding.ItemStepBinding

class StepItemAdapter(private val items: List<String>) :
    RecyclerView.Adapter<StepItemAdapter.VH>() {

    inner class VH(val binding: ItemStepBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemStepBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.binding.tvStep.text = "• ${items[position]}"
    }
}

