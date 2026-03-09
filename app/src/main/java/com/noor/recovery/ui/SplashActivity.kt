package com.noor.recovery.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.noor.recovery.R
import com.noor.recovery.data.RecoveryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
            delay(1500)
            val repo = RecoveryRepository(applicationContext)
            val session = repo.sessionFlow.first()
            val intent = if (session != null) {
                Intent(this@SplashActivity, MainActivity::class.java)
            } else {
                Intent(this@SplashActivity, SetupActivity::class.java)
            }
            startActivity(intent)
            finish()
        }
    }
}
