package com.jangeby.system

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jangeby.system.data.local.AppSettings
import com.jangeby.system.navigation.*
import com.jangeby.system.ui.announcement.AnnouncementScreen
import com.jangeby.system.ui.approval.ApprovalScreen
import com.jangeby.system.ui.asset.AssetScreen
import com.jangeby.system.ui.attendance.AttendanceScreen
import com.jangeby.system.ui.auth.LoginScreen
import com.jangeby.system.ui.dashboard.DashboardScreen
import com.jangeby.system.ui.checkpoint.CheckPointScreen
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
                    onNavigateToAnnouncement = { navController.navigate(AnnouncementRoute) },
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
                    }
                )
            }
            
            composable<MapAttendanceRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<MapAttendanceRoute>()
                com.jangeby.system.ui.attendance.MapAttendanceScreen(
                    action = route.action,
                    onNavigateBack = {
                        navController.navigate(AttendanceRoute) {
                            popUpTo(AttendanceRoute) { inclusive = false }
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

            composable<AnnouncementRoute> {
                AnnouncementScreen(onNavigateBack = { navController.popBackStack() })
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
    }
}
