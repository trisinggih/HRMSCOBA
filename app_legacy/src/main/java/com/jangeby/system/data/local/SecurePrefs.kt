package com.jangeby.system.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Wrapper penyimpanan aman token & profil employee menggunakan
 * EncryptedSharedPreferences (androidx.security:security-crypto).
 */
class SecurePrefs(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var employeeName: String?
        get() = prefs.getString(KEY_NAME, null)
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    var employeeEmail: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    var employeeCompany: String?
        get() = prefs.getString(KEY_COMPANY, null)
        set(value) = prefs.edit().putString(KEY_COMPANY, value).apply()

    var employeePosition: String?
        get() = prefs.getString(KEY_POSITION, null)
        set(value) = prefs.edit().putString(KEY_POSITION, value).apply()

    
    var employeeDivision: String?
        get() = prefs.getString(KEY_EMP_DIVISION, null)
        set(value) = prefs.edit().putString(KEY_EMP_DIVISION, value).apply()
        
    var employeeDepartment: String?
        get() = prefs.getString(KEY_EMP_DEPARTMENT, null)
        set(value) = prefs.edit().putString(KEY_EMP_DEPARTMENT, value).apply()

    var employeeBranch: String?
        get() = prefs.getString(KEY_BRANCH, null)
        set(value) = prefs.edit().putString(KEY_BRANCH, value).apply()

    var employeeId: String?
        get() = prefs.getString(KEY_EMP_ID, null)
        set(value) = prefs.edit().putString(KEY_EMP_ID, value).apply()

    var employeeBranchId: Long
        get() = prefs.getLong(KEY_BRANCH_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_BRANCH_ID, value).apply()

    var dbId: Long
        get() = prefs.getLong(KEY_DB_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_DB_ID, value).apply()

    var employeePhotoUrl: String?
        get() = prefs.getString(KEY_PHOTO_URL, null)
        set(value) = prefs.edit().putString(KEY_PHOTO_URL, value).apply()

    var canClockInAnywhere: Boolean
        get() = prefs.getBoolean(KEY_CAN_CLOCK_IN_ANYWHERE, false)
        set(value) = prefs.edit().putBoolean(KEY_CAN_CLOCK_IN_ANYWHERE, value).apply()

    /** Simpan hasil login sekaligus: token + profil. */
    fun saveSession(token: String, name: String?, email: String?, position: String?, branch: String?, branchId: Long = -1L, employeeId: String?, dbId: Long = -1L, photoUrl: String? = null, canClockInAnywhere: Boolean = false) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .putString(KEY_POSITION, position)
            .putString(KEY_BRANCH, branch)
            .putLong(KEY_BRANCH_ID, branchId)
            .putString(KEY_EMP_ID, employeeId)
            .putLong(KEY_DB_ID, dbId)
            .putString(KEY_PHOTO_URL, photoUrl)
            .putBoolean(KEY_CAN_CLOCK_IN_ANYWHERE, canClockInAnywhere)
            .apply()
    }

    val isLoggedIn: Boolean
        get() = !token.isNullOrBlank()

    /** Hapus seluruh data sesi (logout). */
    fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val FILE_NAME = "hrms_secure_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_NAME = "employee_name"
        private const val KEY_EMAIL = "employee_email"
        private const val KEY_COMPANY = "employee_company"
        private const val KEY_POSITION = "employee_position"
        private const val KEY_BRANCH = "employee_branch"
        private const val KEY_EMP_DIVISION = "employee_division"
        private const val KEY_EMP_DEPARTMENT = "employee_department"
        private const val KEY_BRANCH_ID = "employee_branch_id"
        private const val KEY_EMP_ID = "employee_id"
        private const val KEY_DB_ID = "employee_db_id"
        private const val KEY_PHOTO_URL = "employee_photo_url"
        private const val KEY_CAN_CLOCK_IN_ANYWHERE = "key_can_clock_in_anywhere"
    }
}
