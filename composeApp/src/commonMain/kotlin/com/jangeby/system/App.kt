package com.jangeby.system

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jangeby.system.data.local.AppSettings
import com.jangeby.system.navigation.*
import com.jangeby.system.security.SecurityChecker
import com.jangeby.system.security.SecurityViolation
import com.jangeby.system.ui.announcement.AnnouncementScreen
import com.jangeby.system.ui.approval.ApprovalScreen
import com.jangeby.system.ui.asset.AssetScreen
import com.jangeby.system.ui.attendance.AttendanceScreen
import com.jangeby.system.ui.auth.LoginScreen
import com.jangeby.system.ui.auth.RegisterScreen
import com.jangeby.system.ui.auth.OtpVerificationScreen
import com.jangeby.system.ui.dashboard.DashboardScreen
import com.jangeby.system.ui.checkpoint.CheckPointScreen
import com.jangeby.system.ui.component.SecurityBlockerScreen
import com.jangeby.system.ui.kasbon.KasbonScreen
import com.jangeby.system.ui.leave.LeaveScreen
import com.jangeby.system.ui.notification.NotificationScreen
import com.jangeby.system.ui.overtime.OvertimeScreen
import com.jangeby.system.ui.payslip.PayslipScreen
import com.jangeby.system.ui.pengajuan.PengajuanScreen
import com.jangeby.system.ui.permit.PermitScreen
import com.jangeby.system.ui.personal.PersonalScreen
import com.jangeby.system.ui.profile.ProfileScreen
import com.jangeby.system.ui.reimburse.ReimburseScreen
import com.jangeby.system.ui.splash.SplashScreen
import com.jangeby.system.ui.task.TaskScreen
import com.jangeby.system.ui.theme.HRMSTheme
import org.koin.compose.koinInject

@Composable
fun App() {
    HRMSTheme {
        val navController = rememberNavController()
        val appSettings: AppSettings = koinInject()
        val securityViolation by SecurityChecker.violationState.collectAsState()

        LifecycleResumeEffect(Unit) {
            SecurityChecker.checkSecurity()
            onPauseOrDispose { }
        }

        LaunchedEffect(Unit) {
            SecurityChecker.checkSecurity()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = SplashRoute) {
            composable<SplashRoute> {
                SplashScreen(
                    onNavigateToLogin = {
                        navController.navigate(LoginRoute) {
                            popUpTo(SplashRoute) { inclusive = true }
                        }
                    },
                    onNavigateToDashboard = {
                        navController.navigate(DashboardRoute) {
                            popUpTo(SplashRoute) { inclusive = true }
                        }
                    }
                )
            }

            composable<LoginRoute> {
                LoginScreen(
                    onNavigateToDashboard = {
                        navController.navigate(DashboardRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(RegisterRoute)
                    }
                )
            }

            composable<RegisterRoute> {
                RegisterScreen(
                    onNavigateToLogin = { navController.popBackStack() },
                    onNavigateToOtp = { email, name ->
                        navController.navigate(OtpVerificationRoute(email = email, name = name))
                    }
                )
            }

            composable<OtpVerificationRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<OtpVerificationRoute>()
                OtpVerificationScreen(
                    email = route.email,
                    name = route.name,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDashboard = {
                        navController.navigate(DashboardRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    }
                )
            }
            
            composable<DashboardRoute> {
                DashboardScreen(
                    onLogout = {
                        com.jangeby.system.hardware.LocationTracker.stop()
                        appSettings.clear()
                        com.jangeby.system.data.remote.ApiClient.reset()
                        navController.navigate(LoginRoute) {
                            popUpTo(DashboardRoute) { inclusive = true }
                        }
                    },
                    onNavigateToAttendance = { navController.navigate(AttendanceRoute) },
                    onNavigateToLeave = { navController.navigate(LeaveRoute) },
                    onNavigateToOvertime = { navController.navigate(OvertimeRoute) },
                    onNavigateToMapAttendance = { action -> navController.navigate(MapAttendanceRoute(action)) },
                    onNavigateToPayslip = { navController.navigate(PayslipRoute) },
                    onNavigateToTask = { navController.navigate(TaskRoute) },
                    onNavigateToNotification = { navController.navigate(NotificationRoute) },
                    onNavigateToAnnouncement = { tab -> navController.navigate(AnnouncementRoute(initialTab = tab)) },
                    onNavigateToApproval = { navController.navigate(ApprovalRoute) },
                    onNavigateToKasbon = { navController.navigate(KasbonRoute) },
                    onNavigateToReimburse = { navController.navigate(ReimburseRoute) },
                    onNavigateToPermit = { navController.navigate(PermitRoute) },
                    onNavigateToPengajuan = { navController.navigate(PengajuanRoute) },
                    onNavigateToAsset = { navController.navigate(AssetRoute) },
                    onNavigateToCheckPoint = { navController.navigate(CheckPointRoute) },
                    onNavigateToPersonal = { navController.navigate(PersonalRoute) },
                    onNavigateToProfile = { navController.navigate(ProfileRoute) }
                )
            }

            composable<AttendanceRoute> {
                AttendanceScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMap = { action -> 
                        navController.navigate(MapAttendanceRoute(action))
                    },
                    onNavigateToOvertime = {
                        navController.navigate(OvertimeRoute)
                    },
                    onNavigateToAnnouncement = { tab ->
                        navController.navigate(AnnouncementRoute(initialTab = tab))
                    }
                )
            }
            
            composable<MapAttendanceRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<MapAttendanceRoute>()
                com.jangeby.system.ui.attendance.MapAttendanceScreen(
                    action = route.action,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSubmitSuccess = {
                        navController.navigate(AttendanceRoute) {
                            popUpTo(DashboardRoute) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            
            composable<LeaveRoute> {
                LeaveScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable<OvertimeRoute> {
                OvertimeScreen(onNavigateBack = { navController.popBackStack() })
            }
            
            composable<PayslipRoute> {
                PayslipScreen(onNavigateBack = { navController.popBackStack() })
            }
            
            composable<TaskRoute> {
                TaskScreen(onNavigateBack = { navController.popBackStack() })
            }
            
            composable<NotificationRoute> {
                NotificationScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable<AnnouncementRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<AnnouncementRoute>()
                AnnouncementScreen(
                    initialTab = route.initialTab,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<ApprovalRoute> {
                ApprovalScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable<KasbonRoute> {
                KasbonScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable<ReimburseRoute> {
                ReimburseScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable<PermitRoute> {
                PermitScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable<PengajuanRoute> {
                PengajuanScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable<AssetRoute> {
                AssetScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable<PersonalRoute> {
                PersonalScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable<CheckPointRoute> {
                CheckPointScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable<ProfileRoute> {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onLogout = {
                        com.jangeby.system.hardware.LocationTracker.stop()
                        appSettings.clear()
                        navController.navigate(LoginRoute) {
                            popUpTo(DashboardRoute) { inclusive = true }
                        }
                    }
                )
            }
        }

        if (securityViolation !is SecurityViolation.None) {
            SecurityBlockerScreen(
                violation = securityViolation,
                onOpenSettings = { SecurityChecker.openDeveloperSettings() },
                onRecheck = { SecurityChecker.checkSecurity() },
                onExitApp = { SecurityChecker.exitApp() }
            )
        }
    }
}
}
