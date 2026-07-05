package com.pcontrol.parent.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pcontrol.parent.R
import com.pcontrol.parent.network.ApiClient
import com.pcontrol.parent.network.AuthManager
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private var isSignupMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if (AuthManager.isLoggedIn(this)) {
            goToDashboard()
            return
        }

        nameInput = findViewById(R.id.nameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        val actionButton = findViewById<Button>(R.id.actionButton)
        val toggleModeText = findViewById<TextView>(R.id.toggleModeText)

        updateModeUI(actionButton, toggleModeText)

        toggleModeText.setOnClickListener {
            isSignupMode = !isSignupMode
            updateModeUI(actionButton, toggleModeText)
        }

        actionButton.setOnClickListener {
            if (isSignupMode) signup() else login()
        }
    }

    private fun updateModeUI(actionButton: Button, toggleModeText: TextView) {
        nameInput.visibility = if (isSignupMode) android.view.View.VISIBLE else android.view.View.GONE
        actionButton.text = if (isSignupMode) "Sign Up" else "Log In"
        toggleModeText.text = if (isSignupMode) "Already have an account? Log In" else "New here? Sign Up"
    }

    private fun signup() {
        val json = JSONObject().apply {
            put("name", nameInput.text.toString())
            put("email", emailInput.text.toString())
            put("password", passwordInput.text.toString())
        }
        ApiClient.post("/api/auth/signup", json) { success, response ->
            handleAuthResponse(success, response)
        }
    }

    private fun login() {
        val json = JSONObject().apply {
            put("email", emailInput.text.toString())
            put("password", passwordInput.text.toString())
        }
        ApiClient.post("/api/auth/login", json) { success, response ->
            handleAuthResponse(success, response)
        }
    }

    private fun handleAuthResponse(success: Boolean, response: String?) {
        runOnUiThread {
            if (success && response != null) {
                val obj = JSONObject(response)
                AuthManager.saveToken(this, obj.getString("token"))
                goToDashboard()
            } else {
                Toast.makeText(this, "Failed - check details and try again", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}
