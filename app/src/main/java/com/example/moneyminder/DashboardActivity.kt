package com.example.moneyminder

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog

class DashboardActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var budgetTextView: TextView
    private lateinit var totalTextView: TextView
    private var budget: Double = 1000.0 // Начальный бюджет
    private val transactions = mutableListOf<Transaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        listView = findViewById(R.id.listView)
        budgetTextView = findViewById(R.id.textViewBudget)
        totalTextView = findViewById(R.id.textViewTotal)
        updateBudgetDisplay()

        // Загрузка текущего бюджета из SharedPreferences
        val sharedPreferences = getSharedPreferences("MoneyMinderPrefs", MODE_PRIVATE)
        budget = sharedPreferences.getFloat("budget", 1000.0f).toDouble()
        updateBudgetDisplay()

        // Загрузка данных о финансовых транзакциях из базы данных или другого источника данных
        transactions.addAll(loadTransactionsFromDatabase())

        // Создание массива категорий для фильтрации данных
        val categories = arrayOf("All", "Transport", "Sport", "Food", "Restaurants", "Clothing", "Other")

        // Создание адаптера для списка категорий
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Установка адаптера на спиннер категорий
        val spinner = findViewById<Spinner>(R.id.spinnerCategories)
        spinner.adapter = adapter

        // Установка слушателя для изменения выбранной категории
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = parent.getItemAtPosition(position).toString()

                // Фильтрация данных по выбранной категории
                val filteredTransactions = if (selectedCategory == "All") {
                    transactions // Если выбрана категория "All", показываем все транзакции
                } else {
                    transactions.filter { it.category == selectedCategory }
                }

                // Обновление суммы транзакций для выбранной категории
                val totalAmount = filteredTransactions.sumOf { it.amount }
                totalTextView.text = "Total: $totalAmount"

                // Создание адаптера для списка транзакций и привязка его к ListView
                val transactionAdapter = ArrayAdapter(this@DashboardActivity, android.R.layout.simple_list_item_1, filteredTransactions.map { "${it.description}: ${it.amount}" })
                listView.adapter = transactionAdapter
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Ничего не делаем при отсутствии выбранной категории
            }
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_dashboard -> {
                    // Переход на DashboardActivity
                    // Уже находимся на этом экране, ничего не делаем
                    true
                }
                R.id.menu_diagram -> {
                    // Переход на PieChartActivity
                    val intent = Intent(this, PieChartActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_add_transaction -> {
                    // Обработка нажатия на пункт "Add Transaction"
                    showAddTransactionPanel()
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
        bottomNavigationView.selectedItemId = R.id.menu_dashboard

        // Проверка на интент для открытия панели добавления транзакций
        val openAddTransaction = intent.getBooleanExtra("openAddTransaction", false)
        if (openAddTransaction) {
            showAddTransactionPanel()
        }
    }

    private fun showAddTransactionPanel() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_add_transaction, null)
        bottomSheetDialog.setContentView(view)

        val categorySpinner = view.findViewById<Spinner>(R.id.spinnerCategory)
        val categories = arrayOf("Transport", "Sport", "Food", "Restaurants", "Clothing", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        val descriptionEditText = view.findViewById<EditText>(R.id.editTextDescription)
        val amountEditText = view.findViewById<EditText>(R.id.editTextAmount)
        val addButton = view.findViewById<Button>(R.id.buttonAddTransaction)

        addButton.setOnClickListener {
            val category = categorySpinner.selectedItem.toString()
            val description = descriptionEditText.text.toString()
            val amount = amountEditText.text.toString().toDoubleOrNull()

            if (description.isNotEmpty() && amount != null) {
                // Добавление новой транзакции в список и обновление адаптера
                val newTransaction = Transaction(description, amount, category)
                transactions.add(newTransaction)

                // Обновление бюджета
                budget -= amount
                updateBudgetDisplay()

                // Перефильтрация и обновление списка
                val selectedCategory = (findViewById<Spinner>(R.id.spinnerCategories)).selectedItem.toString()
                val filteredTransactions = if (selectedCategory == "All") {
                    transactions
                } else {
                    transactions.filter { it.category == selectedCategory }
                }
                val transactionAdapter = ArrayAdapter(this@DashboardActivity, android.R.layout.simple_list_item_1, filteredTransactions.map { "${it.description}: ${it.amount}" })
                listView.adapter = transactionAdapter

                // Обновление суммы транзакций для выбранной категории
                val totalAmount = filteredTransactions.sumOf { it.amount }
                totalTextView.text = "Total: $totalAmount"
            }

            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun updateBudgetDisplay() {
        budgetTextView.text = "Budget: $budget"
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

data class Transaction(val description: String, val amount: Double, val category: String)
