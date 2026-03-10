package com.noor.recovery.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.noor.recovery.databinding.ActivitySetupBinding
import com.noor.recovery.data.AddictionTypes
import com.noor.recovery.viewmodel.MainViewModel

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private lateinit var viewModel: MainViewModel
    private var selectedTypeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupAddictionGrid()
        binding.btnStart.setOnClickListener {
            val id = selectedTypeId
            if (id == null) {
                Toast.makeText(this, "اختر نوع الإدمان أولاً", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.startSession(id)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setupAddictionGrid() {
        val cards = listOf(
            binding.cardSmoking  to AddictionTypes.all[0].id,
            binding.cardAlcohol  to AddictionTypes.all[1].id,
            binding.cardGaming   to AddictionTypes.all[2].id,
            binding.cardSocial   to AddictionTypes.all[3].id,
            binding.cardShopping to AddictionTypes.all[4].id,
            binding.cardPorn     to AddictionTypes.all[5].id,
            binding.cardGambling to AddictionTypes.all[6].id
        )
        cards.forEach { (card, typeId) ->
            card.setOnClickListener {
                selectedTypeId = typeId
                cards.forEach { (c, _) -> c.isSelected = false }
                card.isSelected = true
            }
        }
    }
}
