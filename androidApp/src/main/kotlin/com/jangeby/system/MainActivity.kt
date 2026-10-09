package com.jangeby.system

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.FcmTokenRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request runtime permissions for notifications & location
        requestPermissionsIfNeeded()

        // Sync FCM token with backend
        syncFcmToken()

        setContent {
            App() // Dari shared composeApp
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1001)
        }
    }

    private fun syncFcmToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d("FCM", "Current FCM Token: $token")

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val client = ApiClient.create()
                        client.post(ApiClient.BASE_URL + "employee-auth/fcm-token") {
                            contentType(ContentType.Application.Json)
                            setBody(FcmTokenRequest(fcmToken = token))
                        }
                        Log.d("FCM", "FCM token synced to server")
                    } catch (e: Exception) {
                        Log.e("FCM", "Failed to sync token", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FCM", "Error initializing FCM", e)
        }
    }
}
