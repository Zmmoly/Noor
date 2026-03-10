package com.noor.recovery.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noor.recovery.data.TreatmentStep
import com.noor.recovery.databinding.ItemTreatmentWeekBinding

class TreatmentAdapter(private val steps: List<TreatmentStep>) :
    RecyclerView.Adapter<TreatmentAdapter.VH>() {

    inner class VH(val binding: ItemTreatmentWeekBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemTreatmentWeekBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = steps.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val step = steps[position]
        with(holder.binding) {
            tvWeek.text  = step.week
            tvTitle.text = step.titleAr
            rvSteps.layoutManager = LinearLayoutManager(root.context)
            rvSteps.adapter       = StepItemAdapter(step.steps)
        }
    }
}
