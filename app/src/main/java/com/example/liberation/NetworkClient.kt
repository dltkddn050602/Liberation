package com.example.liberation

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkClient {
    // 본인의 PC 로컬 IP 주소로 수정하세요. 끝에 / 가 반드시 있어야 합니다.
    private const val BASE_URL = "http://172.21.118.55:8000/"

    private var retrofit: Retrofit? = null

    fun getRetrofit(): Retrofit {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }
}