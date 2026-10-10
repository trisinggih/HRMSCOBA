package com.jangeby.system.di

import com.jangeby.system.data.local.AppSettings
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.ui.attendance.AttendanceViewModel
import com.jangeby.system.ui.auth.LoginViewModel
import com.jangeby.system.ui.leave.LeaveViewModel
import com.jangeby.system.ui.overtime.OvertimeViewModel
import com.jangeby.system.ui.pengajuan.PengajuanViewModel
import com.jangeby.system.ui.approval.ApprovalViewModel
import com.jangeby.system.ui.task.TaskViewModel
import com.jangeby.system.ui.asset.AssetViewModel
import com.jangeby.system.ui.kasbon.KasbonViewModel
import com.jangeby.system.ui.reimburse.ReimburseViewModel
import com.jangeby.system.ui.announcement.AnnouncementViewModel
import com.jangeby.system.ui.personal.PersonalViewModel
import com.jangeby.system.ui.permit.PermitViewModel
import com.jangeby.system.ui.payslip.PayslipViewModel
import com.jangeby.system.ui.profile.ProfileViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { AppSettings() }
    single { 
        ApiClient.setTokenProvider { get<AppSettings>().token }
        ApiClient.create() 
    }
    viewModel<LoginViewModel> { LoginViewModel(get()) }
    viewModel<LeaveViewModel> { LeaveViewModel() }
    viewModel<OvertimeViewModel> { OvertimeViewModel() }
    viewModel<AttendanceViewModel> { AttendanceViewModel(get()) }
    viewModel<PengajuanViewModel> { PengajuanViewModel() }
    viewModel<ApprovalViewModel> { ApprovalViewModel() }
    viewModel<TaskViewModel> { TaskViewModel(get()) }
    viewModel<AssetViewModel> { AssetViewModel() }
    viewModel<KasbonViewModel> { KasbonViewModel() }
    viewModel<ReimburseViewModel> { ReimburseViewModel() }
    viewModel<AnnouncementViewModel> { AnnouncementViewModel() }
    viewModel<PersonalViewModel> { PersonalViewModel() }
    viewModel<PermitViewModel> { PermitViewModel() }
    viewModel<PayslipViewModel> { PayslipViewModel() }
    viewModel<ProfileViewModel> { ProfileViewModel(get()) }
}

fun initKoin() {
    ApiClient.setTokenProvider { AppSettings().token }
    startKoin {
        modules(appModule)
    }
}
