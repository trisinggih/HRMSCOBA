@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.jangeby.system.notification

import com.jangeby.system.data.local.AppSettings
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.FcmTokenRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.*
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDefaults

actual object FcmManager {
    private const val TOKEN_KEY = "ios_fcm_registration_token"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var syncing: Job? = null
    private var syncedSession: String? = null
    private var syncedToken: String? = null

    actual fun syncToken() {
        // Swift owns Firebase/APNs; Kotlin owns the authenticated server request.
        NSNotificationCenter.defaultCenter.postNotificationName("HrmsRequestPushTokenSync", null)
        uploadCachedToken()
    }

    internal fun acceptToken(token: String) {
        if (token.isBlank()) return
        NSUserDefaults.standardUserDefaults.setObject(token, TOKEN_KEY)
        uploadCachedToken()
    }

    private fun uploadCachedToken() {
        val session = AppSettings().token?.takeIf { it.isNotBlank() } ?: return
        val token = NSUserDefaults.standardUserDefaults.stringForKey(TOKEN_KEY)?.takeIf { it.isNotBlank() } ?: return
        if (syncedSession == session && syncedToken == token) return
        syncing?.cancel()
        syncing = scope.launch {
            repeat(5) { attempt ->
                if (AppSettings().token != session) return@launch
                try {
                    val response = ApiClient.create().post(ApiClient.BASE_URL + "employee-auth/fcm-token") {
                        contentType(ContentType.Application.Json)
                        setBody(FcmTokenRequest(fcmToken = token))
                    }
                    if (response.status.value in 200..299) {
                        syncedSession = session; syncedToken = token
                        return@launch
                    }
                    if (response.status.value in listOf(401, 403, 422)) return@launch
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { /* Retry a temporary network failure without logging tokens. */ }
                delay(1000L shl attempt)
            }
        }
    }
}

// Exported to Swift as IosPushBridgeKt.acceptIosPushToken(token:).
fun acceptIosPushToken(token: String) {
    FcmManager.acceptToken(token)
}
