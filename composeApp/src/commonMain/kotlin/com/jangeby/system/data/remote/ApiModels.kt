package com.jangeby.system.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

// ===================== COMMON =====================
@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val status: String? = null,
    val message: String? = null,
    val data: T? = null
) {
    val isSuccess: Boolean get() = success || status == "success" || data != null
}

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>? = null,
    val meta: PaginationMeta? = null
)

@Serializable
data class PaginationMeta(
    @SerialName("current_page") val currentPage: Int? = null,
    @SerialName("last_page") val lastPage: Int? = null,
    @SerialName("per_page") val perPage: Int? = null,
    val total: Int? = null
)

// ===================== AUTH =====================
@Serializable
data class CompanyLogoData(
    val name: String? = null,
    val logo: String? = null,
    val favicon: String? = null
)

@Serializable
data class FcmTokenRequest(@SerialName("fcm_token") val fcmToken: String)

@Serializable
data class BadgeResponse(
    @SerialName("pengajuan_count") val pengajuanCount: Int = 0,
    @SerialName("approval_count") val approvalCount: Int = 0,
    @SerialName("task_count") val taskCount: Int = 0,
    @SerialName("notification_count") val notificationCount: Int = 0,
    @SerialName("info_count") val infoCount: Int = 0,
    @SerialName("overtime_count") val overtimeCount: Int = 0,
    @SerialName("kasbon_count") val kasbonCount: Int = 0,
    @SerialName("reimburse_count") val reimburseCount: Int = 0,
    @SerialName("permit_count") val permitCount: Int = 0,
    @SerialName("pengajuan_sakit_count") val pengajuanSakitCount: Int = 0,
    @SerialName("pengajuan_ijin_count") val pengajuanIjinCount: Int = 0,
    @SerialName("pengajuan_cuti_count") val pengajuanCutiCount: Int = 0,
    @SerialName("pengajuan_tukar_hari_count") val pengajuanTukarHariCount: Int = 0
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    @SerialName("device_name") val deviceName: String = "Mobile",
    @SerialName("device_id") val deviceId: String? = null
)

@Serializable
data class LoginData(
    val token: String,
    val employee: EmployeeProfile? = null
)

@Serializable
data class EmployeeProfile(
    val id: Long,
    @SerialName("employee_id") val employeeId: String? = null,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    @SerialName("birth_date") val birthDate: String? = null,
    val company: String? = null,
    val position: String? = null,
    @SerialName("branch_id") val branchId: Long? = null,
    val branch: String? = null,
    val division: String? = null,
    val department: String? = null,
    @SerialName("can_clock_in_anywhere") val canClockInAnywhere: Boolean = false,
    @SerialName("photo_url") val photoUrl: String? = null,
    val documents: List<EmployeeDocument>? = null,
    val educations: List<EmployeeEducation>? = null,
    val trainings: List<EmployeeTraining>? = null
)

@Serializable
data class ChangePasswordRequest(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
    @SerialName("new_password_confirmation") val newPasswordConfirmation: String
)

@Serializable
data class EmployeeDocument(
    val id: Long,
    @SerialName("document_name") val documentName: String? = null,
    @SerialName("document_type") val documentType: String? = null,
    @SerialName("file_path") val filePath: String? = null
)

@Serializable
data class EmployeeEducation(
    val id: Long,
    @SerialName("institution_name") val institutionName: String? = null,
    val degree: String? = null,
    val major: String? = null,
    @SerialName("start_year") val startYear: String? = null,
    @SerialName("end_year") val endYear: String? = null
)

@Serializable
data class EmployeeTraining(
    val id: Long,
    @SerialName("training_name") val trainingName: String? = null,
    val institution: String? = null,
    val year: String? = null,
    @SerialName("certificate_path") val certificatePath: String? = null
)

// ===================== ATTENDANCE =====================
@Serializable
data class ClockInRequest(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double,
    @SerialName("device_info") val deviceInfo: String? = null,
    val note: String? = null,
    @SerialName("face_photo") val facePhoto: String? = null
)

@Serializable
data class ClockOutRequest(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double,
    @SerialName("device_info") val deviceInfo: String? = null,
    @SerialName("face_photo") val facePhoto: String? = null
)

@Serializable
data class RadiusPermissionData(
    @SerialName("can_clock_in_anywhere") val canClockInAnywhere: Boolean
)

@Serializable
data class BranchLocationData(
    val id: Long,
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("attendance_radius") val attendanceRadius: Double? = null
)

