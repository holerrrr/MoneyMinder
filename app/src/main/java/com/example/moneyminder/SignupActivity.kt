package com.example.moneyminder

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

class SignupActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var password1EditText: EditText
    private lateinit var password2EditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        nameEditText = findViewById(R.id.TextName)
        emailEditText = findViewById(R.id.TextEmail)
        password1EditText = findViewById(R.id.TextPassword1)
        password2EditText = findViewById(R.id.TextPassword2)

        val textViewLogin = findViewById<TextView>(R.id.textViewLogin)
        val signupButton = findViewById<Button>(R.id.button_signup)

        signupButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password1 = password1EditText.text.toString()
            val password2 = password2EditText.text.toString()
            if (name.isEmpty() || email.isEmpty() || password1.isEmpty() || password2.isEmpty()) {
                // Если какое-то из полей пустое, выводим сообщение
                Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show()
            } else if (password1 != password2) {
                // Если пароли не совпадают, выводим сообщение
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                // Если все поля заполнены и пароли совпадают, можно выполнить регистрацию
                registerUser(name, email, password1)
            }
        }

        textViewLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun registerUser(name: String, email: String, password: String) {
        val client = OkHttpClient()

        val json = JSONObject().apply {
            put("name", name)
            put("email", email)
            put("password", password)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://192.168.1.217/moneyminder/registration.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(applicationContext, responseBody, Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@SignupActivity, DashboardActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(applicationContext, "Registration failed: $responseBody", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
