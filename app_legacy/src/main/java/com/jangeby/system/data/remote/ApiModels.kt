package com.jangeby.system.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Bungkus umum response backend: { success, message, data }.
 * `data` boleh null pada beberapa endpoint.
 */
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?
)

/** Struktur pagination bawaan Laravel (dipakai pada beberapa endpoint). */
data class PaginatedResponse<T>(
    @SerializedName("data") val data: List<T>?,
    @SerializedName("meta") val meta: PaginationMeta?
)

data class PaginationMeta(
    @SerializedName("current_page") val currentPage: Int?,
    @SerializedName("last_page") val lastPage: Int?,
    @SerializedName("per_page") val perPage: Int?,
    @SerializedName("total") val total: Int?
)

// ===================== AUTH =====================
data class CompanyLogoData(
    @SerializedName("name") val name: String?,
    @SerializedName("logo") val logo: String?,
    @SerializedName("favicon") val favicon: String?
)

data class FcmTokenRequest(
    @SerializedName("fcm_token") val fcmToken: String
)

data class BadgeResponse(
    @SerializedName("pengajuan_count") val pengajuanCount: Int,
    @SerializedName("approval_count") val approvalCount: Int,
    @SerializedName("task_count") val taskCount: Int,
    @SerializedName("notification_count") val notificationCount: Int,
    @SerializedName("info_count") val infoCount: Int,
    @SerializedName("overtime_count") val overtimeCount: Int = 0,
    @SerializedName("kasbon_count") val kasbonCount: Int = 0,
    @SerializedName("reimburse_count") val reimburseCount: Int = 0,
    @SerializedName("permit_count") val permitCount: Int = 0,
    @SerializedName("pengajuan_sakit_count") val pengajuanSakitCount: Int = 0,
    @SerializedName("pengajuan_ijin_count") val pengajuanIjinCount: Int = 0,
    @SerializedName("pengajuan_cuti_count") val pengajuanCutiCount: Int = 0,
    @SerializedName("pengajuan_tukar_hari_count") val pengajuanTukarHariCount: Int = 0
)

data class AnnouncementData(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String?,
    @SerializedName("attachment_type") val attachmentType: String?,
    @SerializedName("attachment_url") val attachmentUrl: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("is_read") var isRead: Boolean = false
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("device_name") val deviceName: String? = "Android",
    @SerializedName("device_id") val deviceId: String? = null
)

data class LoginData(
    @SerializedName("token") val token: String,
    @SerializedName("employee") val employee: EmployeeProfile?
)

data class EmployeeProfile(
    @SerializedName("id") val id: Long,
    @SerializedName("employee_id") val employeeId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("birth_date") val birthDate: String?,
    @SerializedName("company") val company: String?,
    @SerializedName("position") val position: String?,
    @SerializedName("branch_id") val branchId: Long?,
    @SerializedName("branch") val branch: String?,
    @SerializedName("division") val division: String?,
    @SerializedName("department") val department: String?,
    @SerializedName("can_clock_in_anywhere") val canClockInAnywhere: Boolean? = false,
    @SerializedName("photo_url") val photoUrl: String?,
    @SerializedName("documents") val documents: List<EmployeeDocument>?,
    @SerializedName("educations") val educations: List<EmployeeEducation>?,
    @SerializedName("trainings") val trainings: List<EmployeeTraining>?
)

data class ChangePasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String,
    @SerializedName("new_password_confirmation") val newPasswordConfirmation: String
)

data class EmployeeDocument(
    @SerializedName("id") val id: Long,
    @SerializedName("document_name") val documentName: String?,
    @SerializedName("document_type") val documentType: String?,
    @SerializedName("file_path") val filePath: String?
)

data class EmployeeEducation(
    @SerializedName("id") val id: Long,
    @SerializedName("institution_name") val institutionName: String?,
    @SerializedName("degree") val degree: String?,
    @SerializedName("major") val major: String?,
    @SerializedName("start_year") val startYear: String?,
    @SerializedName("end_year") val endYear: String?
)

data class EmployeeTraining(
    @SerializedName("id") val id: Long,
    @SerializedName("training_name") val trainingName: String?,
    @SerializedName("institution") val institution: String?,
    @SerializedName("year") val year: String?,
    @SerializedName("certificate_path") val certificatePath: String?
)

