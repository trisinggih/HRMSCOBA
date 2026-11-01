package com.jangeby.system.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.jangeby.system.data.local.AppSettings
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.FcmTokenRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

actual object FcmManager {
    private const val TAG = "FcmManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    actual fun syncToken() {
        try {
            val token = AppSettings().token
            if (token.isNullOrBlank()) {
                Log.d(TAG, "User not authenticated yet. Skipping FCM token sync.")
                return
            }

            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                val fcmToken = task.result
                if (fcmToken.isNullOrBlank()) {
                    Log.w(TAG, "FCM token is empty")
                    return@addOnCompleteListener
                }

                Log.d(TAG, "Auto updating FCM token: $fcmToken")

                scope.launch {
                    try {
                        val client = ApiClient.create()
                        val response = client.post(ApiClient.BASE_URL + "employee-auth/fcm-token") {
                            contentType(ContentType.Application.Json)
                            setBody(FcmTokenRequest(fcmToken = fcmToken))
                        }
                        Log.d(TAG, "FCM token synced to server with status: ${response.status}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to auto update FCM token to server", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating FCM token sync", e)
        }
    }
}
