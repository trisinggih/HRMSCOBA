package com.jangeby.system.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AttendanceSession(
    val date: String,
    @SerialName("active_date") val activeDate: String,
    @SerialName("regular_id") val regularId: Long? = null,
    @SerialName("opens_at") val opensAt: String? = null,
    @SerialName("can_clock_in") val canClockIn: Boolean = false,
    @SerialName("can_clock_out") val canClockOut: Boolean = false,
    @SerialName("clock_out_at") val clockOutAt: String? = null,
    @SerialName("planned_clock_out_at") val plannedClockOutAt: String? = null,
    @SerialName("overtime_punch_enabled") val overtimePunchEnabled: Boolean = false,
    @SerialName("can_start_overtime") val canStartOvertime: Boolean = false
)
