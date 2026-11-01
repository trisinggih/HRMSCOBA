package com.jangeby.system.data.remote

import com.jangeby.system.data.local.AppSettings
import io.ktor.client.HttpClient
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

import kotlinx.coroutines.isActive

/**
 * Factory untuk membuat Ktor HttpClient cross-platform.
 * Engine di-inject via expect/actual dari androidMain & iosMain.
 */
expect fun createHttpEngine(): io.ktor.client.engine.HttpClientEngine

object ApiClient {

    // Local development server (IP Wi-Fi: 192.168.1.127 / adb reverse: localhost:8011)
    // const val BASE_URL = "http://192.168.0.101:8011/api/v1/"
    // Online production server
    const val BASE_URL = "https://jangeby.rjpteknologi.com/api/v1/"

    private var tokenProvider: (() -> String?)? = {
        try {
            AppSettings().token
        } catch (e: Exception) {
            null
        }
    }

    fun setTokenProvider(provider: () -> String?) {
        tokenProvider = provider
    }

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val AuthTokenPlugin = createClientPlugin("AuthTokenPlugin") {
        onRequest { request, _ ->
            val token = tokenProvider?.invoke()
            if (!token.isNullOrEmpty()) {
                request.headers.remove("Authorization")
                request.header("Authorization", "Bearer $token")
            }
        }
    }

    // Singleton client — dibuat sekali, dipakai ulang untuk semua request
    private var _client: HttpClient? = null

    fun create(): HttpClient {
        val current = _client
        if (current == null || !current.isActive) {
            _client = buildClient()
        }
        return _client!!
    }

    private fun buildClient(): HttpClient = HttpClient(createHttpEngine()) {
        defaultRequest {
            header("Accept", "application/json")
        }

        install(AuthTokenPlugin)

        install(ContentNegotiation) {
            json(json)
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("[Ktor] $message")
                }
            }
            level = LogLevel.INFO  // Hanya log status, bukan body (lebih ringan)
        }
    }

    // Panggil ini saat logout untuk reset client
    fun reset() {
        _client?.close()
        _client = null
    }

    /**
     * Sanitasi URL gambar:
     * 1. Menangani path relatif (prepend host dari BASE_URL).
     * 2. Menghindari '127.0.0.1' / 'localhost' jika diakses dari perangkat mobile eksternal,
     *    diganti dengan host dari BASE_URL.
     * 3. Jika local dev (port 8011, http, LAN IP 192.168.x.x, atau emulator 10.0.2.2),
     *    pastikan protokol adalah HTTP (jangan force HTTPS karena server lokal tidak memiliki SSL listener).
     * 4. Jika production domain (https), pastikan protokol HTTPS untuk menghindari mixed-content.
     */
    fun sanitizeImageUrl(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        var url = rawUrl.trim()

        val baseHost = BASE_URL.substringBefore("/api/v1/").removeSuffix("/")

        // Perbaiki path relatif
        if (url.startsWith("/")) {
            url = "$baseHost$url"
        } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "$baseHost/$url"
        }

        // Redirect 127.0.0.1 atau localhost ke host BASE_URL jika BASE_URL bukan localhost
        if ((url.contains("127.0.0.1") || url.contains("localhost")) &&
            !baseHost.contains("127.0.0.1") && !baseHost.contains("localhost")
        ) {
            url = url.replace("http://127.0.0.1:8011", baseHost)
                .replace("https://127.0.0.1:8011", baseHost)
                .replace("http://localhost:8011", baseHost)
                .replace("https://localhost:8011", baseHost)
                .replace("http://127.0.0.1", baseHost)
                .replace("https://127.0.0.1", baseHost)
                .replace("http://localhost", baseHost)
                .replace("https://localhost", baseHost)
        }

        val isLocalDev = BASE_URL.startsWith("http://") ||
                url.contains(":8011") ||
                url.contains("192.168.") ||
                url.contains("10.0.2.2")

        if (isLocalDev) {
            // Local development: jangan gunakan https karena port 8011 / LAN server tidak memakai SSL
            if (url.startsWith("https://") && (url.contains(":8011") || url.contains("192.168.") || url.contains("10.0.2.2") || url.contains("localhost") || url.contains("127.0.0.1"))) {
                url = url.replaceFirst("https://", "http://")
            }
        } else {
            // Production: pastikan https
            if (url.startsWith("http://")) {
                url = url.replaceFirst("http://", "https://")
            }
        }

        return url
    }
}




