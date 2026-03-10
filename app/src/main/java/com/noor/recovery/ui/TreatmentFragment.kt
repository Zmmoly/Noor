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
import com.noor.recovery.data.AddictionTypes
import com.noor.recovery.data.TreatmentPrograms
import com.noor.recovery.databinding.FragmentTreatmentBinding
import com.noor.recovery.ui.adapter.TreatmentAdapter
import com.noor.recovery.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class TreatmentFragment : Fragment() {

    private var _binding: FragmentTreatmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTreatmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.session.collect { session ->
                    if (session != null) {
                        val type    = AddictionTypes.all.find { it.id == session.addictionTypeId }
                        val program = TreatmentPrograms.getProgram(session.addictionTypeId)

                        binding.tvTreatmentTitle.text =
                            "برنامج الإقلاع عن ${type?.nameAr ?: ""}"
                        binding.tvTreatmentEmoji.text  = type?.emoji ?: "💪"

                        binding.rvTreatment.layoutManager = LinearLayoutManager(requireContext())
                        binding.rvTreatment.adapter       = TreatmentAdapter(program)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
