package com.jangeby.system.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

private var applicationContext: Context? = null

fun initSettings(context: Context) {
    applicationContext = context.applicationContext
}

fun getAppContext(): Context? = applicationContext

actual fun createSettings(): Settings {
    val context = applicationContext ?: throw IllegalStateException("Settings context not initialized. Call initSettings(context) first in Application class.")
    
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "hrms_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    return SharedPreferencesSettings(sharedPreferences)
}
