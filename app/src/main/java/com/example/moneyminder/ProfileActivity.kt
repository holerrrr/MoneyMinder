package com.example.moneyminder

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

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

        // Загрузка текущего бюджета из SharedPreferences
        val currentBudget = sharedPreferences.getFloat("budget", 1000.0f)
        editTextBudget.setText(currentBudget.toString())

        buttonSaveBudget.setOnClickListener {
            val newBudget = editTextBudget.text.toString().toDoubleOrNull()
            if (newBudget != null) {
                val editor = sharedPreferences.edit()
                editor.putFloat("budget", newBudget.toFloat())
                editor.apply()
                Toast.makeText(this, "Budget saved", Toast.LENGTH_SHORT).show()
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
}
