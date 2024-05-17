package com.example.moneyminder

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class PieChartActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var spinnerCategories: Spinner

    private var allTransactions: List<Transaction> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pie_chart)

        pieChart = findViewById(R.id.pieChart)
        spinnerCategories = findViewById(R.id.spinnerCategories)

        val categories = arrayOf("All", "Transport", "Food", "Sport")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategories.adapter = adapter

        // Загрузка всех транзакций
        allTransactions = loadTransactionsFromDatabase()

        spinnerCategories.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                updatePieChart(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        })
    }

    private fun updatePieChart(selectedCategory: String) {
        val entries = when (selectedCategory) {
            "All" -> {
                // Пример данных для всех категорий
                val totalAmount = allTransactions.sumByDouble { it.amount }
                allTransactions.groupBy { it.category }
                    .map { entry -> PieEntry((entry.value.sumByDouble { it.amount } / totalAmount * 100).toFloat(), entry.key) }
            }
            else -> {
                // Пример данных для одной выбранной категории
                val selectedTransactions = allTransactions.filter { it.category == selectedCategory }
                val totalAmountForSelectedCategory = selectedTransactions.sumByDouble { it.amount }
                val totalAmountForAllCategories = allTransactions.sumByDouble { it.amount }
                val percentForSelectedCategory = (totalAmountForSelectedCategory / totalAmountForAllCategories * 100).toFloat()
                listOf(PieEntry(percentForSelectedCategory, selectedCategory)) +
                        allTransactions.groupBy { it.category }
                            .map { entry -> PieEntry((entry.value.sumByDouble { it.amount } / totalAmountForAllCategories * 100).toFloat(), entry.key) }
            }
        }

        val dataSet = PieDataSet(entries, "Categories")
        dataSet.colors = ColorTemplate.COLORFUL_COLORS.asList()

        val data = PieData(dataSet)

        pieChart.apply {
            this.data = data
            description.isEnabled = false
            setDrawEntryLabels(false)
            isRotationEnabled = true
            setHoleColor(android.R.color.transparent)
            centerText = "Expenses"
            setDrawCenterText(true)
            invalidate()
        }
    }

    private fun loadTransactionsFromDatabase(): List<Transaction> {
        // Просто для тестирования, создадим список транзакций для каждой категории
        return listOf(
            Transaction("Bus ticket", 15.0, "Transport"),
            Transaction("Gym membership", 30.0, "Sport"),
            Transaction("Lunch", 10.0, "Food"),
            Transaction("Dinner", 40.0, "Restaurants"),
            Transaction("T-shirt", 20.0, "Clothing"),
            Transaction("Book", 15.0, "Other")
        )
    }
}
