package com.nur.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nur.app.data.repository.FirebaseRepository
import com.nur.app.databinding.ActivityAuthBinding
import com.nur.app.util.hide
import com.nur.app.util.show
import com.nur.app.util.toast
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var b: ActivityAuthBinding
    private var isSignUp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.btnAction.setOnClickListener { handleAction() }
        b.tvToggle.setOnClickListener  { isSignUp = !isSignUp; updateUI() }
        updateUI()
    }

    private fun updateUI() {
        if (isSignUp) {
            b.tilName.show(); b.btnAction.text = "Create Account"
            b.tvToggle.text = "Already have an account? Sign In"
            b.tvTitle.text  = "Join Nūr"
        } else {
            b.tilName.hide(); b.btnAction.text = "Sign In"
            b.tvToggle.text = "Don't have an account? Sign Up"
            b.tvTitle.text  = "Welcome Back"
        }
    }

    private fun handleAction() {
        val email    = b.etEmail.text.toString().trim()
        val password = b.etPassword.text.toString().trim()
        if (email.isEmpty() || password.isEmpty()) { toast("Please fill in all fields"); return }
        if (password.length < 6) { toast("Password must be at least 6 characters"); return }

        b.progressBar.show(); b.btnAction.isEnabled = false

        lifecycleScope.launch {
            val result = if (isSignUp) {
                val name = b.etName.text.toString().trim().ifEmpty { "Friend" }
                FirebaseRepository.signUp(email, password, name)
            } else {
                FirebaseRepository.signIn(email, password)
            }
            b.progressBar.hide(); b.btnAction.isEnabled = true
            result.fold(
                onSuccess = { startActivity(Intent(this@AuthActivity, MainActivity::class.java)); finish() },
                onFailure = { toast(it.localizedMessage ?: "Authentication failed") }
            )
        }
    }
}
