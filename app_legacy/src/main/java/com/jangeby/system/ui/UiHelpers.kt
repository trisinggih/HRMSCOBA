package com.jangeby.system.ui

import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException

/**
 * Utilitas UI kecil: ekstraksi pesan error yang ramah untuk ditampilkan.
 */
object UiHelpers {

    /**
     * Ubah Throwable menjadi pesan yang bisa langsung ditampilkan.
     * Berusaha membaca field {message} dari body error Laravel.
     */
    fun errorMessageFrom(context: Context, throwable: Throwable): String {
        return when (throwable) {
            is HttpException -> {
                val fallback = "Terjadi kesalahan server (${throwable.code()})"
                val body = try {
                    throwable.response()?.errorBody()?.string()
                } catch (ignored: Throwable) {
                    null
                }
                val message = body?.let { parseMessage(it) }
                if (message.isNullOrBlank()) fallback else message
            }
            is java.net.UnknownHostException ->
                "Tidak dapat terhubung ke server. Periksa koneksi internet."
            is java.net.ConnectException ->
                "Tidak dapat terhubung ke server. Pastikan backend aktif."
            is java.net.SocketTimeoutException ->
                "Waktu koneksi habis. Coba lagi."
            is IOException ->
                throwable.message ?: "Gagal terhubung ke server."
            else ->
                throwable.message ?: "Terjadi kesalahan yang tidak diketahui."
        }
    }

    /** Ambil nilai `message` dari body JSON Laravel bila ada. */
    private fun parseMessage(body: String): String? {
        return try {
            val obj: JsonObject = JsonParser.parseString(body).asJsonObject
            
            // Periksa apakah ada 'errors' (ValidationException dari Laravel)
            if (obj.has("errors") && obj.get("errors").isJsonObject) {
                val errors = obj.getAsJsonObject("errors")
                // Ambil pesan error pertama dari key apa pun
                if (errors.keySet().isNotEmpty()) {
                    val firstKey = errors.keySet().first()
                    val errorArray = errors.getAsJsonArray(firstKey)
                    if (errorArray.size() > 0) {
                        return errorArray.get(0).asString
                    }
                }
            }

            if (obj.has("message") && !obj.get("message").isJsonNull) {
                obj.get("message").asString
            } else null
        } catch (ignored: Throwable) {
            null
        }
    }

    /**
     * Format ISO date string into readable format.
     */
    fun formatDateTime(isoString: String): String {
        if (isoString.isBlank()) return ""
        try {
            // Coba parsing ISO 8601 dari Laravel
            var parsedDate: java.util.Date? = null
            val formats = arrayOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss"
            )
            for (format in formats) {
                try {
                    val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
                    if (format.endsWith("'Z'")) {
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }
                    parsedDate = sdf.parse(isoString)
                    if (parsedDate != null) break
                } catch (e: Exception) {
                    continue
                }
            }
            if (parsedDate != null) {
                val outFormat = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("id", "ID"))
                return outFormat.format(parsedDate)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return isoString
    }
}
