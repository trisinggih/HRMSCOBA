package com.jangeby.system.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.jangeby.system.R
import com.jangeby.system.data.local.SecurePrefs
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.ui.attendance.AttendanceActivity
import com.jangeby.system.ui.leave.LeaveActivity
import com.jangeby.system.ui.notification.NotificationActivity
import com.jangeby.system.ui.task.TaskActivity
import com.jangeby.system.ui.asset.AssetActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.HttpURLConnection
import android.graphics.BitmapFactory
import android.widget.ImageView
import android.widget.Toast

/**
 * Dashboard/beranda employee setelah login.
 * Menampilkan sambutan + menu: Absensi, Cuti, Notifikasi, Tugas.
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var prefs: SecurePrefs
    private lateinit var tvWelcome: TextView
    private lateinit var tvSubwelcome: TextView
    private lateinit var tvUnread: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        prefs = SecurePrefs(this)

        if (!prefs.isLoggedIn) {
            goToLogin()
            return
        }

        tvWelcome = findViewById(R.id.tvWelcome)
        //tvUnread = findViewById(R.id.tvSubwelcome)
        tvSubwelcome = findViewById(R.id.tvSubwelcome)

        val name = prefs.employeeName ?: "Karyawan"
        val position = listOf(prefs.employeeCompany, prefs.employeeDivision, prefs.employeePosition)
            .filter { !it.isNullOrBlank() }
            .joinToString(" · ")
        tvWelcome.text = if (position.isBlank()) "Hay, $name!" else "Hay, $name"
        tvSubwelcome.text =  if (position.isBlank()) " " else "$position"
        setupMenu()
        //loadUnreadCount()
        findViewById<android.view.View>(R.id.btnLogout).setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Konfirmasi Logout")
                .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
                .setPositiveButton("Ya, Keluar") { _, _ -> logout() }
                .setNegativeButton("Batal", null)
                .show()
        }

        val swipeRefreshLayout = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                val j1 = loadBadges()
                val j2 = fetchProfileInfo()
                setupRecentActivity()
                j1.join()
                j2.join()
                swipeRefreshLayout.isRefreshing = false
            }
        }

        loadProfilePhoto()

        setupBottomNavigation()

        checkPermissionsAndStartService()
        checkFaceStatus()

        // Fetch and register FCM Token
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener(com.google.android.gms.tasks.OnCompleteListener { task ->
            if (!task.isSuccessful) {
                android.util.Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            android.util.Log.d("FCM", "Token: $token")
            
            // Send to backend
            lifecycleScope.launch {
                try {
                    ApiClient.getService(this@DashboardActivity).updateFcmToken(com.jangeby.system.data.remote.FcmTokenRequest(fcmToken = token))
                } catch (e: Exception) {
                    android.util.Log.e("FCM", "Failed to update FCM token", e)
                }
            }
        })

    }

    private fun loadBadges(): kotlinx.coroutines.Job {
        return lifecycleScope.launch {
            try {
                val res = ApiClient.getService(this@DashboardActivity).getBadges()
                val data = res.data
                if (data != null) {
                    updateBadge(findViewById(R.id.badgePengajuan), data.pengajuanCount)
                    updateBadge(findViewById(R.id.badgeApproval), data.approvalCount)
                    updateBadge(findViewById(R.id.badgeTask), data.taskCount)
                    updateBadge(findViewById(R.id.badgeInfo), data.infoCount)
                    updateBadge(findViewById(R.id.badgeOvertime), data.overtimeCount)
                    updateBadge(findViewById(R.id.badgeKasbon), data.kasbonCount)
                    updateBadge(findViewById(R.id.badgeReimburse), data.reimburseCount)
                    updateBadge(findViewById(R.id.badgePermit), data.permitCount)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateBadge(tvBadge: TextView?, count: Int) {
        if (tvBadge == null) return
        if (count > 0) {
            tvBadge.visibility = View.VISIBLE
            tvBadge.text = if (count > 99) "99+" else count.toString()
        } else {
            tvBadge.visibility = View.GONE
        }
    }

    private fun refreshData() {
        setupRecentActivity()
        loadBadges()
        fetchProfileInfo()
    }

    private fun setupRecentActivity() {
        val btnCheckOut = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCheckOut)
        val btnLeaveRequest = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLeaveRequest)
        val btnOvertimeIn = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnOvertimeIn)
        val btnOvertimeOut = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnOvertimeOut)
        val llNotifications = findViewById<android.widget.LinearLayout>(R.id.llRecentNotifications)

        btnCheckOut.setOnClickListener {
            startActivity(Intent(this, AttendanceActivity::class.java))
        }

        btnLeaveRequest.setOnClickListener {
            startActivity(Intent(this, com.jangeby.system.ui.pengajuan.PengajuanActivity::class.java))
        }
        
        btnOvertimeIn.setOnClickListener {
            val intent = Intent(this, com.jangeby.system.ui.attendance.MapAttendanceActivity::class.java)
            intent.putExtra("action", "overtime_clock_in")
            startActivity(intent)
        }
        
        btnOvertimeOut.setOnClickListener {
            val intent = Intent(this, com.jangeby.system.ui.attendance.MapAttendanceActivity::class.java)
            intent.putExtra("action", "overtime_clock_out")
            startActivity(intent)
        }

        lifecycleScope.launch {
            try {
                // Determine Check In / Check Out state based on today's attendance
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val resAtt = ApiClient.getService(this@DashboardActivity).attendanceHistory() // using default pagination
                val attItem = resAtt.data?.data?.firstOrNull()
                
                var hasClockedOut = false
                if (attItem != null && attItem.date == today) {
                    if (attItem.clock_out_at == null) {
                        btnCheckOut.text = "Check Out"
                        // User is currently working (checked in but not checked out)
                        com.jangeby.system.service.LocationService.startService(this@DashboardActivity)
                    } else {
                        btnCheckOut.text = "Check In"
                        hasClockedOut = true
                        // User has finished shift
                        com.jangeby.system.service.LocationService.stopService(this@DashboardActivity)
                    }
                } else {
                    btnCheckOut.text = "Check In"
                    // User hasn't clocked in today
                    com.jangeby.system.service.LocationService.stopService(this@DashboardActivity)
                }

                // Cek lembur hari ini
                val llRegularButtons = findViewById<android.widget.LinearLayout>(R.id.llRegularButtons)
                val llOvertimeButtons = findViewById<android.widget.LinearLayout>(R.id.llOvertimeButtons)
                val overtimeRes = ApiClient.getService(this@DashboardActivity).overtimeToday()
                val overtimeToday = overtimeRes.data
                
                if (overtimeToday != null && hasClockedOut) {
                    // Jika ada overtime yang disetujui/sedang berjalan & sudah clock out shift reguler
                    llRegularButtons.visibility = View.GONE
                    llOvertimeButtons.visibility = View.VISIBLE
                    
                    if (overtimeToday.status == "in_progress") {
                        btnOvertimeIn.isEnabled = false
                        btnOvertimeOut.isEnabled = true
                    } else {
                        btnOvertimeIn.isEnabled = true
                        btnOvertimeOut.isEnabled = false
                    }
                } else {
                    llRegularButtons.visibility = View.VISIBLE
                    llOvertimeButtons.visibility = View.GONE
                }

                // Recent notifications
                val resNotif = ApiClient.getService(this@DashboardActivity).myNotifications(limit = 3)
                val notifs = resNotif.data?.data ?: emptyList()
                llNotifications.removeAllViews()

                if (notifs.isEmpty()) {
                    val tv = TextView(this@DashboardActivity)
                    tv.text = "Belum ada aktivitas"
                    tv.setTextColor(android.graphics.Color.GRAY)
                    llNotifications.addView(tv)
                } else {
                    for (n in notifs) {
                        val view = layoutInflater.inflate(R.layout.item_recent_notification, llNotifications, false)
                        view.findViewById<TextView>(R.id.tvNotifTitle).text = n.title ?: "Aktivitas"
                        view.findViewById<TextView>(R.id.tvNotifDesc).text = n.body ?: ""
                        view.findViewById<TextView>(R.id.tvNotifTime).text = com.jangeby.system.ui.UiHelpers.formatDateTime(n.createdAt ?: "")
                        llNotifications.addView(view)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val registerFaceLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            checkFaceStatus()
        }
    }

    private fun checkFaceStatus() {
        lifecycleScope.launch {
            try {
                val res = ApiClient.getService(this@DashboardActivity).faceStatus()
                val hasFace = res.data?.hasFace ?: false
                val cardWarning = findViewById<View>(R.id.cardWarningFace)
                if (!hasFace) {
                    // Temporarily hide face registration warning as requested by user
                    cardWarning.visibility = View.GONE
                    cardWarning.setOnClickListener {
                        val intent = Intent(this@DashboardActivity, com.jangeby.system.ui.camera.CameraActivity::class.java)
                        intent.putExtra(com.jangeby.system.ui.camera.CameraActivity.EXTRA_MODE, com.jangeby.system.ui.camera.CameraActivity.MODE_REGISTER)
                        registerFaceLauncher.launch(intent)
                    }
                } else {
                    cardWarning.visibility = View.GONE
                }
            } catch (e: Exception) {
                // Ignore error, we can try again next time or just keep it hidden
            }
        }
    }

    private fun checkPermissionsAndStartService() {
        val permissionsToRequest = mutableListOf<String>()

        val fineLoc = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineLoc != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val postNotif = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            if (postNotif != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                1001
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // We no longer blindly start the service here. It will be started by the attendance check if needed.
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_attendance -> {
                    startActivity(Intent(this, AttendanceActivity::class.java))
                    false
                }
                R.id.nav_tasks -> {
                    startActivity(Intent(this, TaskActivity::class.java))
                    false
                }
                R.id.nav_leave -> {
                    startActivity(Intent(this, com.jangeby.system.ui.pengajuan.PengajuanActivity::class.java))
                    false
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, com.jangeby.system.ui.profile.ProfileActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    private fun setupMenu() {
        findViewById<View>(R.id.cardAttendance).setOnClickListener {
            startActivity(Intent(this, AttendanceActivity::class.java))
        }

        findViewById<View>(R.id.cardLeave).setOnClickListener {
            startActivity(Intent(this, com.jangeby.system.ui.pengajuan.PengajuanActivity::class.java))
        }
        findViewById<View>(R.id.cardOvertime).setOnClickListener {
            startActivity(Intent(this, com.jangeby.system.ui.overtime.OvertimeActivity::class.java))
        }
        
        findViewById<View>(R.id.cardPermit).setOnClickListener {
            startActivity(Intent(this, com.jangeby.system.ui.permit.PermitActivity::class.java))
        }
        findViewById<View>(R.id.cardTask).setOnClickListener {
            startActivity(Intent(this, TaskActivity::class.java))
        }
        findViewById<View>(R.id.cardPayslip).setOnClickListener {
            startActivity(Intent(this, com.jangeby.system.ui.payslip.PayslipActivity::class.java))
        }
        findViewById<View>(R.id.cardAsset).setOnClickListener {
            startActivity(Intent(this, com.jangeby.system.ui.asset.AssetActivity::class.java))
        }
        findViewById<View>(R.id.cardReimburse)?.setOnClickListener {
            startActivity(Intent(this, com.jangeby.system.ui.reimburse.ReimburseActivity::class.java))
        }
        findViewById<View>(R.id.cardKasbon).setOnClickListener {
            startActivity(Intent(this, com.jangeby.system.ui.kasbon.KasbonActivity::class.java))
        }
        findViewById<View>(R.id.cardApproval)?.setOnClickListener {
            startActivity(Intent(this, com.jangeby.system.ui.approval.ApprovalActivity::class.java))
        }
        findViewById<View>(R.id.cardCheckPoint)?.setOnClickListener {
            startActivity(Intent(this, com.jangeby.system.ui.checkpoint.CheckPointActivity::class.java))
        }
                findViewById<View>(R.id.cardPersonal)?.setOnClickListener {
            startActivity(Intent(this, com.jangeby.system.ui.personal.PersonalActivity::class.java))
        }
        findViewById<View>(R.id.cardInfo)?.setOnClickListener {
            startActivity(Intent(this, com.jangeby.system.ui.announcement.AnnouncementActivity::class.java))
        }
    }

//    private fun loadUnreadCount() {
//        lifecycleScope.launch {
//            try {
//                val res = ApiClient.getService(this@DashboardActivity)
//                    .unreadNotificationsCount()
//                val count = res.data?.count ?: 0
//                tvUnread.text = if (count > 0) "$count belum dibaca" else "Tidak ada notifikasi baru"
//            } catch (t: Throwable) {
//                tvUnread.text = "Notifikasi: tidak tersedia"
//            }
//        }
//    }

    
    private fun fetchProfileInfo(): kotlinx.coroutines.Job {
        val apiService = com.jangeby.system.data.remote.ApiClient.getService(this)
        return lifecycleScope.launch {
            try {
                val response = apiService.getProfile()
                if (response.success && response.data != null) {
                    val profile = response.data
                    prefs.employeeName = profile.name
                    prefs.employeeCompany = profile.company
                    prefs.employeePosition = profile.position
                    prefs.employeeDivision = profile.division
                    prefs.employeeDepartment = profile.department
                    prefs.employeeBranch = profile.branch
                    prefs.employeeBranchId = profile.branchId ?: -1L
                    prefs.dbId = profile.id ?: -1L
                    prefs.employeePhotoUrl = profile.photoUrl

                    val name = prefs.employeeName ?: "Karyawan"
                    val posDetails = listOf(profile.company, profile.division, profile.position)
                        .filter { !it.isNullOrBlank() }
                        .joinToString(" · ")

                    tvWelcome.text = if (posDetails.isBlank()) "Hay, $name!" else "Hay, $name"
                    tvSubwelcome.text = if (posDetails.isBlank()) " " else posDetails

                    // Load new photo
                    loadProfilePhoto()
                }
            } catch (e: Exception) {
                // Ignore, use cached prefs
            }
        }
    }

    private fun loadProfilePhoto() {
        val photoUrl = prefs.employeePhotoUrl
        if (!photoUrl.isNullOrEmpty()) {
            val ivProfile = findViewById<ImageView>(R.id.ivProfile)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val url = URL(photoUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    val input = connection.inputStream
                    val bitmap = BitmapFactory.decodeStream(input)
                    withContext(Dispatchers.Main) {
                        ivProfile.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    // Ignore, use default icon
                }
            }
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            // Best-effort: cabut token di server; gagal pun tetap logout lokal.
            try {
                ApiClient.getService(this@DashboardActivity).logout()
            } catch (ignored: Throwable) {
            }
            prefs.clearSession()
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, com.jangeby.system.MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (com.jangeby.system.utils.SecurityHelper.isDeviceRooted(this)) {
            android.widget.Toast.makeText(this, "Aplikasi tidak dapat berjalan di perangkat yang di-root", android.widget.Toast.LENGTH_LONG).show()
            finishAffinity()
        }
        if (com.jangeby.system.utils.SecurityHelper.isMockLocationEnabled(this)) {
            android.widget.Toast.makeText(this, "Harap matikan fitur Fake GPS / Mock Location", android.widget.Toast.LENGTH_LONG).show()
            finishAffinity()
        }
        
        // Refresh data saat kembali ke halaman Home
        refreshData()
    }
}
