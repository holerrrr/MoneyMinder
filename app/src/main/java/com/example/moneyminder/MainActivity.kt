package com.example.moneyminder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val loginButton = findViewById<Button>(R.id.button_login)
        val regButton = findViewById<Button>(R.id.button_reg)

        loginButton.setOnClickListener {
            loginButton.setBackgroundResource(R.drawable.background_button_pressed)
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        regButton.setOnClickListener {
            // Створюємо інтент для переходу на активність activity_login
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)

        }

    }
}