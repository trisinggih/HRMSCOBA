package com.jangeby.system.ui.payslip

import com.jangeby.system.data.remote.*
import kotlinx.serialization.json.*
import kotlin.math.round

fun JsonObject?.text(key: String): String? = (this?.get(key) as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
fun JsonObject?.number(key: String): Double? = text(key)?.toDoubleOrNull()
fun JsonObject?.obj(key: String): JsonObject? = this?.get(key) as? JsonObject

object PayslipPresentation {
    private val json = Json { ignoreUnknownKeys = true }
    fun attendance(item: PayslipItem): PayslipAttendance? {
        val saved = item.breakdown.obj("attendance")?.let { json.decodeFromJsonElement<PayslipAttendance>(it) }
        val live = item.attendance ?: return saved
        return live.copy(
            sick = live.sick ?: saved?.sick ?: item.breakdown.obj("variables").number("total_sick_days"),
            permit = live.permit ?: saved?.permit ?: item.breakdown.obj("variables").number("total_permit_days"),
            permitCutDays = live.permitCutDays ?: saved?.permitCutDays ?: item.breakdown.obj("variables").number("total_permit_cut_days"),
            holiday = live.holiday ?: saved?.holiday,
            cuti = live.cuti ?: saved?.cuti,
            workDays = live.workDays ?: saved?.workDays
        )
    }
    fun leaveDays(att: PayslipAttendance): Double = ((att.cuti ?: 0.0) - (att.sick ?: 0.0) - (att.permitCutDays ?: 0.0)).coerceAtLeast(0.0)
    fun count(value: Double?): String {
        val n = round((value ?: 0.0) * 100) / 100
        return (if (n % 1.0 == 0.0) n.toLong().toString() else n.toString()).replace('.', ',')
    }
    fun overtimeHours(item: PayslipItem): Double? {
        val minutes = item.breakdown.obj("overtime").number("minutes")
            ?: item.breakdown.obj("variables").number("total_overtime_minutes") ?: item.attendance?.overtimeMinutes
        return minutes?.let { it / 60 } ?: item.breakdown.obj("variables").number("total_overtime_hours")
    }
    fun isDeduction(item: PayslipDetailItem): Boolean {
        val type = item.type.orEmpty().lowercase()
        return type in listOf("deduction", "tax", "bpjs") || type.contains("potong") || type.contains("deduct") || type.contains("kasbon") || (item.amount ?: 0.0) < 0
    }
    fun status(raw: String?): String = when (raw?.lowercase()) {
        "present", "hadir" -> "Hadir"
        "late", "terlambat" -> "Terlambat"
        "absent", "alpha", "alpa" -> "Alpa"
        "leave", "cuti" -> "Cuti"
        "permit", "permission", "izin" -> "Izin"
        "early_leave" -> "Izin Pulang Awal"
        "sick", "sakit" -> "Sakit"
        "day_off", "off", "libur" -> "Libur"
        "holiday" -> "Libur Perusahaan"
        "business", "business_trip" -> "Perjalanan Dinas"
        else -> raw?.replace('_', ' ') ?: "-"
    }
}
