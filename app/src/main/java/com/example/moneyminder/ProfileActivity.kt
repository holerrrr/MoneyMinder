package com.example.moneyminder

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ProfileActivity : AppCompatActivity() {

    private lateinit var editTextBudget: EditText
    private lateinit var buttonSaveBudget: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        editTextBudget = findViewById(R.id.editTextBudget)
        buttonSaveBudget = findViewById(R.id.buttonSaveBudget)
        sharedPreferences = getSharedPreferences("MoneyMinderPrefs", MODE_PRIVATE)

        // Загрузка текущего бюджета из базы данных
        val userId = sharedPreferences.getInt("user_id", -1)
        loadBudgetFromDatabase(userId)

        buttonSaveBudget.setOnClickListener {
            val newBudget = editTextBudget.text.toString().toDoubleOrNull()
            if (newBudget != null) {
                updateBudgetInDatabase(userId, newBudget)
            } else {
                Toast.makeText(this, "Invalid budget amount", Toast.LENGTH_SHORT).show()
            }
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_dashboard -> {
                    val intent = Intent(this, DashboardActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_diagram -> {
                    // Переход на PieChartActivity
                    val intent = Intent(this, PieChartActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_add_transaction -> {
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.putExtra("openAddTransaction", true)
                    startActivity(intent)
                    true
                }
                R.id.menu_profile -> {
                    // Уже находимся на этом экране, ничего не делаем
                    true
                }
                else -> false
            }
        }

        // Установка текущего элемента панели навигации как "Profile"
        bottomNavigationView.selectedItemId = R.id.menu_profile
    }

    private fun loadBudgetFromDatabase(userId: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                val json = JSONObject().apply {
                    put("user_id", userId)
                }

                val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("http://192.168.1.250/moneyminder/get_balance.php")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    val jsonObject = JSONObject(responseBody)
                    if (jsonObject.has("balance")) {
                        val balance = jsonObject.getDouble("balance")
                        runOnUiThread {
                            editTextBudget.setText(balance.toString())
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@ProfileActivity, "Failed to load budget", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@ProfileActivity, "Failed to load budget: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Error loading budget: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateBudgetInDatabase(userId: Int, newBudget: Double) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                val json = JSONObject().apply {
                    put("user_id", userId)
                    put("balance", newBudget)
                }

                val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("http://192.168.1.250/moneyminder/update_balance.php")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@ProfileActivity, "Budget updated successfully", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@ProfileActivity, "Failed to update budget: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Error updating budget: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
