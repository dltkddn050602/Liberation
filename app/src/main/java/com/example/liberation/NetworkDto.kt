package com.example.liberation

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// 백엔드 /consent API 요청 구조
data class ConsentRequest(
    val client_id: String,
    val required_accepted: Boolean,
    val optional_accepted: Boolean,
    val required_version: String? = "2026-01-30",
    val optional_version: String? = "2026-01-30"
)

data class AnalyzeUrlOut(
    val decision: String,           // ALLOW | BLOCK | HOLD
    val final_url: String,          // 실제 접속용 raw URL
    val final_url_display: String,  // 마스킹된 표시용 URL
    val reason: String,             // 사용자에게 보여줄 차단/허용 이유
    val reason_code: String,        // 내부 로직용 코드 (예: APK_BLOCK)
    val risk_score: Float,          // 위험 점수 (0.0 ~ 1.0)
    val is_file: Boolean,           // 파일 여부
    val detected_type_hint: String, // PDF | APK | ZIP | UNKNOWN
    val redirect_chain_display: List<String> // 리다이렉트 경로 리스트
)

// 최근 기록 조회를 위한 모델 (백엔드 events/recent 매칭)
data class EventResponse(
    val count: Int,
    val events: List<AnalysisEvent>
)

data class AnalysisEvent(
    val id: Int,
    val ts: String,                 // 검사 시간
    val decision: String,           // 판단 결과
    val final_url_display: String,  // 마스킹된 URL
    val risk_score: Float,           // 위험 점수
    val reason_code: String,
    val detected_type_hint: String
)


interface LiberationApi {
    @POST("consent")
    fun postConsent(@Body request: ConsentRequest): Call<Void>
    @POST("analyze_url")
    fun analyzeUrl(@Body request: AnalyzeUrlIn): Call<AnalyzeUrlOut>
    @POST("allow_once")
    fun allowOnce(@Body body: AllowOnceIn): Call<Void>
    @GET("/events/recent")
    fun getRecentEvents(@Query("with_feedback") withFeedback: Int = 1): Call<AnalyzeEventResponse>
}

data class AllowOnceIn(
    val client_id: String,
    val final_url: String, // 분석 결과로 받았던 원본 final_url
    val reason: String? = "사용자 직접 허용"
)

data class AnalyzeUrlIn(
    val url: String,
    val client_id: String?
)

data class AnalyzeEventResponse(
    val count: Int,
    val events: List<AnalysisEvent>
)

