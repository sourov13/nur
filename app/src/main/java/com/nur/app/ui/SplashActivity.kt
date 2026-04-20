package com.nur.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nur.app.data.repository.FirebaseRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            delay(1200)
            val next = if (FirebaseRepository.isLoggedIn) MainActivity::class.java
                       else AuthActivity::class.java
            startActivity(Intent(this@SplashActivity, next))
            finish()
        }
    }
}
