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
}




