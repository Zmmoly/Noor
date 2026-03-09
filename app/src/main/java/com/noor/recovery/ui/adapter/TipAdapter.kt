package com.noor.recovery.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noor.recovery.databinding.ItemTipBinding

class TipAdapter(private val tips: List<String>) : RecyclerView.Adapter<TipAdapter.VH>() {

    inner class VH(val binding: ItemTipBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemTipBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = tips.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.binding.tvNumber.text = (position + 1).toString()
        holder.binding.tvTip.text    = tips[position]
    }
}
