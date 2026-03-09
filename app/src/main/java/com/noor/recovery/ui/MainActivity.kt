package com.noor.recovery.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.noor.recovery.R
import com.noor.recovery.databinding.ActivityMainBinding
import com.noor.recovery.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setupBottomNav()
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home       -> { loadFragment(HomeFragment()); true }
                R.id.nav_milestones -> { loadFragment(MilestonesFragment()); true }
                R.id.nav_tips       -> { loadFragment(TipsFragment()); true }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle("إعادة البدء")
            .setMessage("هل أنت متأكد؟ سيتم مسح تقدمك الحالي.")
            .setPositiveButton("نعم") { _, _ ->
                viewModel.resetSession()
                startActivity(Intent(this, SetupActivity::class.java))
                finish()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}
