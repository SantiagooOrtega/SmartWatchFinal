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
    // Usamos IP directa porque el emulador Wear OS no resuelve DNS externos
    private const val BASE_URL = "https://18.229.248.167/"
    private const val NGROK_HOST = "capably-professor-effective.ngrok-free.dev"

    val instance: ApiService by lazy {
        val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
        }
        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .addHeader("User-Agent", "SmartTranslator/1.0")
                    .addHeader("Host", NGROK_HOST)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .hostnameVerifier { _, _ -> true }
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
