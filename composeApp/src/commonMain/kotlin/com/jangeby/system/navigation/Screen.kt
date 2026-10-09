package com.jangeby.system.navigation

import kotlinx.serialization.Serializable

@Serializable
object SplashRoute

@Serializable
object LoginRoute

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
object AnnouncementRoute

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
