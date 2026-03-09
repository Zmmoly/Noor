package com.noor.recovery.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.noor.recovery.data.Milestones
import com.noor.recovery.databinding.FragmentMilestonesBinding
import com.noor.recovery.ui.adapter.MilestoneAdapter
import com.noor.recovery.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MilestonesFragment : Fragment() {

    private var _binding: FragmentMilestonesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: MilestoneAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMilestonesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MilestoneAdapter(Milestones.all, emptySet())
        binding.rvMilestones.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMilestones.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.completedMilestones.collect { completed ->
                    adapter.updateCompleted(completed)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
