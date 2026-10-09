package com.jangeby.system.data.remote

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.MultipartBody
import okhttp3.RequestBody

/**
 * Definisi seluruh endpoint mobile /api/v1 (auth: Bearer token ditambahkan interceptor).
 */
interface ApiService {

    @GET("company/logo")
    suspend fun getCompanyLogo(): ApiResponse<CompanyLogoData>

    // ===================== AUTH =====================
    @POST("employee-auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<LoginData>

    @POST("employee-auth/logout")
    suspend fun logout(): ApiResponse<String?>

    @POST("employee-auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): ApiResponse<String?>

    @POST("employee-auth/fcm-token")
    suspend fun updateFcmToken(@Body body: FcmTokenRequest): ApiResponse<String?>

    @GET("employee/dashboard/badges")
    suspend fun getBadges(): ApiResponse<BadgeResponse>

    @GET("employee/announcements")
    suspend fun fetchAnnouncements(): ApiResponse<PaginatedResponse<AnnouncementData>>

    @POST("employee/announcements/{id}/read")
    suspend fun markAnnouncementAsRead(@Path("id") id: Long): ApiResponse<String?>

    @GET("employee/profile")
    suspend fun getProfile(): ApiResponse<EmployeeProfile>

    @Multipart
    @POST("employee/profile/update")
    suspend fun updateProfile(
        @Part("name") name: RequestBody?,
        @Part("phone") phone: RequestBody?,
        @Part("address") address: RequestBody?,
        @Part("birth_date") birthDate: RequestBody?,
        @Part photo: MultipartBody.Part?
    ): ApiResponse<EmployeeProfile>

    @POST("employee-auth/register-face")
    suspend fun registerFace(@Body body: RegisterFaceRequest): ApiResponse<FaceStatusData>

    @GET("employee-auth/face-status")
    suspend fun faceStatus(): ApiResponse<FaceStatusData>


    // ===================== ATTENDANCE =====================
    @GET("attendance/check-radius-permission")
    suspend fun checkRadiusPermission(): ApiResponse<RadiusPermissionData>

    @GET("attendance/branch-locations")
    suspend fun branchLocations(): ApiResponse<List<BranchLocationData>>

    @POST("attendance/clock-in")
    suspend fun clockIn(@Body body: ClockInRequest): ApiResponse<AttendanceItem>

    @POST("attendance/clock-out")
    suspend fun clockOut(@Body body: ClockOutRequest): ApiResponse<AttendanceItem>

    @POST("attendance/break")
    suspend fun takeBreak(@Body body: BreakRequest): ApiResponse<AttendanceItem>

    @POST("attendance/return-break")
    suspend fun returnBreak(@Body body: BreakRequest): ApiResponse<AttendanceItem>

    // ===================== LATE / LEAVE =====================
    @POST("late/request")
    suspend fun lateRequest(@Body body: LateRequestData): ApiResponse<Any>
    
    @GET("late/request")
    suspend fun myLateRequests(
        @Query("status") status: String? = null,
        @Query("per_page") perPage: Int? = null
    ): ApiResponse<PaginatedResponse<LateRequestItem>>
    
    @GET("sick-leave/request")
    suspend fun mySickLeaveRequests(
        @Query("status") status: String? = null,
        @Query("per_page") perPage: Int? = null
    ): ApiResponse<PaginatedResponse<SickLeaveRequestItem>>

    @GET("workday-swap/request")
    suspend fun myWorkdaySwapRequests(
        @Query("status") status: String? = null,
        @Query("per_page") perPage: Int? = null
    ): ApiResponse<PaginatedResponse<WorkdaySwapRequestItem>>

    @POST("workday-swap/request")
    suspend fun createWorkdaySwap(@Body body: Map<String, String>): ApiResponse<Any>

    // ===================== KASBON / LOAN =====================
    @GET("loan/request")
    suspend fun getLoanRequests(
        @Query("per_page") perPage: Int? = null,
        @Query("status") status: String? = null
    ): ApiResponse<PaginatedResponse<LoanRequestItem>>

    @Multipart
    @POST("loan/request")
    suspend fun createLoanRequest(
        @Part("amount") amount: okhttp3.RequestBody,
        @Part("reason") reason: okhttp3.RequestBody,
        @Part photo: okhttp3.MultipartBody.Part?
    ): ApiResponse<LoanRequestItem>

    @GET("attendance/history")
    suspend fun attendanceHistory(
        @Query("per_page") perPage: Int? = null,
        @Query("status") status: String? = null
    ): ApiResponse<PaginatedResponse<AttendanceItem>>

    @GET("employee-shifts/calendar")
    suspend fun getShiftCalendar(
        @Query("employee_id") employeeId: Long,
        @Query("year") year: Int,
        @Query("month") month: Int
    ): ApiResponse<List<CalendarItem>>

    @GET("branches/{id}")
    suspend fun getBranchDetail(@Path("id") id: Long): ApiResponse<Branch>

    // ===================== LEAVE =====================
    @GET("leave/balance")
    suspend fun leaveBalance(@Query("year") year: Int? = null): ApiResponse<List<LeaveBalanceItem>>

    @GET("leave/request")
    suspend fun myLeaveRequests(
        @Query("status") status: String? = null,
        @Query("per_page") perPage: Int? = null
    ): ApiResponse<PaginatedResponse<LeaveRequestItem>>

    @POST("leave/request")
    suspend fun createLeaveRequest(@Body body: LeaveRequestCreate): ApiResponse<LeaveRequestItem>

    // ===================== NOTIFICATIONS =====================
    @GET("my/notifications")
    suspend fun myNotifications(
        @Query("limit") limit: Int? = null
    ): ApiResponse<PaginatedResponse<NotificationItem>>

    @GET("my/notifications/unread-count")
    suspend fun unreadNotificationsCount(): ApiResponse<UnreadCountData>

    @PUT("my/notifications/{id}/mark-read")
    suspend fun markNotificationRead(@Path("id") id: Long): ApiResponse<NotificationItem>

    // ===================== TASKS =====================
    @GET("my/tasks")
    suspend fun myTasks(
        @Query("status") status: String? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("date") date: String? = null
    ): ApiResponse<TaskListData>

    @POST("my/tasks/batch")
    suspend fun myTasksBatchStore(
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): ApiResponse<Any>

    @POST("my/tasks/complete-all")
    suspend fun myTasksCompleteAll(
        @Body request: Map<String, String>
    ): ApiResponse<Any>

    @PUT("my/tasks/{task}/status")
    suspend fun myTasksUpdateStatus(
        @Path("task") taskId: Long,
        @Body request: Map<String, String>
    ): ApiResponse<Any>

    // ===================== PAYSLIPS =====================
    @GET("my/payslips")
    suspend fun myPayslips(
        @Query("per_page") perPage: Int? = null,
        @Query("payroll_period_id") payrollPeriodId: Int? = null
    ): ApiResponse<com.jangeby.system.data.model.PayrollRunListData>

    @GET("my/payslips/{id}/pdf")
    suspend fun downloadPayslipPdf(
        @Path("id") runId: Int
    ): retrofit2.Response<okhttp3.ResponseBody>

    // ===================== LOCATION =====================
    @POST("attendance/sync-location")
    suspend fun syncLocation(@Body body: SyncLocationRequest): ApiResponse<String?>

    // ===================== ASSETS =====================
    @GET("my-assets")
    suspend fun myAssets(): ApiResponse<List<AssetItem>>

    // ===================== APPROVALS =====================
    @GET("approval/requests")
    suspend fun getApprovals(
        @Query("status") status: String? = null
    ): ApiResponse<PaginatedResponse<ApprovalItem>>

    @GET("approval/requests/{id}")
    suspend fun getApprovalRequest(@Path("id") id: Long): ApiResponse<ApprovalItem>

    @POST("approval/requests/{id}/approve")
    suspend fun approveRequest(@Path("id") id: Long, @Body body: Map<String, String> = emptyMap()): ApiResponse<Any>

    @POST("approval/requests/{id}/reject")
    suspend fun rejectRequest(@Path("id") id: Long, @Body body: Map<String, String> = emptyMap()): ApiResponse<ApprovalItem>

    @POST("approval/requests/{id}/cancel")
    suspend fun cancelApproval(@Path("id") id: Long, @Body body: Map<String, String> = emptyMap()): ApiResponse<ApprovalItem>

    // ===================== CHECK POINT =====================
    @GET("attendance/check-points")
    suspend fun getCheckPoints(): ApiResponse<List<CheckPointItem>>

    @Multipart
    @POST("attendance/check-points")
    suspend fun submitCheckPoint(
        @Part("client_name") clientName: RequestBody,
        @Part("purpose") purpose: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("address") address: RequestBody?,
        @Part("notes") notes: RequestBody?,
        @Part photos: List<MultipartBody.Part>
    ): ApiResponse<CheckPointItem>

    // ===================== REIMBURSEMENTS =====================
    @GET("my/reimbursements")
    suspend fun myReimbursements(
        @Query("per_page") perPage: Int? = null,
        @Query("status") status: String? = null
    ): ApiResponse<PaginatedResponse<ReimburseItem>>

    @Multipart
    @POST("my/reimbursements")
    suspend fun createReimbursement(
        @Part("date") date: RequestBody,
        @Part("type") type: RequestBody,
        @Part("amount") amount: RequestBody,
        @Part("description") description: RequestBody?,
        @Part attachment: MultipartBody.Part?
    ): ApiResponse<ReimburseItem>

    


    // Personal - Family
    @GET("employee/family")
    suspend fun getFamily(): ApiResponse<List<FamilyModel>>

    @POST("employee/family")
    suspend fun storeFamily(@Body request: Map<String, @JvmSuppressWildcards Any>): ApiResponse<FamilyModel>

    @PUT("employee/family/{id}")
    suspend fun updateFamily(@Path("id") id: Int, @Body request: Map<String, @JvmSuppressWildcards Any>): ApiResponse<FamilyModel>

    @DELETE("employee/family/{id}")
    suspend fun deleteFamily(@Path("id") id: Int): ApiResponse<Any>

    // Personal - Education
    @GET("employee/education")
    suspend fun getEducation(): ApiResponse<List<EducationModel>>

    @POST("employee/education")
    suspend fun storeEducation(@Body request: Map<String, @JvmSuppressWildcards Any>): ApiResponse<EducationModel>

    @PUT("employee/education/{id}")
    suspend fun updateEducation(@Path("id") id: Int, @Body request: Map<String, @JvmSuppressWildcards Any>): ApiResponse<EducationModel>

    @DELETE("employee/education/{id}")
    suspend fun deleteEducation(@Path("id") id: Int): ApiResponse<Any>

    // Personal - Certification
    @GET("personal/certification")
    suspend fun getCertification(): ApiResponse<List<CertificationModel>>

    @POST("personal/certification")
    suspend fun storeCertification(@Body request: Map<String, @JvmSuppressWildcards Any>): ApiResponse<CertificationModel>

    @PUT("personal/certification/{id}")
    suspend fun updateCertification(@Path("id") id: Int, @Body request: Map<String, @JvmSuppressWildcards Any>): ApiResponse<CertificationModel>

    @DELETE("personal/certification/{id}")
    suspend fun deleteCertification(@Path("id") id: Int): ApiResponse<Any>

    // Personal - Document
    @GET("personal/document")
    suspend fun getDocument(): ApiResponse<List<DocumentModel>>

    @Multipart
    @POST("personal/document")
    suspend fun storeDocument(
        @Part("document_type") documentType: RequestBody,
        @Part file: MultipartBody.Part
    ): ApiResponse<DocumentModel>

    @DELETE("personal/document/{id}")
    suspend fun deleteDocument(@Path("id") id: Int): ApiResponse<Any>


    // ===================== OVERTIME =====================
    @GET("mobile/overtime/today")
    suspend fun overtimeToday(): ApiResponse<OvertimeRequestItem?>

    @GET("mobile/overtime/history")
    suspend fun getOvertimeHistory(): ApiResponse<PaginatedResponse<OvertimeRequestItem>>

    @POST("mobile/overtime/{id}/cancel")
    suspend fun cancelOvertime(
        @Path("id") id: Long,
        @Body request: Map<String, String>
    ): ApiResponse<Any>

    @Multipart
    @POST("mobile/overtime/request")
    suspend fun createOvertimeRequest(
        @Part("date") date: okhttp3.RequestBody,
        @Part("reason") reason: okhttp3.RequestBody,
        @Part("requested_start_time") requestedStartTime: okhttp3.RequestBody,
        @Part("requested_end_time") requestedEndTime: okhttp3.RequestBody,
        @Part attachment: okhttp3.MultipartBody.Part?
    ): ApiResponse<OvertimeRequestItem>

    @POST("mobile/overtime/clock-in")
    suspend fun overtimeClockIn(): ApiResponse<AttendanceItem>

    @Multipart
    @POST("mobile/overtime/clock-out")
    suspend fun overtimeClockOut(
        @Part("report_text") reportText: okhttp3.RequestBody,
        @Part("face_photo") facePhoto: okhttp3.RequestBody,
        @Part reportPhotos: List<okhttp3.MultipartBody.Part>
    ): ApiResponse<AttendanceItem>


    // Permit / Izin
    @GET("permission/request")
    suspend fun getPermitHistory(
        @Query("status") status: String? = null,
        @Query("per_page") perPage: Int? = null
    ): ApiResponse<PaginatedResponse<PermitRequestItem>>

    @Multipart
    @POST("permission/request")
    suspend fun createPermitRequest(
        @Part("type") type: okhttp3.RequestBody,
        @Part("date") date: okhttp3.RequestBody,
        @Part("start_time") startTime: okhttp3.RequestBody,
        @Part("end_time") endTime: okhttp3.RequestBody,
        @Part("reason") reason: okhttp3.RequestBody,
        @Part attachment: okhttp3.MultipartBody.Part? = null
    ): ApiResponse<PermitRequestItem>

    @Multipart
    @POST("sick-leave/request")
    suspend fun createSickLeaveRequest(
        @Part("start_date") startDate: RequestBody,
        @Part("end_date") endDate: RequestBody,
        @Part("reason") reason: RequestBody,
        @Part medicalCertificate: MultipartBody.Part? = null
    ): ApiResponse<SickLeaveRequestItem>
    
    @Multipart
    @POST("attendance/correction")
    suspend fun createAttendanceCorrection(
        @Part("type") type: RequestBody,
        @Part("date") date: RequestBody,
        @Part("time") time: RequestBody,
        @Part("reason") reason: RequestBody,
        @Part attachment: MultipartBody.Part? = null
    ): ApiResponse<Any>
    
    
    @GET("workday-swap/request")
    suspend fun getWorkdaySwapRequests(): ApiResponse<PaginatedResponse<com.jangeby.system.data.remote.SwapRequestItem>>
}

