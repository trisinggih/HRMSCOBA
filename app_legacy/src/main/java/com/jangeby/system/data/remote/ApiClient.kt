package com.jangeby.system.data.remote

import android.content.Context
import com.jangeby.system.data.local.SecurePrefs
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Pusat konfigurasi Retrofit untuk seluruh panggilan API mobile.
 * BASE_URL dapat diubah; default menunjuk ke backend Laravel lokal.
 */
object ApiClient {

    /** Ubah di sini bila base URL backend berbeda. */
    //const val BASE_URL: String = "http://172.20.10.4:8011/api/v1/"
    //const val BASE_URL: String = "http://192.168.10.6:8011/api/v1/"
    const val BASE_URL: String = "https://jangeby.rjpteknologi.com/api/v1/"

    @Volatile
    private var service: ApiService? = null

    @Volatile
    private var prefs: SecurePrefs? = null

    /**
     * Wajib dipanggil saat aplikasi start (mis. dari MainActivity onCreate)
     * agar interceptor bisa membaca token dari penyimpanan aman.
     */
    fun init(context: Context) {
        if (prefs == null) {
            synchronized(this) {
                if (prefs == null) {
                    prefs = SecurePrefs(context.applicationContext)
                }
            }
        }
    }

    private fun currentToken(): String? = prefs?.token

    fun getService(context: Context): ApiService {
        init(context)
        return service ?: synchronized(this) {
            service ?: buildService().also { service = it }
        }
    }

    private fun buildService(): ApiService {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val token = currentToken()
            val request = if (token.isNullOrBlank()) {
                original
            } else {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/json")
                    .build()
            }
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
