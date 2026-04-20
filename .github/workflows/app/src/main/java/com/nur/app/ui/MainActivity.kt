package com.nur.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.messaging.FirebaseMessaging
import com.nur.app.R
import com.nur.app.data.repository.FirebaseRepository
import com.nur.app.databinding.ActivityMainBinding
import com.nur.app.ui.admin.AdminActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Guard: if somehow not logged in, go back to auth
        if (!FirebaseRepository.isLoggedIn) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Setup navigation
        try {
            val navHost = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            b.bottomNav.setupWithNavController(navHost.navController)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // FCM subscription (non-fatal if fails)
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                CoroutineScope(Dispatchers.IO).launch {
                    try { FirebaseRepository.updateFcmToken(token) } catch (e: Exception) { e.printStackTrace() }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Check admin status (non-fatal)
        b.fabAdmin.visibility = android.view.View.GONE
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val user = FirebaseRepository.getCurrentUser()
                if (user?.isAdmin == true) {
                    b.fabAdmin.visibility = android.view.View.VISIBLE
                    b.fabAdmin.setOnClickListener {
                        startActivity(Intent(this@MainActivity, AdminActivity::class.java))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
