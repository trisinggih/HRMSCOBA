package com.jangeby.system.navigation

import kotlinx.serialization.Serializable

@Serializable
object SplashRoute

@Serializable
object LoginRoute

@Serializable
object RegisterRoute

@Serializable
data class OtpVerificationRoute(
    val email: String,
    val name: String
)

@Serializable
object DashboardRoute

@Serializable
object AttendanceRoute

@Serializable
object LeaveRoute

@Serializable
object OvertimeRoute

@Serializable
object PayslipRoute

@Serializable
object TaskRoute

@Serializable
object NotificationRoute

@Serializable
data class AnnouncementRoute(val initialTab: Int = 0)

@Serializable
object ApprovalRoute

@Serializable
object KasbonRoute

@Serializable
object ReimburseRoute

@Serializable
object PermitRoute

@Serializable
object PengajuanRoute

@Serializable
object AssetRoute

@Serializable
object PersonalRoute

@Serializable
object ProfileRoute

@Serializable
data class MapAttendanceRoute(val action: String)

@Serializable
object CheckPointRoute
