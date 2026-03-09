package com.noor.recovery.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.noor.recovery.R
import com.noor.recovery.data.Milestone
import com.noor.recovery.databinding.ItemMilestoneBinding

class MilestoneAdapter(
    private val milestones: List<Milestone>,
    private var completed: Set<String>
) : RecyclerView.Adapter<MilestoneAdapter.VH>() {

    inner class VH(val binding: ItemMilestoneBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemMilestoneBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = milestones.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = milestones[position]
        val isDone = completed.contains(m.id)
        with(holder.binding) {
            tvEmoji.text    = m.emoji
            tvTitle.text    = m.titleAr
            tvSubtitle.text = m.subtitleAr
            tvCheck.text    = if (isDone) "✓" else "○"

            val bg = if (isDone) R.drawable.bg_milestone_done else R.drawable.bg_milestone_locked
            root.background = ContextCompat.getDrawable(root.context, bg)
            tvEmoji.alpha   = if (isDone) 1f else 0.35f
            tvTitle.alpha   = if (isDone) 1f else 0.4f
            tvCheck.setTextColor(
                ContextCompat.getColor(root.context,
                    if (isDone) R.color.teal else R.color.textSecondary)
            )
        }
    }

    fun updateCompleted(newCompleted: Set<String>) {
        completed = newCompleted
        notifyDataSetChanged()
    }
}
