package com.example.liberation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.graphics.Color
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


private val WHITELIST_SUFFIXES = listOf(
    ".google.com", ".google.co.kr", "google.com",
    ".naver.com", "naver.com",
    ".daum.net", "daum.net",
    ".kakao.com", "kakao.com",
    ".youtube.com", "youtube.com",
    ".gov.kr", ".go.kr",
    ".ac.kr", ".cau.ac.kr",
    ".microsoft.com", ".apple.com"
)
class LinkHandlerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_activivty)

        val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val isServiceEnabled = sharedPref.getBoolean("service_enabled", false)
        val savedBrowserPkg = sharedPref.getString("selected_browser_pkg", "com.android.chrome")
        val clientId = sharedPref.getString("client_id", null)

        // 1. 원본 URL 가져오기
        val rawUrl = intent.dataString ?: return finish()

        if (isServiceEnabled) {
            // 2. 구글 래핑 등 리다이렉션 URL 먼저 해제
            val actualUrl = unwrapUrl(rawUrl)

            // 3. 해제된 '진짜 목적지'로 화이트리스트 검사
            if (shouldBypassAnalysis(actualUrl)) {
                Log.d("LinkHandler", "Bypass Success: $actualUrl")
                openInCustomTabs(actualUrl, savedBrowserPkg)
            } else {
                analyzeUrlWithBackend(actualUrl, clientId, savedBrowserPkg)
            }
        } else {
            openInCustomTabs(rawUrl, savedBrowserPkg)
        }
    }

    /**
     * 구글 검색 결과나 Gmail 링크 등에 포함된 실제 목적지 URL을 추출합니다.
     */
    private fun unwrapUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            // google.com/url?q=... 또는 google.com/url?url=... 형태 처리
            if (uri.host?.contains("google.com") == true && uri.path == "/url") {
                uri.getQueryParameter("q") ?: uri.getQueryParameter("url") ?: url
            } else {
                url
            }
        } catch (e: Exception) {
            url
        }
    }

    private fun shouldBypassAnalysis(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return false

            // 래핑이 해제된 순수 도메인이 화이트리스트에 있는지 확인
            WHITELIST_SUFFIXES.any { suffix ->
                host == suffix.removePrefix(".") || host.endsWith(suffix)
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun analyzeUrlWithBackend(url: String, clientId: String?, browserPkg: String?) {
        // 백엔드 서버 연결 설정
        val retrofit = Retrofit.Builder()
            .baseUrl("http://172.21.118.55:8000/") // 실제 맥북 IP로 수정 필수
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(LiberationApi::class.java)
        val request = AnalyzeUrlIn(url, clientId)

        // 백엔드의 /analyze_url 엔드포인트 호출
        api.analyzeUrl(request).enqueue(object : Callback<AnalyzeUrlOut> {
            override fun onResponse(call: Call<AnalyzeUrlOut>, response: Response<AnalyzeUrlOut>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    // 백엔드의 decision(ALLOW, BLOCK, HOLD)에 따라 분기
                    handleScanResult(result, browserPkg)
                } else {
                    // 서버 에러 시 기본 브라우저로 안전하게 이동
                    openInCustomTabs(url, browserPkg)
                }
            }

            override fun onFailure(call: Call<AnalyzeUrlOut>, t: Throwable) {
                Log.e("LinkHandler", "연결 실패: ${t.message}")
                openInCustomTabs(url, browserPkg)
            }
        })
    }

    private fun handleScanResult(result: AnalyzeUrlOut?, browserPkg: String?) {
        val view = findViewById<View>(android.R.id.content)
        val decision = result?.decision ?: "ALLOW"
        val displayUrl = result?.final_url_display ?: "" // 마스킹된 URL

        when (decision) {
            "ALLOW" -> {
                val snackbar = Snackbar.make(view, "✅ 안전한 사이트입니다.", Snackbar.LENGTH_LONG)
                snackbar.setBackgroundTint(Color.parseColor("#6eff6d"))
                snackbar.setTextColor(Color.BLACK).show()
                openInCustomTabs(result?.final_url ?: "", browserPkg)
            }
            "HOLD" -> {
                val snackbar = Snackbar.make(view, "⚠️ 악성 사이트일 수 있습니다.", Snackbar.LENGTH_LONG)
                snackbar.setBackgroundTint(Color.parseColor("#fff163"))
                snackbar.setTextColor(Color.BLACK).show()

                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, HoldActivity::class.java)
                    intent.putExtra("TARGET_URL", result?.final_url)
                    intent.putExtra("DISPLAY_URL", displayUrl) // 가명처리된 URL 전달
                    intent.putExtra("REASON", result?.reason)
                    intent.putExtra("BROWSER_PKG", browserPkg)
                    startActivity(intent)
                    finish()
                }, 1000)
            }
            "BYPASS" -> {
                // [수정 포인트] 안전하다고 표시하지 않고 '임시 허용'임을 명시
                Toast.makeText(this, "⚠️ 주의: 사용자가 직접 허용한 사이트입니다.", Toast.LENGTH_SHORT).show()
                openInCustomTabs(result?.final_url ?: "", browserPkg)
            }
            "BLOCK" -> {
                val snackbar = Snackbar.make(view, "❌ 위험 사이트로 차단되었습니다.", Snackbar.LENGTH_LONG)
                snackbar.setBackgroundTint(Color.parseColor("#FF7777"))
                snackbar.setTextColor(Color.BLACK).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    finish()
                }, 1000)
            }
        }
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