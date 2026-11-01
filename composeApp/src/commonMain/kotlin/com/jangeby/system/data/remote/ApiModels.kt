package com.jangeby.system.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
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
    val data: T? = null,
    @SerialName("has_subordinates") val hasSubordinates: Boolean? = null,
    @SerialName("can_request_assets") val canRequestAssets: Boolean? = null,
    @SerialName("subordinates_count") val subordinatesCount: Int? = null
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
data class RegisterRequest(
    val name: String,
    val email: String,
    val phone: String? = null,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String
)

@Serializable
data class RegisterResponseData(
    val email: String? = null,
    val name: String? = null
)

@Serializable
data class VerifyOtpRequest(
    val email: String,
    val otp: String,
    @SerialName("device_name") val deviceName: String = "Mobile",
    @SerialName("device_id") val deviceId: String? = null
)

@Serializable
data class ResendOtpRequest(
    val email: String
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
    @SerialName("has_subordinates") val hasSubordinates: Boolean = false,
    @SerialName("subordinates_count") val subordinatesCount: Int = 0,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("signature_url") val signatureUrl: String? = null,
    val documents: List<EmployeeDocument>? = null,
    val educations: List<EmployeeEducation>? = null,
    val trainings: List<EmployeeTraining>? = null
)

@Serializable
data class EmployeeSignatureResponse(
    val id: Long? = null,
    @SerialName("signature_url") val signatureUrl: String? = null,
    @SerialName("strokes_data") val strokesData: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SaveSignatureRequest(
    @SerialName("signature_base64") val signatureBase64: String,
    @SerialName("strokes_data") val strokesData: String? = null
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
    @SerialName("clock_out") val endTime: String? = null,
    @SerialName("cross_midnight") val crossMidnight: Boolean? = null
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

// ===================== MILESTONES & WISHES =====================
@Serializable
data class MilestoneResponseData(
    val month: Int? = null,
    val year: Int? = null,
    @SerialName("today_count") val todayCount: Int = 0,
    @SerialName("total_milestones") val totalMilestones: Int = 0,
    @SerialName("my_milestone_today") val myMilestoneToday: MilestoneItemData? = null,
    val milestones: List<MilestoneItemData> = emptyList()
)

@Serializable
data class MilestoneItemData(
    val id: String,
    val type: String, // "birthday" or "work_anniversary"
    @SerialName("employee_id") val employeeId: Long,
    @SerialName("employee_name") val employeeName: String,
    val nickname: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("position_name") val positionName: String? = null,
    @SerialName("department_name") val departmentName: String? = null,
    @SerialName("branch_name") val branchName: String? = null,
    @SerialName("event_date") val eventDate: String? = null,
    @SerialName("event_day") val eventDay: Int = 0,
    @SerialName("event_month") val eventMonth: Int = 0,
    @SerialName("formatted_date") val formattedDate: String? = null,
    @SerialName("is_today") val isToday: Boolean = false,
    @SerialName("days_until") val daysUntil: Int = 0,
    @SerialName("years_count") val yearsCount: Int = 0,
    @SerialName("badge_title") val badgeTitle: String? = null,
    @SerialName("milestone_year") val milestoneYear: Int = 0,
    @SerialName("wishes_count") var wishesCount: Int = 0,
    @SerialName("has_wished") var hasWished: Boolean = false,
    @SerialName("recent_wishes") val recentWishes: List<MilestoneWishData> = emptyList()
)

@Serializable
data class MilestoneWishData(
    val id: Long,
    @SerialName("sender_id") val senderId: Long,
    @SerialName("sender_name") val senderName: String,
    @SerialName("sender_nickname") val senderNickname: String? = null,
    @SerialName("sender_photo_url") val senderPhotoUrl: String? = null,
    val message: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("time_ago") val timeAgo: String? = null,
    @SerialName("is_mine") val isMine: Boolean = false
)

@Serializable
data class SendMilestoneWishRequest(
    @SerialName("celebrated_employee_id") val celebratedEmployeeId: Long,
    @SerialName("milestone_type") val milestoneType: String,
    @SerialName("milestone_year") val milestoneYear: Int,
    val message: String
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
    @SerialName("approver_id") val approverId: Long? = null,
    @SerialName("approver_name") val approverName: String? = null,
    @SerialName("expected_approver_name") val expectedApproverName: String? = null,
    @SerialName("is_optional") val isOptional: Boolean? = null
)

@Serializable
data class ApprovalLog(
    val id: Long,
    @SerialName("approval_step_id") val stepId: Long? = null,
    @SerialName("step_order") val stepOrder: Int? = null,
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
            "asset_request", "asset", "permintaan_aset" -> "Pengajuan Aset"
            "employee_mutations", "mutation", "employee_mutation", "mutasi" -> "Surat Mutasi"
            else -> module?.replaceFirstChar { it.uppercase() }?.replace("_", " ") ?: "Pengajuan"
        }

    val parsedDescription: String
        get() {
            val p = payloadObject
            val info = StringBuilder()
            val leaveType = p?.get("leave_type")?.jsonPrimitive?.contentOrNull
            val startDate = p?.get("start_date")?.jsonPrimitive?.contentOrNull
            val endDate = p?.get("end_date")?.jsonPrimitive?.contentOrNull
            val days = p?.get("total_days")?.jsonPrimitive?.contentOrNull

            if (!leaveType.isNullOrEmpty()) {
                info.append("Cuti: $leaveType\n")
            }

            val permType = p?.get("type")?.jsonPrimitive?.contentOrNull
            val isExitPermit = permType in listOf("out_of_office", "izin_keluar")
            val startTime = p?.get("start_time")?.jsonPrimitive?.contentOrNull
                ?: p?.get("requested_start_time")?.jsonPrimitive?.contentOrNull
            val endTime = p?.get("end_time")?.jsonPrimitive?.contentOrNull
                ?: p?.get("requested_end_time")?.jsonPrimitive?.contentOrNull

            val effectiveStart = startDate ?: p?.get("date")?.jsonPrimitive?.contentOrNull
            val effectiveEnd = endDate ?: effectiveStart
            val isMultiDay = !effectiveEnd.isNullOrBlank() && !effectiveStart.isNullOrBlank() && effectiveEnd.take(10) != effectiveStart.take(10)

            if (!effectiveStart.isNullOrEmpty()) {
                val sdFmt = com.jangeby.system.utils.DateTimeUtils.formatDateOnlyDMY(effectiveStart)
                val edFmt = if (!effectiveEnd.isNullOrEmpty()) com.jangeby.system.utils.DateTimeUtils.formatDateOnlyDMY(effectiveEnd) else null
                val dayText = if (!days.isNullOrEmpty()) " ($days Hari)" else ""
                val sTime = startTime?.take(5)
                val eTime = endTime?.take(5)
                val timeStr = if (!isMultiDay && isExitPermit && !sTime.isNullOrEmpty() && !eTime.isNullOrEmpty()) {
                    " ($sTime - $eTime)"
                } else ""
                val dateText = if (isMultiDay && edFmt != null) {
                    "$sdFmt s/d $edFmt"
                } else {
                    sdFmt
                }
                info.append("Tanggal: $dateText$dayText$timeStr\n")
            }

            val isAsset = module?.lowercase()?.trim() in listOf("asset_request", "asset", "permintaan_aset")
            if (isAsset) {
                val itemName = p?.get("item_name")?.jsonPrimitive?.contentOrNull
                val qty = p?.get("quantity")?.jsonPrimitive?.contentOrNull
                val remark = (p?.get("notes")?.jsonPrimitive?.contentOrNull
                    ?: p?.get("note")?.jsonPrimitive?.contentOrNull
                    ?: p?.get("remark")?.jsonPrimitive?.contentOrNull
                    ?: p?.get("specifications")?.jsonPrimitive?.contentOrNull)
                val purpose = p?.get("purpose")?.jsonPrimitive?.contentOrNull

                if (!itemName.isNullOrBlank()) {
                    val qtyStr = if (!qty.isNullOrBlank()) " ($qty unit)" else ""
                    info.append("Barang: $itemName$qtyStr\n")
                }
                if (!purpose.isNullOrBlank()) {
                    info.append("Tujuan: $purpose\n")
                }
                if (!remark.isNullOrBlank()) {
                    info.append("Keterangan: $remark")
                } else if (purpose.isNullOrBlank() && itemName.isNullOrBlank()) {
                    info.append("Pengajuan pengadaan / perbaikan aset")
                }
                return info.toString().trim()
            }

            val isMutation = module?.lowercase()?.trim() in listOf("employee_mutations", "mutation", "employee_mutation", "mutasi")
            if (isMutation) {
                val memoNumber = p?.get("memo_number")?.jsonPrimitive?.contentOrNull
                val mutType = p?.get("mutation_type")?.jsonPrimitive?.contentOrNull ?: p?.get("type")?.jsonPrimitive?.contentOrNull
                val effDate = p?.get("effective_date")?.jsonPrimitive?.contentOrNull
                val oldPos = p?.get("old_position_name")?.jsonPrimitive?.contentOrNull
                val newPos = p?.get("new_position_name")?.jsonPrimitive?.contentOrNull
                val oldBranch = p?.get("old_branch_name")?.jsonPrimitive?.contentOrNull ?: p?.get("old_company_name")?.jsonPrimitive?.contentOrNull
                val newBranch = p?.get("new_branch_name")?.jsonPrimitive?.contentOrNull ?: p?.get("new_company_name")?.jsonPrimitive?.contentOrNull
                val reason = p?.get("reason")?.jsonPrimitive?.contentOrNull

                val typeLabel = if (mutType?.equals("promosi", ignoreCase = true) == true) "Promosi (Kenaikan Jabatan)" else "Mutasi (Rotasi / Transfer)"
                info.append("Tipe: $typeLabel\n")
                if (!memoNumber.isNullOrEmpty()) {
                    info.append("No: $memoNumber\n")
                }
                if (!oldPos.isNullOrEmpty() || !newPos.isNullOrEmpty()) {
                    info.append("Jabatan: ${oldPos ?: "-"} ➔ ${newPos ?: "-"}\n")
                }
                if (!oldBranch.isNullOrEmpty() || !newBranch.isNullOrEmpty()) {
                    info.append("Penempatan: ${oldBranch ?: "-"} ➔ ${newBranch ?: "-"}\n")
                }
                if (!effDate.isNullOrEmpty()) {
                    val effFmt = com.jangeby.system.utils.DateTimeUtils.formatDateOnlyDMY(effDate)
                    info.append("Tgl Efektif: $effFmt\n")
                }
                if (!reason.isNullOrEmpty()) {
                    info.append("Alasan: $reason")
                }
                return info.toString().trim()
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
                val wFmt = com.jangeby.system.utils.DateTimeUtils.formatDateOnlyDMY(workDate)
                val sFmt = com.jangeby.system.utils.DateTimeUtils.formatDateOnlyDMY(swapToDate)
                info.append("Tukar: $wFmt -> $sFmt\n")
            }

            val swapDate = p?.get("swap_date")?.jsonPrimitive?.contentOrNull
            if (!swapDate.isNullOrEmpty() && effectiveStart.isNullOrEmpty()) {
                val swFmt = com.jangeby.system.utils.DateTimeUtils.formatDateOnlyDMY(swapDate)
                info.append("Tanggal: $swFmt\n")
            }

            val tenor = p?.get("installment_months")?.jsonPrimitive?.contentOrNull
                ?: p?.get("tenor")?.jsonPrimitive?.contentOrNull
            if (!tenor.isNullOrEmpty() && tenor != "0") {
                info.append("Tenor: $tenor Bulan\n")
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

    val isAssetRequest: Boolean
        get() = module?.lowercase()?.trim() in listOf("asset_request", "asset", "permintaan_aset")

    val assetForWho: String?
        get() = payloadObject?.get("for_who")?.jsonPrimitive?.contentOrNull

    val assetPlacement: String?
        get() = payloadObject?.get("placement")?.jsonPrimitive?.contentOrNull

    val assetPurpose: String?
        get() = payloadObject?.get("purpose")?.jsonPrimitive?.contentOrNull

    val assetRequestType: String?
        get() = payloadObject?.get("request_type")?.jsonPrimitive?.contentOrNull

    val assetTotalEstimatedPrice: Double?
        get() {
            val ep = payloadObject?.get("estimated_price")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            if (ep != null && ep > 0) return ep
            val sum = assetItems.sumOf { it.estimatedPrice ?: 0.0 }
            return if (sum > 0) sum else null
        }

    val assetNotes: String?
        get() = payloadObject?.get("notes")?.jsonPrimitive?.contentOrNull
            ?: payloadObject?.get("specifications")?.jsonPrimitive?.contentOrNull

    val assetItems: List<AssetApprovalDetailItem>
        get() {
            val p = payloadObject ?: return emptyList()
            val itemsJson = p["items"] as? JsonArray
            if (itemsJson != null && itemsJson.isNotEmpty()) {
                return itemsJson.mapNotNull { element ->
                    val obj = element as? JsonObject ?: return@mapNotNull null
                    AssetApprovalDetailItem(
                        id = obj["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull(),
                        itemName = obj["item_name"]?.jsonPrimitive?.contentOrNull,
                        itemSize = obj["item_size"]?.jsonPrimitive?.contentOrNull,
                        quantity = obj["quantity"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1,
                        placement = obj["placement"]?.jsonPrimitive?.contentOrNull,
                        unitPrice = obj["unit_price"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull(),
                        estimatedPrice = obj["estimated_price"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull(),
                        notes = obj["notes"]?.jsonPrimitive?.contentOrNull
                    )
                }
            }
            val singleName = p["item_name"]?.jsonPrimitive?.contentOrNull
            if (!singleName.isNullOrBlank()) {
                return listOf(
                    AssetApprovalDetailItem(
                        itemName = singleName,
                        itemSize = p["item_size"]?.jsonPrimitive?.contentOrNull,
                        quantity = p["quantity"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1,
                        placement = p["placement"]?.jsonPrimitive?.contentOrNull ?: p["for_who"]?.jsonPrimitive?.contentOrNull,
                        unitPrice = p["unit_price"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull(),
                        estimatedPrice = p["estimated_price"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull(),
                        notes = p["notes"]?.jsonPrimitive?.contentOrNull ?: p["specifications"]?.jsonPrimitive?.contentOrNull
                    )
                )
            }
            return emptyList()
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

    val isMutation: Boolean
        get() = module?.lowercase()?.trim() in listOf("employee_mutations", "mutation", "employee_mutation", "mutasi")

    val mutationType: String?
        get() = payloadObject?.get("mutation_type")?.jsonPrimitive?.contentOrNull
            ?: payloadObject?.get("type")?.jsonPrimitive?.contentOrNull

    val mutationMemoNumber: String?
        get() = payloadObject?.get("memo_number")?.jsonPrimitive?.contentOrNull

    val mutationOldPosition: String?
        get() = payloadObject?.get("old_position_name")?.jsonPrimitive?.contentOrNull

    val mutationNewPosition: String?
        get() = payloadObject?.get("new_position_name")?.jsonPrimitive?.contentOrNull

    val mutationOldBranch: String?
        get() = payloadObject?.get("old_branch_name")?.jsonPrimitive?.contentOrNull
            ?: payloadObject?.get("old_company_name")?.jsonPrimitive?.contentOrNull

    val mutationNewBranch: String?
        get() = payloadObject?.get("new_branch_name")?.jsonPrimitive?.contentOrNull
            ?: payloadObject?.get("new_company_name")?.jsonPrimitive?.contentOrNull

    val mutationEffectiveDate: String?
        get() = payloadObject?.get("effective_date")?.jsonPrimitive?.contentOrNull

    val mutationReason: String?
        get() = payloadObject?.get("reason")?.jsonPrimitive?.contentOrNull
}

@Serializable
data class ActionApprovalRequest(
    val note: String? = null,
    val tenor: String? = null,
    @SerialName("approved_item_ids") val approvedItemIds: List<Long>? = null
)

@Serializable
data class AssetApprovalDetailItem(
    val id: Long? = null,
    @SerialName("item_name") val itemName: String? = null,
    @SerialName("item_size") val itemSize: String? = null,
    val quantity: Int? = 1,
    val placement: String? = null,
    @SerialName("unit_price") val unitPrice: Double? = null,
    @SerialName("estimated_price") val estimatedPrice: Double? = null,
    val notes: String? = null
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

@Serializable
data class AssetApprovalStageApprover(
    val id: Long? = null,
    val name: String? = null
)

@Serializable
data class AssetRequestApprovalItem(
    val id: Long? = null,
    val level: Int? = null,
    @SerialName("role_name") val roleName: String? = null,
    @SerialName("approver_id") val approverId: Long? = null,
    val status: String? = null,
    val notes: String? = null,
    @SerialName("action_date") val actionDate: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val approver: AssetApprovalStageApprover? = null
)

@Serializable
data class AssetRequestDetailItem(
    val id: Long? = null,
    @SerialName("item_name") val itemName: String? = null,
    @SerialName("item_size") val itemSize: String? = null,
    val quantity: Int? = 1,
    val placement: String? = null,
    @SerialName("unit_price") val rawUnitPrice: JsonElement? = null,
    @SerialName("estimated_price") val rawEstimatedPrice: JsonElement? = null,
    val notes: String? = null
) {
    val unitPrice: Double?
        get() = try {
            rawUnitPrice?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
        } catch (_: Exception) {
            null
        }

    val estimatedPrice: Double?
        get() = try {
            rawEstimatedPrice?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
        } catch (_: Exception) {
            null
        }
}

@Serializable
data class AssetRequestItem(
    val id: Long,
    @SerialName("request_type") val requestType: String? = "kebutuhan_rutin",
    @SerialName("item_name") val itemName: String,
    @SerialName("item_size") val itemSize: String? = null,
    @SerialName("for_who") val forWho: String? = null,
    val placement: String? = null,
    val purpose: String,
    val quantity: Int? = 1,
    @SerialName("estimated_price") val rawEstimatedPrice: JsonElement? = null,
    val specifications: String? = null,
    val notes: String? = null,
    @SerialName("admin_notes") val adminNotes: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    val status: String,
    @SerialName("purchased_at") val purchasedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val asset: AssetItem? = null,
    val items: List<AssetRequestDetailItem>? = null,
    val approvals: List<AssetRequestApprovalItem>? = null
) {
    val estimatedPrice: Double?
        get() = try {
            rawEstimatedPrice?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
        } catch (_: Exception) {
            null
        }

    val allItems: List<AssetRequestDetailItem>
        get() {
            if (!items.isNullOrEmpty()) return items
            return listOf(
                AssetRequestDetailItem(
                    itemName = itemName,
                    itemSize = itemSize,
                    quantity = quantity ?: 1,
                    placement = forWho,
                    rawEstimatedPrice = rawEstimatedPrice,
                    notes = notes ?: specifications
                )
            )
        }

    val totalEstimatedPrice: Double?
        get() {
            val ep = estimatedPrice
            if (ep != null && ep > 0) return ep
            val sum = allItems.sumOf { it.estimatedPrice ?: 0.0 }
            return if (sum > 0) sum else null
        }

    val currentApprovalProgressSummary: String
        get() {
            val apps = approvals
            if (apps.isNullOrEmpty()) {
                return when (status.lowercase()) {
                    "approved" -> "Persetujuan Selesai"
                    "purchased" -> "Barang Telah Dibeli"
                    "rejected" -> "Pengajuan Ditolak"
                    else -> "Menunggu Persetujuan"
                }
            }
            if (status.lowercase() == "approved" || status.lowercase() == "purchased") {
                return "Persetujuan Selesai (${apps.size}/${apps.size} Tahap)"
            }
            if (status.lowercase() == "rejected") {
                val rejectedStage = apps.find { it.status.equals("rejected", ignoreCase = true) }
                return if (rejectedStage != null) "Ditolak pada ${rejectedStage.roleName ?: ("Tahap " + (rejectedStage.level ?: ""))}" else "Pengajuan Ditolak"
            }
            val pendingStage = apps.find { it.status.equals("pending", ignoreCase = true) }
            val approvedCount = apps.count { it.status.equals("approved", ignoreCase = true) }
            if (pendingStage != null) {
                val role = pendingStage.roleName ?: ("Tahap " + (pendingStage.level ?: ""))
                val approverName = pendingStage.approver?.name?.let { " ($it)" } ?: ""
                return "Menunggu: $role$approverName"
            }
            return "$approvedCount/${apps.size} Tahap Selesai"
        }
}

@Serializable
data class AssetRequestItemCreate(
    @SerialName("item_name") val itemName: String,
    @SerialName("item_size") val itemSize: String? = null,
    val quantity: Int = 1,
    val placement: String? = null,
    @SerialName("unit_price") val unitPrice: Double? = null,
    @SerialName("estimated_price") val estimatedPrice: Double? = null,
    val notes: String? = null
)

@Serializable
data class AssetRequestCreate(
    @SerialName("request_type") val requestType: String = "kebutuhan_rutin",
    @SerialName("item_name") val itemName: String? = null,
    @SerialName("item_size") val itemSize: String? = null,
    @SerialName("for_who") val forWho: String? = null,
    val purpose: String,
    val quantity: Int = 1,
    @SerialName("estimated_price") val estimatedPrice: Double? = null,
    val specifications: String? = null,
    val items: List<AssetRequestItemCreate>? = null
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
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
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
    val prorate: PayslipProrate? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class PayslipProrate(
    @SerialName("is_prorated") val isProrated: Boolean? = false,
    @SerialName("join_date") val joinDate: String? = null,
    @SerialName("working_days") val workingDays: Int? = null,
    @SerialName("month_working_days") val monthWorkingDays: Int? = null,
    val ratio: Double? = null,
    val formula: String? = null,
    val note: String? = null
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
