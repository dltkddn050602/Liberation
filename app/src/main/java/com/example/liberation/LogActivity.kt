package com.example.liberation

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LogActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var apiService: LiberationApi // 클래스 내부 선언 하나만 남깁니다.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.log_activity)

        recyclerView = findViewById(R.id.recyclerViewLogs)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // NetworkClient를 통해 API 서비스 초기화
        apiService = NetworkClient.getRetrofit().create(LiberationApi::class.java)

        val btnClose = findViewById<Button>(R.id.btnCloseLog)
        btnClose.setOnClickListener {
            finish()
        }

        loadAnalysisLogs()
    }

    private fun loadAnalysisLogs() {
        apiService.getRecentEvents(withFeedback = 1).enqueue(object : Callback<AnalyzeEventResponse> {
            override fun onResponse(call: Call<AnalyzeEventResponse>, response: Response<AnalyzeEventResponse>) {
                if (response.isSuccessful) {
                    val events = response.body()?.events ?: emptyList()
                    // AnalysisEvent(events) 대신 LogAdapter(events)를 사용합니다.
                    recyclerView.adapter = LogAdapter(events)
                    Log.d("LogActivity", "Successfully loaded ${events.size} logs")
                } else {
                    Log.e("LogActivity", "Server error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<AnalyzeEventResponse>, t: Throwable) {
                Log.e("LogActivity", "Network failure: ${t.message}")
            }
        })
    }
}