@Serializable
data class BreakRequest(
    @SerialName("device_info") val deviceInfo: String? = null
)

@Serializable
data class RegisterFaceRequest(
    @SerialName("face_photo") val facePhoto: String
)

@Serializable
data class FaceStatusData(
    @SerialName("has_face") val hasFace: Boolean,
    @SerialName("face_registered_at") val faceRegisteredAt: String? = null,
    @SerialName("face_photo_url") val facePhotoUrl: String? = null
)

@Serializable
data class AttendanceItem(
    val id: Long? = null,
    @SerialName("employee_id") val employeeId: Long? = null,
    @SerialName("shift_id") val shiftId: Long? = null,
    @SerialName("shift_name") val shiftName: String? = null,
    val date: String? = null,
    @SerialName("clock_in_at") val clockInAt: String? = null,
    @SerialName("clock_out_at") val clockOutAt: String? = null,
    @SerialName("break_start_at") val breakStartAt: String? = null,
    @SerialName("break_end_at") val breakEndAt: String? = null,
    @SerialName("break_minutes") val breakMinutes: Int? = null,
    @SerialName("working_hours") val workingHours: kotlinx.serialization.json.JsonElement? = null,
    @SerialName("late_minutes") val lateMinutes: Int? = null,
    @SerialName("overtime_minutes") val overtimeMinutes: Int? = null,
    val status: String? = null,
    val notes: String? = null,
    @SerialName("late_reason") val lateReason: String? = null,
    @SerialName("is_face_verified") val isFaceVerified: Boolean? = null,
    @SerialName("shift_start") val shiftStart: String? = null,
    @SerialName("shift_end") val shiftEnd: String? = null
) {
    val clockIn: String? get() = clockInAt
    val clockOut: String? get() = clockOutAt
}

@Serializable
data class CalendarItem(
    val date: String? = null,
    @SerialName("day_of_week") val dayOfWeek: Int? = null,
    @SerialName("is_holiday") val isHoliday: Boolean? = null,
    @SerialName("has_approved_overtime") val hasApprovedOvertime: Boolean? = null,
    val shift: ShiftItem? = null,
    val attendance: AttendanceDetail? = null,
    @SerialName("late_reason") val lateReason: String? = null
)

@Serializable
data class ShiftItem(
    val id: Long,
    val name: String? = null,
    @SerialName("clock_in") val startTime: String? = null,
    @SerialName("clock_out") val endTime: String? = null
)

@Serializable
data class AttendanceDetail(
    val id: Long? = null,
    @SerialName("clock_in") val clockIn: String? = null,
    @SerialName("clock_out") val clockOut: String? = null,
    @SerialName("overtime_in") val overtimeIn: String? = null,
    @SerialName("overtime_out") val overtimeOut: String? = null,
    @SerialName("late_minutes") val lateMinutes: Int? = null,
    val status: String? = null,
    val notes: String? = null,
    @SerialName("late_reason") val lateReason: String? = null
)

// ===================== LEAVE =====================
@Serializable
data class LeaveBalanceItem(
    @SerialName("leave_type_id") val leaveTypeId: Long = 0L,
    @SerialName("leave_type_name") val leaveTypeName: String? = null,
    @SerialName("leave_type_code") val leaveTypeCode: String? = null,
    val quota: Double = 0.0,
    val used: Double = 0.0,
    val remaining: Double = 0.0,
    val year: Int? = null
)

@Serializable
data class LeaveTypeItem(
    val id: Long,
    val name: String? = null,
    val code: String? = null,
    val category: String? = null,
    @SerialName("default_days") val defaultDays: Int? = null,
    @SerialName("is_paid") val isPaid: Boolean? = null
)

@Serializable
data class LeaveRequestItem(
    val id: Long,
    @SerialName("leave_type_id") val leaveTypeId: Long,
    @SerialName("employee_id") val employeeId: Long,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("total_days") val totalDays: Int? = null,
    val reason: String? = null,
    val status: String? = null,
    val attachment: String? = null,
    @SerialName("leave_type") val leaveType: LeaveTypeItem? = null,
    val approval: ApprovalItem? = null
)

@Serializable
data class LeaveRequestCreate(
    @SerialName("leave_type_id") val leaveTypeId: Long,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    val reason: String
)

