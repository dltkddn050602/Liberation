package com.example.liberation

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

class TermsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.terms_activity)

        val first_agree = findViewById<RadioButton>(R.id.agree1)
        val second_agree = findViewById<RadioButton>(R.id.agree2)
        val third_agree = findViewById<RadioButton>(R.id.agree3)
        val third_disagree = findViewById<RadioButton>(R.id.disagree3)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        first_agree.setOnClickListener {
            if ((first_agree.isChecked && second_agree.isChecked && third_agree.isChecked)||(first_agree.isChecked && second_agree.isChecked && third_disagree.isChecked))
            {
                btnSubmit.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#000000"))
            }
        }
        second_agree.setOnClickListener {
            if ((first_agree.isChecked && second_agree.isChecked && third_agree.isChecked)||(first_agree.isChecked && second_agree.isChecked && third_disagree.isChecked))
            {
                btnSubmit.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#000000"))
            }
        }
        third_agree.setOnClickListener {
            if ((first_agree.isChecked && second_agree.isChecked && third_agree.isChecked)||(first_agree.isChecked && second_agree.isChecked && third_disagree.isChecked))
            {
                btnSubmit.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#000000"))
            }
        }
        third_disagree.setOnClickListener {
            if ((first_agree.isChecked && second_agree.isChecked && third_agree.isChecked)||(first_agree.isChecked && second_agree.isChecked && third_disagree.isChecked))
            {
                btnSubmit.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#000000"))
            }
        }

        btnSubmit.setOnClickListener {
            if ((first_agree.isChecked && second_agree.isChecked)) { // 필수 항목 동의 확인

                // 1. Client ID 생성 (백엔드에서 UUID 형식을 요구함)
                val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)
                var clientId = sharedPref.getString("client_id", null)
                if (clientId == null) {
                    clientId = UUID.randomUUID().toString()
                    sharedPref.edit().putString("client_id", clientId).apply()
                }

                val isOptionalAgreed = third_agree.isChecked

                // 2. 백엔드로 동의 데이터 전송
                sendConsentToBackend(clientId, true, isOptionalAgreed)

                // 3. 로컬 저장 및 화면 이동
                sharedPref.edit().putBoolean("terms_agreed", true).apply()
                sharedPref.edit().putBoolean("Conditions Optional", isOptionalAgreed).apply()

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
    private fun sendConsentToBackend(clientId: String, required: Boolean, optional: Boolean) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/") // 내 컴퓨터 백엔드 주소 (실제 폰 디버깅시 PC IP 입력)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(LiberationApi::class.java)
        val request = ConsentRequest(clientId, required, optional)

        api.postConsent(request).enqueue(object : retrofit2.Callback<Void> {
            override fun onResponse(call: Call<Void>, response: retrofit2.Response<Void>) {
                // 성공 시 백엔드 DB에 저장됨
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                // 네트워크 오류 처리
            }
        })
    }
}