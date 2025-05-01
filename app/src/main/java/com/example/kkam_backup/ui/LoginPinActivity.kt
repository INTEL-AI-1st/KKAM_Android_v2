package com.example.kkam_backup.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kkam_backup.R

class LoginPinActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button  // 이름을 btnLogin 으로 변경

    companion object {
        private const val CORRECT_EMAIL = "email@domain.com"
        private const val CORRECT_PASSWORD = "password123"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // activity_login.xml 레이아웃 사용
        setContentView(R.layout.activity_login)

        etEmail    = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin   = findViewById(R.id.btnLogin)  // R.id.btnLogin 과 일치시킴

        btnLogin.setOnClickListener {
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            when {
                email.isEmpty() || password.isEmpty() -> {
                    Toast.makeText(this, "이메일과 비밀번호를 모두 입력하세요", Toast.LENGTH_SHORT).show()
                }
                email != CORRECT_EMAIL || password != CORRECT_PASSWORD -> {
                    Toast.makeText(this, "이메일 또는 비밀번호가 올바르지 않습니다", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // 로그인 성공 시 MainActivity 로 이동
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}
