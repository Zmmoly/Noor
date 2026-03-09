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
import com.noor.recovery.data.AddictionTypes
import com.noor.recovery.data.Quotes
import com.noor.recovery.databinding.FragmentHomeBinding
import com.noor.recovery.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Random quote
        val (quoteText, quoteSource) = Quotes.random()
        binding.tvQuote.text = "\u201C$quoteText\u201D"
        binding.tvQuoteSource.text = "— $quoteSource"

        // Craving button
        binding.btnCraving.setOnClickListener {
            CravingBottomSheet().show(parentFragmentManager, "craving")
        }

        // Reset button
        binding.btnReset.setOnClickListener {
            (requireActivity() as MainActivity).confirmReset()
        }

        // Observe session
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.session.collect { session ->
                    if (session != null) {
                        val type = AddictionTypes.all.find { it.id == session.addictionTypeId }
                        binding.tvSubtitle.text = "منذ توقفت عن ${type?.nameAr ?: ""}"
                    }
                }
            }
        }

        // Observe timer
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.timer.collect { t ->
                    binding.tvDays.text    = t.days.toString()
                    binding.tvHours.text   = t.hours.toString()
                    binding.tvMinutes.text = t.minutes.toString()
                    binding.tvSeconds.text = t.seconds.toString()
                    binding.circularProgress.progress = (t.ringProgress * 100).toInt()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
