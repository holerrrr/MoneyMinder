package com.example.moneyminder

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class PieChartActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var spinnerCategories: Spinner

    private var allTransactions: List<Transaction> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pie_chart)

        pieChart = findViewById(R.id.pieChart)
        spinnerCategories = findViewById(R.id.spinnerCategories)

        val categories = arrayOf("All", "Transport", "Sport", "Food", "Restaurants", "Clothing", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategories.adapter = adapter

        // Загрузка всех транзакций из базы данных
        val sharedPreferences = getSharedPreferences("MoneyMinderPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)
        loadTransactionsFromDatabase(userId)

        spinnerCategories.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                updatePieChartForCategory(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        })

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_dashboard -> {
                    val intent = Intent(this, DashboardActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_diagram -> {
                    // Already on PieChartActivity, do nothing
                    true
                }
                R.id.menu_add_transaction -> {
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.putExtra("openAddTransaction", true)
                    startActivity(intent)
                    true
                }
                R.id.menu_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Set the selected item of bottom navigation view to "Diagram"
        bottomNavigationView.selectedItemId = R.id.menu_diagram
    }

    private fun updatePieChart(entries: List<PieEntry>) {
        val dataSet = PieDataSet(entries, "Categories")
        dataSet.colors = ColorTemplate.COLORFUL_COLORS.asList()
        dataSet.valueTextSize = 16f // Увеличиваем размер текста значений
        dataSet.valueLineColor = Color.BLACK // Устанавливаем цвет линий значений
        dataSet.valueLinePart1Length = 0.5f // Устанавливаем длину первой части линий значений
        dataSet.valueLinePart2Length = 0.5f // Устанавливаем длину второй части линий значений
        dataSet.valueLinePart1OffsetPercentage = 80f // Устанавливаем отступ первой части линий значений
        dataSet.valueFormatter = PercentFormatter() // Форматируем значения как проценты

        val data = PieData(dataSet)
        data.setValueTextSize(16f) // Увеличиваем размер текста данных

        pieChart.apply {
            this.data = data
            description.isEnabled = false
            setDrawEntryLabels(false)
            isRotationEnabled = true
            setHoleColor(android.R.color.transparent)
            centerText = "Expenses"
            setDrawCenterText(true)
            setCenterTextColor(Color.WHITE)
            setCenterTextSize(20f) // Увеличиваем размер текста в центре
            legend.isEnabled = true
            legend.apply {
                isEnabled = true
                textSize = 20f
                textColor = Color.WHITE
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                form = Legend.LegendForm.CIRCLE
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                xEntrySpace = 7f
                yEntrySpace = 0f
                yOffset = 10f
                // Разрешаем перенос текста
                isWordWrapEnabled = true
            }
            invalidate()
        }
    }

    private fun updatePieChartForCategory(selectedCategory: String) {
        val entries = mutableListOf<PieEntry>()
        val totalAmountForAllCategories = allTransactions.sumByDouble { it.amount }

        if (selectedCategory == "All") {
            val groupedTransactions = allTransactions.groupBy { it.category }
            groupedTransactions.forEach { (category, transactions) ->
                val totalAmount = transactions.sumByDouble { it.amount }
                entries.add(PieEntry(totalAmount.toFloat(), category))
            }
        } else {
            val selectedTransactions = allTransactions.filter { it.category == selectedCategory }
            val totalAmountForSelectedCategory = selectedTransactions.sumByDouble { it.amount }
            entries.add(PieEntry(totalAmountForSelectedCategory.toFloat(), selectedCategory))
            entries.add(PieEntry((totalAmountForAllCategories - totalAmountForSelectedCategory).toFloat(), "Other Categories"))
        }

        updatePieChart(entries)
    }

    private fun loadTransactionsFromDatabase(userId: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                val json = JSONObject().apply {
                    put("user_id", userId)
                }

                val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("http://192.168.1.250/moneyminder/get_expenses.php")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    val jsonArray = JSONArray(responseBody)
                    val transactionsList = mutableListOf<Transaction>()
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val id = jsonObject.getInt("Expenses_ID")
                        val description = jsonObject.getString("description")
                        val amount = jsonObject.getDouble("Sum")
                        val category = jsonObject.getString("category")
                        transactionsList.add(Transaction(id, description, amount, category))
                    }
                    allTransactions = transactionsList
                    runOnUiThread {
                        updatePieChartForCategory("All")
                    }
                } else {
                    Log.e("PieChartActivity", "Failed to load transactions: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("PieChartActivity", "Error loading transactions: ${e.message}")
            }
        }
    }
}
