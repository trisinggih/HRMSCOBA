package com.jangeby.system.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object DateTimeUtils {

    /**
     * Waktu Indonesia Barat (WIB) / GMT+7
     */
    val JAKARTA_TIMEZONE: TimeZone = runCatching { TimeZone.of("Asia/Jakarta") }
        .getOrElse { runCatching { TimeZone.of("+07:00") }.getOrElse { TimeZone.currentSystemDefault() } }

    /**
     * Mendapatkan waktu saat ini dalam zona GMT+7
     */
    fun nowInJakarta(): LocalDateTime {
        return Clock.System.now().toLocalDateTime(JAKARTA_TIMEZONE)
    }

    /**
     * Mendapatkan tanggal hari ini dalam zona GMT+7 dengan format YYYY-MM-DD
     */
    fun todayDateString(): String {
        val now = nowInJakarta()
        return "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}"
    }

    /**
     * Format jam/menit (HH:mm) dengan kepastian zona GMT+7 jika input berupa ISO string
     */
    fun formatTimeOnly(timeOrIsoStr: String?): String {
        if (timeOrIsoStr.isNullOrBlank()) return "-"
        val clean = timeOrIsoStr.trim()
        if (clean.matches(Regex("""^\d{1,2}:\d{2}(:\d{2})?$"""))) {
            return clean.take(5)
        }
        val dt = parseToLocalDateTime(clean)
        if (dt != null) {
            val h = dt.hour.toString().padStart(2, '0')
            val m = dt.minute.toString().padStart(2, '0')
            return "$h:$m"
        }
        return clean.take(5)
    }

    private val MONTH_NAMES_SHORT = listOf("", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Ags", "Sep", "Okt", "Nov", "Des")
    private val MONTH_NAMES_FULL = listOf("", "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")

    /**
     * Parse date string into LocalDateTime, correctly handling UTC ISO strings (with 'Z' or offset)
     * by converting them strictly to GMT+7 (Asia/Jakarta).
     * If the input is already a local datetime string (no timezone info), it parses it as-is without shifting.
     */
    fun parseToLocalDateTime(dateStr: String?): LocalDateTime? {
        if (dateStr.isNullOrBlank()) return null
        val clean = dateStr.trim()

        // 1. Check if ISO string with UTC 'Z' or offset (+HH:mm or -HH:mm after time)
        val hasTimezone = clean.endsWith("Z", ignoreCase = true) ||
            (clean.contains("T") && (clean.contains("+") || clean.substringAfter("T").contains("-")))

        if (hasTimezone) {
            val parsedInstant = runCatching { Instant.parse(clean) }.getOrNull()
            if (parsedInstant != null) {
                return runCatching { parsedInstant.toLocalDateTime(JAKARTA_TIMEZONE) }.getOrNull()
            }
        }

        // 2. Check if local datetime format (e.g., "2026-10-14 14:30:00" or "2026-10-14T14:30:00")
        val withT = clean.replace(" ", "T")
        if (withT.contains("T")) {
            val withoutNanos = withT.substringBefore(".")
            val parsedDt = runCatching { LocalDateTime.parse(withoutNanos) }.getOrNull()
            if (parsedDt != null) return parsedDt
        }

        // 3. Date only (e.g., "2026-10-14")
        return runCatching {
            val ld = LocalDate.parse(clean.take(10))
            LocalDateTime(ld.year, ld.monthNumber, ld.dayOfMonth, 0, 0)
        }.getOrNull()
    }

    /**
     * Format into "dd-MMM-yyyy HH:mm" (or "dd-MMM-yyyy" if input was date-only).
     * Example: "14-Okt-2026 14:30" or "14-Okt-2026"
     */
    fun formatDateDMY(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "-"
        val clean = dateStr.trim()
        val dt = parseToLocalDateTime(clean) ?: return clean

        val day = dt.dayOfMonth.toString().padStart(2, '0')
        val monthStr = if (dt.monthNumber in 1..12) MONTH_NAMES_SHORT[dt.monthNumber] else dt.monthNumber.toString()
        val year = dt.year

        // Determine if original string had a time component
        val hasTime = clean.contains("T") || clean.contains(":") || clean.contains(" ")
        return if (hasTime) {
            val hour = dt.hour.toString().padStart(2, '0')
            val min = dt.minute.toString().padStart(2, '0')
            "$day-$monthStr-$year $hour:$min"
        } else {
            "$day-$monthStr-$year"
        }
    }

    /**
     * Format into "dd-MMM-yyyy HH:mm" (or "dd-MMM-yyyy" if input was date-only).
     * Used by ApprovalScreen: e.g. "14-Okt-2026 14:30"
     */
    fun formatDisplayDate(dateStr: String?): String {
        return formatDateDMY(dateStr)
    }

    /**
     * Format into "dd-MMM-yyyy" (always date-only, e.g. "14-Okt-2026").
     * Used for approval dates (cuti, sakit, lembur, izin, tukar hari/shift, dll).
     */
    fun formatDateOnlyDMY(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "-"
        val clean = dateStr.trim()
        if (clean.length >= 10 && clean.take(10).matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
            val parts = clean.take(10).split("-")
            val y = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            val d = parts[2].toIntOrNull()
            if (y != null && m != null && d != null) {
                val day = d.toString().padStart(2, '0')
                val monthStr = if (m in 1..12) MONTH_NAMES_SHORT[m] else m.toString()
                return "$day-$monthStr-$y"
            }
        }
        val dt = parseToLocalDateTime(clean) ?: return clean.take(10)

        val day = dt.dayOfMonth.toString().padStart(2, '0')
        val monthStr = if (dt.monthNumber in 1..12) MONTH_NAMES_SHORT[dt.monthNumber] else dt.monthNumber.toString()
        val year = dt.year
        return "$day-$monthStr-$year"
    }

    /**
     * Format notification time: "dd/MM/yyyy HH:mm"
     */
    fun formatNotifTime(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        val clean = dateStr.trim()
        val dt = parseToLocalDateTime(clean) ?: return clean

        val day = dt.dayOfMonth.toString().padStart(2, '0')
        val month = dt.monthNumber.toString().padStart(2, '0')
        val year = dt.year
        val hour = dt.hour.toString().padStart(2, '0')
        val min = dt.minute.toString().padStart(2, '0')

        return "$day/$month/$year $hour:$min"
    }

    /**
     * Format announcement: "dd MMM yyyy, HH:mm"
     */
    fun formatDateTime(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "-"
        val clean = dateStr.trim()
        val dt = parseToLocalDateTime(clean) ?: return clean

        val day = dt.dayOfMonth.toString().padStart(2, '0')
        val monthStr = if (dt.monthNumber in 1..12) MONTH_NAMES_SHORT[dt.monthNumber] else dt.monthNumber.toString()
        val year = dt.year
        val hour = dt.hour.toString().padStart(2, '0')
        val min = dt.minute.toString().padStart(2, '0')

        return "$day $monthStr $year, $hour:$min"
    }

    /**
     * Format into "d-MMMM-yyyy" (e.g. "15-Oktober-2026") strictly without time component.
     * Takes date part only (yyyy-MM-dd) to prevent timezone hour shifts.
     */
    fun formatDateIndoFull(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "-"
        val clean = dateStr.trim().take(10)
        val parts = clean.split("-")
        if (parts.size == 3) {
            val year = parts[0]
            val monthIdx = parts[1].toIntOrNull() ?: 1
            val day = parts[2].padStart(2, '0')
            val monthName = if (monthIdx in 1..12) MONTH_NAMES_FULL[monthIdx] else parts[1]
            return "$day-$monthName-$year"
        }
        return clean
    }
}