// ===================== NOTIFICATION =====================
@Serializable
data class NotificationItem(
    val id: Long,
    val type: String? = null,
    val title: String? = null,
    val body: String? = null,
    val data: String? = null,
    @SerialName("is_read") val isRead: Boolean? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AnnouncementData(
    val id: Long,
    val title: String,
    val body: String? = null,
    @SerialName("attachment_type") val attachmentType: String? = null,
    @SerialName("attachment_url") val attachmentUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("is_read") var isRead: Boolean = false
)

// ===================== APPROVAL =====================
@Serializable
data class ApprovalWorkflow(
    val id: Long,
    val name: String? = null,
    val steps: List<ApprovalStep>? = null
)

@Serializable
data class ApprovalStep(
    val id: Long,
    val order: Int? = null,
    @SerialName("approver_type") val approverType: String? = null,
    @SerialName("approver_name") val approverName: String? = null,
    @SerialName("expected_approver_name") val expectedApproverName: String? = null,
    @SerialName("is_optional") val isOptional: Boolean? = null
)

@Serializable
data class ApprovalLog(
    val id: Long,
    @SerialName("approval_step_id") val stepId: Long? = null,
    val action: String? = null,
    @SerialName("approver_name") val approverName: String? = null,
    val note: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class RequesterData(
    val id: Long? = null,
    val name: String? = null,
    @SerialName("employee_id") val employeeId: String? = null,
    val photo: String? = null
)

@Serializable
data class ApprovalItem(
    val id: Long,
    val module: String? = null,
    val status: String? = null,
    @SerialName("current_step") val currentStep: Int? = null,
    @SerialName("requested_by") val requestedBy: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val workflow: ApprovalWorkflow? = null,
    val logs: List<ApprovalLog>? = null,
    val payload: JsonElement? = null,
    @SerialName("attachment_url") val attachmentUrl: String? = null,
    val requester: RequesterData? = null
) {
    val payloadObject: JsonObject?
        get() = payload as? JsonObject

    val moduleLabel: String
        get() = when (module?.lowercase()?.trim()) {
            "leave" -> "Cuti"
            "sick_leave", "sick" -> "Sakit"
            "late", "late_request", "late_reason", "keterlambatan" -> "Terlambat"
            "permission", "permit" -> "Izin"
            "overtime" -> "Lembur"
            "attendance_correction" -> "Koreksi Absen"
            "shift_exchange", "shift_swap" -> "Tukar Shift"
            "workday_swap" -> "Tukar Hari"
            "loan_request", "kasbon", "loan" -> "Kasbon"
            "reimbursement", "reimburse" -> "Reimburse"
            else -> module?.replaceFirstChar { it.uppercase() }?.replace("_", " ") ?: "Pengajuan"
        }

    val parsedDescription: String
        get() {
            val p = payloadObject
            val info = StringBuilder()
            val leaveType = p?.get("leave_type")?.jsonPrimitive?.contentOrNull
            if (!leaveType.isNullOrEmpty()) {
                info.append("Cuti: $leaveType\n")
                val sd = p["start_date"]?.jsonPrimitive?.contentOrNull?.take(10)
                val ed = p["end_date"]?.jsonPrimitive?.contentOrNull?.take(10)
                val days = p["total_days"]?.jsonPrimitive?.contentOrNull
                if (sd != null && ed != null) {
                    info.append("Tanggal: $sd s/d $ed ($days Hari)\n")
                }
            }

            val amount = p?.get("amount")?.jsonPrimitive?.contentOrNull
            val type = p?.get("type")?.jsonPrimitive?.contentOrNull
            if (!type.isNullOrEmpty()) {
                info.append("Tipe: $type\n")
            }
            if (!amount.isNullOrEmpty()) {
                val formattedAmount = formatNominalRupiah(amount)
                info.append("Nominal: $formattedAmount\n")
            }

            val workDate = p?.get("work_date")?.jsonPrimitive?.contentOrNull
            val swapToDate = p?.get("swap_to_date")?.jsonPrimitive?.contentOrNull
            if (!workDate.isNullOrEmpty() && !swapToDate.isNullOrEmpty()) {
                info.append("Tukar: $workDate -> $swapToDate\n")
            }

            val reason = (p?.get("reason")?.jsonPrimitive?.contentOrNull
                ?: p?.get("remark")?.jsonPrimitive?.contentOrNull
                ?: p?.get("note")?.jsonPrimitive?.contentOrNull
                ?: p?.get("description")?.jsonPrimitive?.contentOrNull)
            if (!reason.isNullOrEmpty()) {
                info.append("Keterangan: $reason")
            }

            return info.toString().trim()
        }

    private fun formatNominalRupiah(amountStr: String?): String {
        if (amountStr.isNullOrBlank()) return ""
        return try {
            val cleanStr = amountStr.replace(",", "")
            val doubleVal = cleanStr.toDoubleOrNull()
            if (doubleVal != null) {
                val rounded = doubleVal.toLong()
                val formatted = rounded.toString().reversed().chunked(3).joinToString(".").reversed()
                "Rp $formatted"
            } else {
                "Rp $amountStr"
            }
        } catch (e: Exception) {
            "Rp $amountStr"
        }
    }
}

@Serializable
data class ActionApprovalRequest(
    val note: String? = null,
    val tenor: String? = null
)

@Serializable
data class UnreadCountData(val count: Int)

// ===================== KASBON =====================
@Serializable
data class CreateLoanRequest(val amount: Double, val reason: String)

@Serializable
data class LoanRequestItem(
    val id: Long,
    val date: String,
    val amount: Double,
    @SerialName("installment_months") val installmentMonths: Int? = null,
    val reason: String? = null,
    val status: String,
    @SerialName("approved_at") val approvedAt: String? = null,
    @SerialName("rejected_at") val rejectedAt: String? = null,
    @SerialName("attachment_url") val attachmentUrl: String? = null,
    val approval: ApprovalItem? = null
)

// ===================== TASKS =====================
@Serializable
data class TaskItem(
    val id: Long,
    val title: String? = null,
    val description: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    val priority: String? = null,
    val progress: Int? = null,
    val status: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class TaskListData(
    val items: List<TaskItem>? = null,
    val pagination: TaskPagination? = null
)

@Serializable
data class TaskPagination(
    val total: Int? = null,
    @SerialName("per_page") val perPage: Int? = null,
    @SerialName("current_page") val currentPage: Int? = null,
    @SerialName("last_page") val lastPage: Int? = null
)

@Serializable
data class TaskBatchRequest(
    val date: String,
    val tasks: List<String>
)

@Serializable
data class TaskStatusRequest(
    val status: String
)

@Serializable
data class TaskCompleteAllRequest(
    val date: String
)

// ===================== ASSETS =====================
@Serializable
data class AssetItem(
    val id: Long,
    val name: String,
    @SerialName("serial_number") val serialNumber: String? = null,
    val type: String? = null,
    val status: String? = null,
    @SerialName("purchase_date") val purchaseDate: String? = null,
    val notes: String? = null,
    @SerialName("inventory_number") val inventoryNumber: String? = null
)

// ===================== OVERTIME =====================
@Serializable
data class OvertimeRequestItem(
    val id: Long,
    val date: String,
    @SerialName("requested_start_time") val requestedStartTime: String? = null,
    @SerialName("requested_end_time") val requestedEndTime: String? = null,
    val reason: String? = null,
    val status: String,
    val attachment: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val approval: ApprovalItem? = null
)

@Serializable
data class OvertimeRequestCreate(
    @SerialName("shift_id") val shiftId: Long,
    val date: String,
    val reason: String,
    @SerialName("requested_end_time") val requestedEndTime: String
)

// ===================== REIMBURSE =====================
@Serializable
data class ReimburseItem(
    val id: Long,
    val date: String? = null,
    val type: String? = null,
    val amount: Double? = null,
    val description: String? = null,
    val status: String? = null,
    val attachment: String? = null,
    val approval: ApprovalItem? = null,
    @SerialName("created_at") val createdAt: String? = null
)

// ===================== PENGAJUAN =====================
@Serializable
data class LateRequestData(val date: String, val reason: String)

@Serializable
data class SickLeaveRequestItem(
    val id: Long,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val reason: String? = null,
    val status: String? = null,
    @SerialName("medical_certificate") val medicalCertificate: String? = null,
    val approval: ApprovalItem? = null
)

@Serializable
data class LateRequestItem(
    val id: Long,
    val date: String? = null,
    val reason: String? = null,
    val status: String? = null,
    val approval: ApprovalItem? = null
)

@Serializable
data class WorkdaySwapRequestItem(
    val id: Long,
    @SerialName("work_date") val workDate: String? = null,
    @SerialName("swap_to_date") val swapToDate: String? = null,
    val reason: String? = null,
    val status: String? = null,
    val approval: ApprovalItem? = null
)

@Serializable
data class PermitRequestItem(
    val id: Long,
    val type: String? = null,
    val date: String? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    val reason: String? = null,
    val status: String? = null,
    val attachment: String? = null,
    val approval: ApprovalItem? = null
)

@Serializable
data class PermissionRequestCreate(
    val type: String = "izin",
    val date: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val reason: String
)

@Serializable
data class SickLeaveRequestCreate(
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    val reason: String
)

@Serializable
data class WorkdaySwapRequestCreate(
    @SerialName("work_date") val workDate: String,
    @SerialName("swap_to_date") val swapToDate: String,
    val reason: String
)


// ===================== PERSONAL DATA =====================
@Serializable
data class FamilyModel(
    val id: Int,
    @SerialName("employee_id") val employeeId: Int? = null,
    val name: String = "",
    val relation: String = "",
    val phone: String? = null,
    val gender: String? = null,
    val dob: String? = null,
    @SerialName("birth_date") val birthDate: String? = null,
    val profession: String? = null,
    val occupation: String? = null,
    val address: String? = null,
    @SerialName("domicile_address") val domicileAddress: String? = null,
    @SerialName("residential_address") val residentialAddress: String? = null,
    @SerialName("is_emergency_contact") val isEmergencyContact: Int? = 0,
    @SerialName("is_dependent") val isDependent: Boolean? = false
)

@Serializable
data class EducationModel(
    val id: Int,
    @SerialName("employee_id") val employeeId: Int? = null,
    val level: String = "",
    @SerialName("institution_name") val institutionName: String = "",
    val major: String? = null,
    @SerialName("start_year") val startYear: Int? = null,
    @SerialName("end_year") val endYear: Int? = null,
    val gpa: Double? = null
)

@Serializable
data class FamilyRequest(
    val name: String,
    val relation: String,
    val phone: String? = null,
    val gender: String = "other",
    val dob: String = "2000-01-01",
    val profession: String? = null,
    val address: String? = null,
    @SerialName("is_emergency_contact") val isEmergencyContact: Boolean = false
)

@Serializable
data class EducationRequest(
    @SerialName("institution_name") val institutionName: String,
    val level: String,
    val major: String? = null,
    @SerialName("start_year") val startYear: Int? = null,
    @SerialName("end_year") val endYear: Int? = null,
    val gpa: Double? = null
)

// ===================== LOCATION =====================
@Serializable
data class SyncLocationRequest(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null
)

// ===================== CHECKPOINT =====================
@Serializable
data class CheckPointItem(
    val id: Long,
    @SerialName("client_name") val clientName: String? = null,
    val purpose: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val address: String? = null,
    @SerialName("photo_path") val photoPath: String? = null,
    val photos: List<String>? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val employee: EmployeeProfile? = null
)

// ===================== PAYSLIP =====================
@Serializable
data class PayslipItem(
    val id: Long,
    @SerialName("payroll_period_id") val payrollPeriodId: Long? = null,
    @SerialName("employee_id") val employeeId: Long? = null,
    @SerialName("gross_salary") val grossSalary: Double? = null,
    @SerialName("total_allowance") val totalAllowance: Double? = null,
    @SerialName("total_overtime") val totalOvertime: Double? = null,
    @SerialName("total_bonus") val totalBonus: Double? = null,
    @SerialName("total_commission") val totalCommission: Double? = null,
    @SerialName("total_reward") val totalReward: Double? = null,
    @SerialName("total_deduction") val totalDeduction: Double? = null,
    @SerialName("total_tax") val totalTax: Double? = null,
    @SerialName("total_bpjs") val totalBpjs: Double? = null,
    @SerialName("net_salary") val netSalary: Double? = null,
    val status: String? = null,
    val attendance: PayslipAttendance? = null,
    val period: PayslipPeriod? = null,
    val details: List<PayslipDetailItem>? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class PayslipPeriod(
    val id: Long,
    val name: String? = null,
    val period: String? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("payment_date") val paymentDate: String? = null
)

@Serializable
data class PayslipDetailItem(
    val id: Long? = null,
    @SerialName("payroll_run_id") val payrollRunId: Long? = null,
    @SerialName("payroll_component_id") val payrollComponentId: Long? = null,
    val label: String? = null,
    val type: String? = null,
    val amount: Double? = null
)

@Serializable
data class PayslipAttendance(
    @SerialName("total_period_days") val totalPeriodDays: Int? = null,
    @SerialName("work_days") val workDays: Int? = null,
    val hadir: Int? = null,
    val libur: Int? = null,
    val cuti: Int? = null,
    val alpha: Int? = null,
    @SerialName("rate_per_day") val ratePerDay: Double? = null,
    @SerialName("rate_per_hour") val ratePerHour: Double? = null
)