// ===================== ATTENDANCE =====================
data class ClockInRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracy") val accuracy: Double,
    @SerializedName("device_info") val deviceInfo: String? = null,
    @SerializedName("note") val note: String? = null,
    @SerializedName("face_photo") val facePhoto: String? = null // base64 face image
)

data class ClockOutRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracy") val accuracy: Double,
    @SerializedName("device_info") val deviceInfo: String? = null,
    @SerializedName("face_photo") val facePhoto: String? = null // base64 face image
)

data class RadiusPermissionData(
    @SerializedName("can_clock_in_anywhere") val canClockInAnywhere: Boolean
)

data class BranchLocationData(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("attendance_radius") val attendanceRadius: Double?
)

data class BreakRequest(
    @SerializedName("device_info") val deviceInfo: String? = null
)

data class RegisterFaceRequest(
    @SerializedName("face_photo") val facePhoto: String
)

data class FaceStatusData(
    @SerializedName("has_face") val hasFace: Boolean,
    @SerializedName("face_registered_at") val faceRegisteredAt: String?,
    @SerializedName("face_photo_url") val facePhotoUrl: String?
)

data class LateRequestData(
    @SerializedName("date") val date: String,
    @SerializedName("reason") val reason: String
)

data class CreateLoanRequest(
    @SerializedName("amount") val amount: Double,
    @SerializedName("reason") val reason: String
)

data class LoanRequestItem(
    @SerializedName("id") val id: Long,
    @SerializedName("date") val date: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("installment_months") val installmentMonths: Int?,
    @SerializedName("reason") val reason: String?,
    @SerializedName("status") val status: String,
    @SerializedName("approved_at") val approvedAt: String?,
    @SerializedName("rejected_at") val rejectedAt: String?,
    @SerializedName("attachment_url") val attachmentUrl: String?,
    @SerializedName("approval") val approval: ApprovalItem? = null
)

data class AttendanceItem(
    @SerializedName("id") val id: Long,
    @SerializedName("employee_id") val employeeId: Long?,
    @SerializedName("shift_id") val shiftId: Long?,
    @SerializedName("shift_name") val shiftName: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("clock_in_at") val clock_in_at: String?,
    @SerializedName("clock_out_at") val clock_out_at: String?,
    @SerializedName("break_start_at") val breakStartAt: String?,
    @SerializedName("break_end_at") val breakEndAt: String?,
    @SerializedName("break_minutes") val breakMinutes: Int?,
    @SerializedName("working_hours") val workingHours: Double?,
    @SerializedName("late_minutes") val lateMinutes: Int?,
    @SerializedName("overtime_minutes") val overtimeMinutes: Int?,
    @SerializedName("status") val status: String?,
    @SerializedName("is_face_verified") val isFaceVerified: Boolean?,
    @SerializedName("shift_start") val shiftStart: String?,
    @SerializedName("shift_end") val shiftEnd: String?
)

data class CalendarItem(
    @SerializedName("date") val date: String?,
    @SerializedName("day_of_week") val dayOfWeek: Int?,
    @SerializedName("is_holiday") val isHoliday: Boolean?,
    @SerializedName("has_approved_overtime") val hasApprovedOvertime: Boolean?,
    @SerializedName("shift") val shift: ShiftItem?,
    @SerializedName("attendance") val attendance: AttendanceDetail?
)

data class ShiftItem(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("clock_in") val startTime: String?,
    @SerializedName("clock_out") val endTime: String?
)

data class AttendanceDetail(
    @SerializedName("id") val id: Long,
    @SerializedName("clock_in") val clockIn: String?,
    @SerializedName("clock_out") val clockOut: String?,
    @SerializedName("overtime_in") val overtimeIn: String?,
    @SerializedName("overtime_out") val overtimeOut: String?,
    @SerializedName("late_minutes") val lateMinutes: Int?,
    @SerializedName("status") val status: String?
)

data class Branch(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("radius") val radius: Double?
)

// ===================== LEAVE =====================
data class LeaveBalanceItem(
    @SerializedName("leave_type_id") val leaveTypeId: Long,
    @SerializedName("leave_type_name") val leaveTypeName: String,
    @SerializedName("leave_type_code") val leaveTypeCode: String,
    @SerializedName("quota") val quota: Double,
    @SerializedName("used") val used: Double,
    @SerializedName("remaining") val remaining: Double
)

