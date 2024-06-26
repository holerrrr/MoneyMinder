package com.example.moneyminder

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.common.api.ApiException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {
    private lateinit var oneTapClient: SignInClient
    private lateinit var signUpRequest: BeginSignInRequest
    private lateinit var callbackManager: CallbackManager
    private lateinit var signInLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val textViewRegister = findViewById<TextView>(R.id.textViewRegister)
        textViewRegister.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        // Initialize One Tap API
        oneTapClient = Identity.getSignInClient(this)
        signUpRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build())
            .build()

        // Initialize ActivityResultLauncher for Google Sign-In
        signInLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                try {
                    val credential: SignInCredential = oneTapClient.getSignInCredentialFromIntent(result.data)
                    val idToken = credential.googleIdToken
                    if (idToken != null) {
                        Log.d(TAG, "Got ID token.")
                        // Use the ID token for authentication on your server
                    } else {
                        Log.d(TAG, "No ID token!")
                    }
                } catch (e: ApiException) {
                    Log.e(TAG, "ApiException: ${e.localizedMessage}")
                }
            } else {
                Log.d(TAG, "Google Sign-In failed with resultCode: ${result.resultCode}")
            }
        }

        // Google Sign-In button click handler
        val googleButton = findViewById<Button>(R.id.button_google)
        googleButton.setOnClickListener {
            Log.d(TAG, "Google sign-in button clicked")
            oneTapClient.beginSignIn(signUpRequest)
                .addOnSuccessListener(this) { result ->
                    try {
                        val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                        signInLauncher.launch(intentSenderRequest)
                    } catch (e: IntentSender.SendIntentException) {
                        Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                    }
                }
                .addOnFailureListener(this) { e ->
                    Log.d(TAG, "One Tap SignIn failed: ${e.localizedMessage}")
                }
        }

        callbackManager = CallbackManager.Factory.create()
        val facebookButton = findViewById<Button>(R.id.button_facebook)
        facebookButton.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
        }


        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                Log.d(TAG, "Facebook token: ${result.accessToken.token}")
                val accessToken = result.accessToken
                val request = GraphRequest.newMeRequest(accessToken) { obj, response ->
                    try {
                        val name = obj?.getString("name") ?: ""
                        val email = obj?.getString("email") ?: ""
                        saveUserToDatabase(name, email)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val parameters = Bundle()
                parameters.putString("fields", "id,name,email")
                request.parameters = parameters
                request.executeAsync()
            }

            override fun onCancel() {
                Log.d(TAG, "Facebook login canceled")
            }

            override fun onError(error: FacebookException) {
                Log.e(TAG, "Facebook login failed: ${error.localizedMessage}")
            }
        })

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

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
    fun saveUserToDatabase(name: String, email: String) {
        val client = OkHttpClient()

        val json = JSONObject().apply {
            put("name", name)
            put("email", email)
            put("password", "facebook_user") // Установите дефолтный пароль или генерайте случайный
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://192.168.1.217/moneyminder/registration.php") // URL вашего скрипта для регистрации
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Log.e("LoginActivity", "Registration failed: ${e.message}")
                    Toast.makeText(applicationContext, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    Log.d("LoginActivity", "Server response: $responseBody")
                    if (response.isSuccessful) {
                        Toast.makeText(applicationContext, "Registration successful", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                        startActivity(intent)
                    } else {
                        Log.e("LoginActivity", "Registration failed: $responseBody")
                        Toast.makeText(applicationContext, "Registration failed: $responseBody", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun loginUser(email: String, password: String) {
        val client = OkHttpClient()

        val json = JSONObject().apply {
            put("email", email)
            put("password", password)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://192.168.1.217/moneyminder/login.php")
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
                                Toast.makeText(applicationContext, "Login successful", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@LoginActivity, DashboardActivity::class.java) // Replace with your next screen
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

    companion object {
        private const val TAG = "LoginActivity"
        private const val REQ_ONE_TAP = 2
    }
}
