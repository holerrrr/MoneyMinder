package com.example.moneyminder

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

object NetworkUtils {
    private var requestQueue: RequestQueue? = null

    private fun getRequestQueue(context: Context): RequestQueue {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context)
        }
        return requestQueue!!
    }

    fun addIncome(context: Context, userId: Int, sum: Double, listener: Response.Listener<String>, errorListener: Response.ErrorListener) {
        val url = "http://192.168.1.250/moneyminder/add_income.php"
        val params = HashMap<String, String>()
        params["user_id"] = userId.toString()
        params["sum"] = sum.toString()

        val request = object : StringRequest(Method.POST, url, listener, errorListener) {
            override fun getParams(): MutableMap<String, String> = params
        }

        getRequestQueue(context).add(request)
    }

    fun addExpense(context: Context, userId: Int, categoryId: Int, sum: Double, listener: Response.Listener<String>, errorListener: Response.ErrorListener) {
        val url = "http://192.168.1.250/moneyminder/add_expense.php"
        val params = HashMap<String, String>()
        params["user_id"] = userId.toString()
        params["category_id"] = categoryId.toString()
        params["sum"] = sum.toString()

        val request = object : StringRequest(Method.POST, url, listener, errorListener) {
            override fun getParams(): MutableMap<String, String> = params
        }

        getRequestQueue(context).add(request)
    }

    fun getBalance(context: Context, userId: Int, listener: Response.Listener<JSONObject>, errorListener: Response.ErrorListener) {
        val url = "http://192.168.1.250/moneyminder/get_balance.php"
        val params = HashMap<String, String>()
        params["user_id"] = userId.toString()

        val request = JsonObjectRequest(Request.Method.POST, url, JSONObject(params as Map<*, *>), listener, errorListener)

        getRequestQueue(context).add(request)
    }
}