data class LeaveTypeItem(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("code") val code: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("default_days") val defaultDays: Int?,
    @SerializedName("is_paid") val isPaid: Boolean?
)

data class LeaveRequestItem(
    @SerializedName("id") val id: Long,
    @SerializedName("leave_type_id") val leaveTypeId: Long,
    @SerializedName("employee_id") val employeeId: Long,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("end_date") val endDate: String?,
    @SerializedName("total_days") val totalDays: Int?,
    @SerializedName("reason") val reason: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("attachment") val attachment: String?,
    @SerializedName("leave_type") val leaveType: LeaveTypeItem?,
    @SerializedName("approval") val approval: ApprovalItem? = null
)

data class LeaveRequestCreate(
    @SerializedName("leave_type_id") val leaveTypeId: Long,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("reason") val reason: String
)

// ===================== NOTIFICATION =====================
data class NotificationItem(
    @SerializedName("id") val id: Long,
    @SerializedName("type") val type: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("data") val data: String?,
    @SerializedName("is_read") val isRead: Boolean?,
    @SerializedName("read_at") val readAt: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class ApprovalWorkflow(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("steps") val steps: List<ApprovalStep>?
)

data class ApprovalStep(
    @SerializedName("id") val id: Long,
    @SerializedName("order") val order: Int?,
    @SerializedName("approver_type") val approverType: String?,
    @SerializedName("approver_name") val approverName: String?,
    @SerializedName("expected_approver_name") val expectedApproverName: String?,
    @SerializedName("is_optional") val isOptional: Boolean?
)

data class ApprovalLog(
    @SerializedName("id") val id: Long,
    @SerializedName("approval_step_id") val stepId: Long?,
    @SerializedName("action") val action: String?,
    @SerializedName("approver_name") val approverName: String?,
    @SerializedName("note") val note: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class RequesterData(
    @SerializedName("name") val name: String?,
    @SerializedName("photo") val photo: String?
)

data class ApprovalItem(
    @SerializedName("id") val id: Long,
    @SerializedName("module") val module: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("current_step") val currentStep: Int?,
    @SerializedName("requested_by") val requestedBy: Long?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("workflow") val workflow: ApprovalWorkflow?,
    @SerializedName("logs") val logs: List<ApprovalLog>?,
    @SerializedName("payload") val payload: Map<String, Any>?,
    @SerializedName("attachment_url") val attachmentUrl: String?,
    @SerializedName("requester") val requester: RequesterData?
)

data class UnreadCountData(
    @SerializedName("count") val count: Int
)

// ===================== TASKS =====================
data class TaskListData(
    @SerializedName("items") val items: List<TaskItem>?,
    @SerializedName("pagination") val pagination: TaskPagination?
)
data class TaskPagination(
    @SerializedName("total") val total: Int?,
    @SerializedName("per_page") val perPage: Int?,
    @SerializedName("current_page") val currentPage: Int?,
    @SerializedName("last_page") val lastPage: Int?
)

data class TaskItem(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("due_date") val dueDate: String?,
    @SerializedName("priority") val priority: String?,
    @SerializedName("progress") val progress: Int?,
    @SerializedName("status") val status: String?,
    @SerializedName("created_at") val createdAt: String?
)

// ===================== LOCATION SYNC =====================
data class SyncLocationRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracy") val accuracy: Double? = null
)

// ===================== ASSETS =====================
data class AssetItem(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("serial_number") val serialNumber: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("purchase_date") val purchaseDate: String?,
    @SerializedName("notes") val notes: String?
)

// ===================== CHECK POINT =====================
data class CheckPointItem(
    @SerializedName("id") val id: Long,
    @SerializedName("client_name") val clientName: String?,
    @SerializedName("purpose") val purpose: String?,
    @SerializedName("latitude") val latitude: String?,
    @SerializedName("longitude") val longitude: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("photo_path") val photoPath: String?,
    @SerializedName("photos") val photos: List<String>?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("employee") val employee: EmployeeProfile?
)

data class ReimburseItem(
    @SerializedName("id") val id: Long,
    @SerializedName("date") val date: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("amount") val amount: Double?,
    @SerializedName("description") val description: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("attachment") val attachment: String?,
    @SerializedName("approval") val approval: ApprovalItem? = null,
    @SerializedName("created_at") val createdAt: String?
)

data class FamilyModel(
    val id: Int,
    val employee_id: Int,
    val name: String,
    val relation: String,
    val phone: String?,
    val gender: String,
    val dob: String,
    val profession: String?,
    val address: String?,
    val is_emergency_contact: Int
)

data class EducationModel(
    val id: Int,
    val employee_id: Int,
    val level: String,
    val institution_name: String,
    val major: String?,
    val start_year: Int?,
    val end_year: Int?,
    val gpa: Double?
)

data class CertificationModel(
    val id: Int,
    val employee_id: Int,
    val name: String,
    val issuer: String?,
    val issue_date: String?,
    val expiry_date: String?,
    val credential_id: String?,
    val credential_url: String?,
    val description: String?
)

data class DocumentModel(
    val id: Int,
    val employee_id: Int,
    val document_type: String,
    val file_path: String,
    val file_name: String,
    val mime_type: String?,
    val file_size: Long?
)

data class FamilyResponse(val success: Boolean, val message: String?, val data: List<FamilyModel>)
data class EducationResponse(val success: Boolean, val message: String?, val data: List<EducationModel>)
data class CertificationResponse(val success: Boolean, val message: String?, val data: List<CertificationModel>)
data class DocumentResponse(val success: Boolean, val message: String?, val data: List<DocumentModel>)


// ===================== OVERTIME =====================
data class OvertimeRequestItem(
    val id: Long,
    val date: String,
    val requested_start_time: String?,
    val requested_end_time: String?,
    val reason: String?,
    val status: String,
    val supervisor_approved_at: String?,
    val hr_approved_at: String?,
    val rejection_reason: String?,
    val created_at: String?,
    val updated_at: String?,
    val approval: ApprovalItem? = null
)

data class OvertimeRequestCreate(
    val shift_id: Long,
    val date: String,
    val reason: String,
    val requested_end_time: String
)
data class UnifiedPengajuan(
    val id: Long,
    val type: String, // "Cuti", "Sakit", "Ijin", "Telat", "Tukar Shift"
    val title: String,
    val date: String,
    val reason: String,
    val status: String,
    val approval: ApprovalItem? = null,
    val attachmentUrl: String? = null
)



data class SwapRequestItem(
    val id: Long,
    val user_id: Long,
    @com.google.gson.annotations.SerializedName("work_date") val workDate: String?,
    @com.google.gson.annotations.SerializedName("swap_to_date") val swapToDate: String?,
    val reason: String?,
    val status: String?,
    val approval: ApprovalItem? = null
)

data class SickLeaveRequestItem(
    @SerializedName("id") val id: Long,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("end_date") val endDate: String?,
    @SerializedName("reason") val reason: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("approval") val approval: ApprovalItem? = null
)

data class LateRequestItem(
    @SerializedName("id") val id: Long,
    @SerializedName("date") val date: String?,
    @SerializedName("reason") val reason: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("approval") val approval: ApprovalItem? = null
)

data class WorkdaySwapRequestItem(
    @SerializedName("id") val id: Long,
    @SerializedName("work_date") val workDate: String?,
    @SerializedName("swap_to_date") val swapToDate: String?,
    @SerializedName("reason") val reason: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("approval") val approval: ApprovalItem? = null
)
data class PermitRequestItem(
    @SerializedName("id") val id: Long,
    @SerializedName("type") val type: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("start_time") val startTime: String?,
    @SerializedName("end_time") val endTime: String?,
    @SerializedName("reason") val reason: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("attachment") val attachment: String?,
    @SerializedName("approval") val approval: ApprovalItem? = null
)

data class FamilyRequest(
    @SerializedName("name") val name: String,
    @SerializedName("relation") val relation: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("gender") val gender: String = "other",
    @SerializedName("dob") val dob: String = "2000-01-01"
)

data class EducationRequest(
    @SerializedName("institution_name") val institutionName: String,
    @SerializedName("level") val level: String,
    @SerializedName("major") val major: String?,
    @SerializedName("end_year") val endYear: String?
)
