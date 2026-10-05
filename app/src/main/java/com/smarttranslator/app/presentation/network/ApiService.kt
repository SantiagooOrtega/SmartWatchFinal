package com.smarttranslator.app.presentation.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// ── Modelos de datos ──────────────────────────────────────────

data class TranslateRequest(
    val text: String,
    val source_lang: String = "ES",
    val target_lang: String = "EN"
)

data class TranslationData(
    val id: String,
    val original_text: String,
    val translated_text: String,
    val source_lang: String,
    val target_lang: String,
    val created_at: String
)

data class TranslateResponse(
    val success: Boolean,
    val data: TranslationData?
)

data class HistoryResponse(
    val success: Boolean,
    val count: Int,
    val data: List<TranslationData>
)

// ── Interface Retrofit ────────────────────────────────────────

interface ApiService {

    @POST("api/translate")
    suspend fun translate(@Body request: TranslateRequest): TranslateResponse

    @GET("api/translations")
    suspend fun getHistory(): HistoryResponse
}

// ── Singleton Retrofit ────────────────────────────────────────

object RetrofitClient {
    private const val BASE_URL = "https://capably-professor-effective.ngrok-free.dev/"

    val instance: ApiService by lazy {
        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
