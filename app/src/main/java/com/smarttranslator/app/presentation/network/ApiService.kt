package com.smarttranslator.app.presentation.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
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
    val data: TranslationData?,
    /** false si la traducción funcionó pero no se pudo guardar en la BD */
    val saved: Boolean = true
)

data class HistoryResponse(
    val success: Boolean,
    val count: Int,
    val data: List<TranslationData>
)

data class ClearResponse(
    val success: Boolean,
    val deleted: Int = 0
)

// ── Interface Retrofit ────────────────────────────────────────

interface ApiService {

    @POST("api/translate")
    suspend fun translate(@Body request: TranslateRequest): TranslateResponse

    @GET("api/translations")
    suspend fun getHistory(): HistoryResponse

    @DELETE("api/translations")
    suspend fun clearHistory(): ClearResponse
}

// ── Singleton Retrofit ────────────────────────────────────────

object RetrofitClient {
    /**
     * URL del backend.
     *  - Reloj real en la misma Wi-Fi: http://<IP de tu Mac>:3000/   (ver `ipconfig getifaddr en0`)
     *  - Emulador: http://10.0.2.2:3000/
     *  - Fuera de casa: tu URL pública de ngrok (https://xxxx.ngrok-free.dev/)
     */
    private const val BASE_URL = "http://192.168.0.26:3000/"

    val instance: ApiService by lazy {
        val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
        }
        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .addHeader("ngrok-skip-browser-warning", "true")
                        .addHeader("User-Agent", "SmartTranslator/1.0")
                        .build()
                )
            }
            .addInterceptor(logging)
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
