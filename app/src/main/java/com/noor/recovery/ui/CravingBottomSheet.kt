package com.noor.recovery.ui

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.noor.recovery.databinding.BottomSheetCravingBinding

class CravingBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCravingBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())
    private var inhale = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetCravingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startBreathing()
        binding.btnClose.setOnClickListener { dismiss() }
    }

    private fun startBreathing() {
        val breathRunnable = object : Runnable {
            override fun run() {
                if (_binding == null) return
                inhale = !inhale
                binding.tvBreath.text = if (inhale) "شهيق" else "زفير"

                val scale = if (inhale) 1.3f else 1.0f
                ObjectAnimator.ofPropertyValuesHolder(
                    binding.breathCircle,
                    android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, scale),
                    android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, scale)
                ).apply {
                    duration = 2000
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
                handler.postDelayed(this, 2000)
            }
        }
        handler.post(breathRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}
