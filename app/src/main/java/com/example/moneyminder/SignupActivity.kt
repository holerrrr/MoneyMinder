package com.example.moneyminder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SignupActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var loginEditText: EditText
    private lateinit var password1EditText: EditText
    private lateinit var password2EditText: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signup)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        nameEditText = findViewById(R.id.TextName)
        emailEditText = findViewById(R.id.TextEmail)
        loginEditText = findViewById(R.id.TextLogin)
        password1EditText = findViewById(R.id.TextPassword1)
        password2EditText = findViewById(R.id.TextPassword2)

        val textViewLogin = findViewById<TextView>(R.id.textViewLogin)
        val signupButton = findViewById<Button>(R.id.button_signup)


        signupButton.setOnClickListener {
            // Получаем значения из полей ввода
            val name = nameEditText.text.toString()
            val email = emailEditText.text.toString()
            val login = loginEditText.text.toString()
            val password1 = password1EditText.text.toString()
            val password2 = password2EditText.text.toString()

            // Теперь можно выполнить нужные действия с этими данными, например, отправить их на сервер или сохранить в базу данных
            // Например:
            if (password1 == password2) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Пароли не совпадают, выведите сообщение об ошибке
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            }
        }

        signupButton.setOnClickListener {
            // Створюємо інтент для переходу на активність activity_login
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }
    }

}