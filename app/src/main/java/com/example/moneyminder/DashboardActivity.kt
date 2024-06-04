package com.example.moneyminder

import android.content.Intent
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
    private var budget: Double = 0.0
    private val transactions = mutableListOf<Transaction>()
    private val categories = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        listView = findViewById(R.id.listView)
        budgetTextView = findViewById(R.id.textViewBudget)
        totalTextView = findViewById(R.id.textViewTotal)
        categorySpinner = findViewById(R.id.spinnerCategories)

        val sharedPreferences = getSharedPreferences("MoneyMinderPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)
        loadBudgetFromDatabase(userId)
        loadCategoriesFromDatabase()

        // Загрузка данных о финансовых транзакциях из базы данных или другого источника данных будет вызвана после инициализации спиннера

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = parent.getItemAtPosition(position).toString()
                filterAndDisplayTransactions(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Ничего не делаем при отсутствии выбранной категории
            }
        }

        listView.setOnItemLongClickListener { parent, view, position, id ->
            val transaction = transactions[position]
            showDeleteTransactionDialog(transaction)
            true
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_dashboard -> {
                    true
                }
                R.id.menu_diagram -> {
                    val intent = Intent(this, PieChartActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_add_transaction -> {
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
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories.drop(1))
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
                val newTransaction = Transaction(
                    id = 0,
                    description = description,
                    amount = amount,
                    category = category
                )
                transactions.add(newTransaction)

                val sharedPreferences = getSharedPreferences("MoneyMinderPrefs", MODE_PRIVATE)
                val userId = sharedPreferences.getInt("user_id", -1)
                addTransactionToDatabase(userId, category, description, amount)

                filterAndDisplayTransactions(category)
            }

            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun filterAndDisplayTransactions(selectedCategory: String) {
        val filteredTransactions = if (selectedCategory == "All") {
            transactions
        } else {
            transactions.filter { it.category == selectedCategory }
        }

        val totalAmount = filteredTransactions.sumOf { it.amount }
        totalTextView.text = "Total: $totalAmount"

        val transactionAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, filteredTransactions.map { "${it.description}: ${it.amount}" })
        listView.adapter = transactionAdapter
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
                    .url("http://192.168.1.250/moneyminder/get_transactions.php")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d("DashboardActivity", "Request: $json")
                Log.d("DashboardActivity", "Response: $responseBody")

                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    val jsonArray = JSONArray(responseBody)
                    transactions.clear()
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)

                        val id = jsonObject.optInt("id", -1)
                        val description = jsonObject.optString("description", "Unknown")
                        val amount = jsonObject.optDouble("Sum", 0.0)
                        val category = jsonObject.optString("category", "Unknown")

                        if (id != -1 && description != "Unknown" && category != "Unknown") {
                            transactions.add(Transaction(id, description, amount, category))
                        } else {
                            Log.e("DashboardActivity", "Invalid transaction data: $jsonObject")
                        }
                    }
                    runOnUiThread {
                        val selectedCategory = categorySpinner.selectedItem?.toString() ?: "All"
                        filterAndDisplayTransactions(selectedCategory)
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
                    .url("http://192.168.1.250/moneyminder/get_categories.php")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    val jsonArray = JSONArray(responseBody)
                    categories.clear()
                    categories.add("All")
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val categoryName = jsonObject.getString("Name")
                        categories.add(categoryName)
                    }
                    runOnUiThread {
                        val adapter = ArrayAdapter(this@DashboardActivity, android.R.layout.simple_spinner_item, categories)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        categorySpinner.adapter = adapter
                        // Загружаем транзакции после инициализации спиннера
                        val sharedPreferences = getSharedPreferences("MoneyMinderPrefs", MODE_PRIVATE)
                        val userId = sharedPreferences.getInt("user_id", -1)
                        loadTransactionsFromDatabase(userId)
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
                    .url("http://192.168.1.250/moneyminder/get_budget.php")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d("DashboardActivity", "Request: $json")
                Log.d("DashboardActivity", "Response: $responseBody")

                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    val jsonObject = JSONObject(responseBody)
                    budget = jsonObject.optDouble("budget", 0.0)
                    updateBudgetDisplay()
                } else {
                    Log.e("DashboardActivity", "Failed to load budget: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error loading budget: ${e.message}")
            }
        }
    }

    private fun showDeleteTransactionDialog(transaction: Transaction) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Transaction")
        builder.setMessage("Are you sure you want to delete this transaction?")
        builder.setPositiveButton("Yes") { dialog, which ->
            val sharedPreferences = getSharedPreferences("MoneyMinderPrefs", MODE_PRIVATE)
            val userId = sharedPreferences.getInt("user_id", -1)
            deleteTransactionFromDatabase(transaction, userId)
            transactions.remove(transaction)
            val selectedCategory = categorySpinner.selectedItem.toString()
            filterAndDisplayTransactions(selectedCategory)
        }
        builder.setNegativeButton("No", null)
        builder.show()
    }

    private fun deleteTransactionFromDatabase(transaction: Transaction, userId: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                val json = JSONObject().apply {
                    put("transaction_id", transaction.id)
                    put("user_id", userId)
                }

                val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("http://192.168.1.250/moneyminder/delete_transaction.php")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d("DashboardActivity", "Request: $json")
                Log.d("DashboardActivity", "Response: $responseBody")

                if (!response.isSuccessful) {
                    Log.e("DashboardActivity", "Failed to delete transaction: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error deleting transaction: ${e.message}")
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
                    .url("http://192.168.1.250/moneyminder/add_transaction.php")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d("DashboardActivity", "Request: $json")
                Log.d("DashboardActivity", "Response: $responseBody")

                if (!response.isSuccessful) {
                    Log.e("DashboardActivity", "Failed to add transaction: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("DashboardActivity", "Error adding transaction: ${e.message}")
            }
        }
    }
}
data class Transaction(val id: Int, val description: String, val amount: Double, val category: String)