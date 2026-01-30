package com.example.liberation

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class PolicyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.policy_activity)

        val close_btn = findViewById<Button>(R.id.btnClosePolicy)
        close_btn.setOnClickListener {
            finish()
        }
    }
}