package com.example.liberation

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HoldActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hold_activity)

        val targetUrl = intent.getStringExtra("TARGET_URL")
        val browserPkg = intent.getStringExtra("BROWSER_PKG")
        val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val clientId = sharedPref.getString("client_id", null)

        // '이번 한 번만 이동' 버튼 클릭 시
        findViewById<Button>(R.id.btnContinue).setOnClickListener {
            if (clientId != null && targetUrl != null) {
                // [보완] 서버에 1회 허용 요청을 먼저 보냅니다.
                requestAllowOnce(clientId, targetUrl, browserPkg)
            } else {
                // 정보가 부족할 경우 바로 이동 (안전 장치)
                openInCustomTabs(targetUrl ?: "", browserPkg)
            }
        }

        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            Toast.makeText(this, "⚠️ 취소되었습니다", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun requestAllowOnce(clientId: String, url: String, browserPkg: String?) {
        // [보완] 기존에 MainActivity/LogActivity에서 쓰던 NetworkClient를 활용합니다.
        val api = NetworkClient.getRetrofit().create(LiberationApi::class.java)
        val body = AllowOnceIn(clientId, url)

        api.allowOnce(body).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                // 백엔드 예외 등록 성공 후 브라우저 실행
                openInCustomTabs(url, browserPkg)
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // 네트워크 오류 시에도 일단 사용자의 선택을 존중하여 이동하지만 로그를 남깁니다.
                android.util.Log.e("HoldActivity", "AllowOnce 실패: ${t.message}")
                openInCustomTabs(url, browserPkg)
            }
        })
    }

    private fun openInCustomTabs(url: String, browserPkg: String?) {
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        if (browserPkg != null) customTabsIntent.intent.setPackage(browserPkg)
        try {
            customTabsIntent.launchUrl(this, Uri.parse(url))
        } catch (e: Exception) {
            customTabsIntent.intent.setPackage(null)
            customTabsIntent.launchUrl(this, Uri.parse(url))
        }
        finish()
    }
}