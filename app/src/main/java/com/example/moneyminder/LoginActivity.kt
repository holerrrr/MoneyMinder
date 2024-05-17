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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val buttonLogin = findViewById<Button>(R.id.button_log)
        val editTextEmail = findViewById<EditText>(R.id.TextLogin)
        val editTextPassword = findViewById<EditText>(R.id.TextPassword)

        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(applicationContext, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        val textViewRegister = findViewById<TextView>(R.id.textViewRegister)
        textViewRegister.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser(email: String, password: String) {
        val client = OkHttpClient()

        val json = JSONObject()
        json.put("email", email)
        json.put("password", password)

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://192.168.1.214/moneyminder/login.php")
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Log.e("LoginActivity", "Login failed: ${e.message}")
                    Toast.makeText(applicationContext, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    Log.d("LoginActivity", "Server response: $responseBody")
                    if (response.isSuccessful) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            if (jsonResponse.getString("status") == "success") {
                                val userId = jsonResponse.getInt("user_id") // Получаем user ID
                                saveUserId(userId) // Сохраняем user ID в SharedPreferences
                                Toast.makeText(applicationContext, "Login successful", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                                startActivity(intent)
                            } else {
                                Toast.makeText(applicationContext, "Login failed: ${jsonResponse.getString("message")}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("LoginActivity", "Error parsing response: ${e.message}")
                            Toast.makeText(applicationContext, "Login failed: Invalid response from server", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("LoginActivity", "Login failed: $responseBody")
                        Toast.makeText(applicationContext, "Login failed: $responseBody", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun saveUserId(userId: Int) {
        val sharedPreferences = getSharedPreferences("MoneyMinderPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("user_id", userId)
        editor.apply()
    }
}
