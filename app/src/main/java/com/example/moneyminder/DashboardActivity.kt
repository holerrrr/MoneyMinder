package com.example.moneyminder

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
class DashboardActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var budgetTextView: TextView
    private lateinit var totalTextView: TextView
    private lateinit var categorySpinner: Spinner
    private var budget: Double = 0.0 // Начальный бюджет
    private val transactions = mutableListOf<Transaction>()
    private val categories = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        listView = findViewById(R.id.listView)
        budgetTextView = findViewById(R.id.textViewBudget)
        totalTextView = findViewById(R.id.textViewTotal)
        categorySpinner = findViewById(R.id.spinnerCategories)

        // Загрузка текущего бюджета из SharedPreferences
        val sharedPreferences = getSharedPreferences("MoneyMinderPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1) // Получение идентификатора пользователя
        loadBudgetFromDatabase(userId)

        // Загрузка категорий из базы данных
        loadCategoriesFromDatabase()

        // Загрузка данных о финансовых транзакциях из базы данных или другого источника данных
        loadTransactionsFromDatabase(userId)

        // Установка слушателя для изменения выбранной категории
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
        listView.setOnItemLongClickListener { parent, view, position, id ->
            val transaction = transactions[position]
            showDeleteTransactionDialog(transaction, position)
            true // Возвращаем true, чтобы сообщить обработчику, что событие обработано
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
    private fun showDeleteTransactionDialog(transaction: Transaction, position: Int) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Delete Transaction")
        alertDialogBuilder.setMessage("Are you sure you want to delete this transaction?")
        alertDialogBuilder.setPositiveButton("Yes") { _, _ ->
            // Удаление транзакции из базы данных
            val sharedPreferences = getSharedPreferences("MoneyMinderPrefs", MODE_PRIVATE)
            val userId = sharedPreferences.getInt("user_id", -1)
            deleteTransactionFromDatabase(userId, position)
        }
        alertDialogBuilder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun deleteTransactionFromDatabase(userId: Int, position: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                val json = JSONObject().apply {
                    put("user_id", userId)
                    put("position", position)
                }

                val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("http://192.168.1.214/moneyminder/delete_transaction.php")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e("DashboardActivity", "Failed to delete transaction: ${response.message}")
                } else {
                    // Удаляем элемент из списка транзакций
                    transactions.removeAt(position)
                    // Обновляем список транзакций после успешного удаления
                    loadTransactionsFromDatabase(userId)
                }
            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error deleting transaction: ${e.message}")
            }
        }
    }

    private fun showAddTransactionPanel() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_add_transaction, null)
        bottomSheetDialog.setContentView(view)

        val categorySpinner = view.findViewById<Spinner>(R.id.spinnerCategory)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories.drop(1)) // исключаем "All" из категорий
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

                // Добавление транзакции в базу данных
                val sharedPreferences = getSharedPreferences("MoneyMinderPrefs", MODE_PRIVATE)
                val userId = sharedPreferences.getInt("user_id", -1)
                addTransactionToDatabase(userId, category, description, amount)

                // Перефильтрация и обновление списка
                val selectedCategory = categorySpinner.selectedItem.toString()
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
        runOnUiThread {
            budgetTextView.text = "Budget: $budget"
        }
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
                    .url("http://192.168.1.214/moneyminder/get_transactions.php")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d("DashboardActivity", "Request: $json")
                Log.d("DashboardActivity", "Response: $responseBody")

                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    val jsonArray = JSONArray(responseBody)
                    transactions.clear() // Очищаем список перед добавлением новых данных
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val description = jsonObject.getString("description")
                        val amount = jsonObject.getDouble("Sum")
                        val category = jsonObject.getString("category")
                        transactions.add(Transaction(description, amount, category))
                    }
                    runOnUiThread {
                        val selectedCategory = findViewById<Spinner>(R.id.spinnerCategories).selectedItem.toString()
                        val filteredTransactions = if (selectedCategory == "All") {
                            transactions
                        } else {
                            transactions.filter { it.category == selectedCategory }
                        }
                        val transactionAdapter = ArrayAdapter(this@DashboardActivity, android.R.layout.simple_list_item_1, filteredTransactions.map { "${it.description}: ${it.amount}" })
                        listView.adapter = transactionAdapter

                        val totalAmount = filteredTransactions.sumOf { it.amount }
                        totalTextView.text = "Total: $totalAmount"
                    }
                } else {
                    Log.e("DashboardActivity", "Failed to load transactions: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error loading transactions: ${e.message}")
            }
        }
    }

    private fun loadCategoriesFromDatabase() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                val request = Request.Builder()
                    .url("http://192.168.1.214/moneyminder/get_categories.php")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    val jsonArray = JSONArray(responseBody)
                    categories.clear() // Очищаем список перед добавлением новых данных
                    categories.add("All") // Добавляем "All" в список категорий
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val categoryName = jsonObject.getString("Name")
                        categories.add(categoryName)
                    }
                    runOnUiThread {
                        val adapter = ArrayAdapter(this@DashboardActivity, android.R.layout.simple_spinner_item, categories)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        categorySpinner.adapter = adapter
                    }
                }
            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error loading categories: ${e.message}")
            }
        }
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
                    .url("http://192.168.1.214/moneyminder/get_balance.php")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d("DashboardActivity", "Request: $json")
                Log.d("DashboardActivity", "Response: $responseBody")

                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    val jsonObject = JSONObject(responseBody)
                    if (jsonObject.has("balance")) {
                        val balance = jsonObject.getDouble("balance")
                        budget = balance // Обновляем значение переменной budget
                        runOnUiThread {
                            budgetTextView.text = "Budget: $budget"
                            Log.d("DashboardActivity", "Budget updated: $budget")
                        }
                    } else {
                        Log.e("DashboardActivity", "JSON does not contain 'balance' key")
                    }
                } else {
                    Log.e("DashboardActivity", "Failed to load budget: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error loading budget: ${e.message}")
            }
        }
    }

    private fun addTransactionToDatabase(userId: Int, category: String, description: String, amount: Double) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                val json = JSONObject().apply {
                    put("user_id", userId)
                    put("category", category)
                    put("description", description)
                    put("amount", amount)
                }

                val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("http://192.168.1.214/moneyminder/add_transaction.php")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    // Обновляем список транзакций после успешного добавления
                    loadTransactionsFromDatabase(userId)
                    // Обновляем бюджет после успешного добавления
                    loadBudgetFromDatabase(userId)
                } else {
                    Log.e("DashboardActivity", "Failed to add transaction: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error adding transaction: ${e.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences = getSharedPreferences("MoneyMinderPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)
        loadTransactionsFromDatabase(userId)
        loadBudgetFromDatabase(userId)
        val openAddTransaction = intent.getBooleanExtra("openAddTransaction", false)
        if (openAddTransaction) {
            showAddTransactionPanel()
        }
    }
}

data class Transaction(val description: String, val amount: Double, val category: String)
