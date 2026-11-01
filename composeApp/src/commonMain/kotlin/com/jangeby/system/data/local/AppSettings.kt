package com.jangeby.system.data.local

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

/**
 * Pengganti SharedPreferences / UserDefaults.
 * Factory untuk createSettings di-inject via expect/actual agar bisa
 * menggunakan EncryptedSharedPreferences di Android dan Keychain di iOS (nanti).
 */
expect fun createSettings(): Settings

class AppSettings(private val settings: Settings = createSettings()) {

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER = "user_json"
        private const val KEY_USERNAME = "user_name"
        private const val KEY_COMPANY = "company_name"
        private const val KEY_COMPANY_LOGO = "company_logo_url"
        private const val KEY_POSITION = "user_position"
        private const val KEY_EMPLOYEE_ID = "employee_db_id"
        private const val KEY_BRANCH_ID = "employee_branch_id"
        private const val KEY_PHOTO = "user_photo_url"
        private const val KEY_HAS_SUBORDINATES = "has_subordinates"
        private const val KEY_SUBORDINATES_COUNT = "subordinates_count"
        private const val KEY_LAST_SEEN_MILESTONE_POPUP_DATE = "last_seen_milestone_popup_date_v4"
    }

    var employeeId: Long
        get() = settings.getLong(KEY_EMPLOYEE_ID, 0L)
        set(value) { settings[KEY_EMPLOYEE_ID] = value }

    var employeeBranchId: Long
        get() = settings.getLong(KEY_BRANCH_ID, -1L)
        set(value) { settings[KEY_BRANCH_ID] = value }

    var token: String?
        get() = settings[KEY_TOKEN]
        set(value) {
            if (value != null) settings[KEY_TOKEN] = value
            else settings.remove(KEY_TOKEN)
        }

    var userJson: String?
        get() = settings[KEY_USER]
        set(value) {
            if (value != null) settings[KEY_USER] = value
            else settings.remove(KEY_USER)
        }

    var userName: String?
        get() = settings[KEY_USERNAME]
        set(value) {
            if (value != null) settings[KEY_USERNAME] = value
            else settings.remove(KEY_USERNAME)
        }

    var companyName: String?
        get() = settings[KEY_COMPANY]
        set(value) {
            if (value != null) settings[KEY_COMPANY] = value
            else settings.remove(KEY_COMPANY)
        }

    var companyLogoUrl: String?
        get() = settings[KEY_COMPANY_LOGO]
        set(value) {
            if (value != null) settings[KEY_COMPANY_LOGO] = value
            else settings.remove(KEY_COMPANY_LOGO)
        }

    var userPosition: String?
        get() = settings[KEY_POSITION]
        set(value) {
            if (value != null) settings[KEY_POSITION] = value
            else settings.remove(KEY_POSITION)
        }

    var userPhotoUrl: String?
        get() = com.jangeby.system.data.remote.ApiClient.sanitizeImageUrl(settings[KEY_PHOTO])
        set(value) {
            val sanitized = com.jangeby.system.data.remote.ApiClient.sanitizeImageUrl(value)
            if (sanitized != null) settings[KEY_PHOTO] = sanitized
            else settings.remove(KEY_PHOTO)
        }

    var hasSubordinates: Boolean
        get() = settings.getBoolean(KEY_HAS_SUBORDINATES, false)
        set(value) { settings[KEY_HAS_SUBORDINATES] = value }

    var subordinatesCount: Int
        get() = settings.getInt(KEY_SUBORDINATES_COUNT, 0)
        set(value) { settings[KEY_SUBORDINATES_COUNT] = value }

    var lastSeenMilestonePopupDate: String?
        get() = settings[KEY_LAST_SEEN_MILESTONE_POPUP_DATE]
        set(value) {
            if (value != null) settings[KEY_LAST_SEEN_MILESTONE_POPUP_DATE] = value
            else settings.remove(KEY_LAST_SEEN_MILESTONE_POPUP_DATE)
        }

    fun clear() {
        val cachedLogo = companyLogoUrl
        val cachedCompany = companyName
        settings.clear()
        if (cachedLogo != null) companyLogoUrl = cachedLogo
        if (cachedCompany != null) companyName = cachedCompany
    }
}
