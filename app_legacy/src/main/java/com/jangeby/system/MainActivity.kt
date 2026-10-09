package com.jangeby.system

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.graphics.BitmapFactory
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jangeby.system.data.local.SecurePrefs
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.LoginRequest
import com.jangeby.system.ui.UiHelpers
import com.jangeby.system.ui.dashboard.DashboardActivity
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * MainActivity — layar login employee (tabel employees).
 * Terhubung ke POST /api/v1/employee-auth/login.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var tvError: TextView
    private lateinit var btnLogin: Button

    private val prefs by lazy { SecurePrefs(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi ApiClient agar interceptor punya akses penyimpanan aman.
        ApiClient.init(this)

        // Apabila sudah login, langsung masuk dashboard.
        if (prefs.isLoggedIn) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val container = findViewById<View>(R.id.loginContainer)
        container.alpha = 0f
        container.translationY = 50f
        container.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        tvError = findViewById(R.id.tvError)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener { performLogin() }

        findViewById<TextView>(R.id.tvForgot).setOnClickListener {
            Toast.makeText(this, "Hubungi admin HR untuk reset password.", Toast.LENGTH_SHORT).show()
        }

        loadCompanyLogo()
    }

    private fun performLogin() {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString().orEmpty()

        if (email.isEmpty() || password.isEmpty()) {
            showError("Email dan password wajib diisi")
            return
        }

        setLoading(true)
        showError(null)

        lifecycleScope.launch {
            try {
                // Ambil Android ID sebagai identitas unik perangkat
                val androidId = Settings.Secure.getString(
                    this@MainActivity.contentResolver,
                    Settings.Secure.ANDROID_ID
                )
                val deviceName = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL

                val response = ApiClient.getService(this@MainActivity)
                    .login(LoginRequest(
                        email = email,
                        password = password,
                        deviceName = deviceName,
                        deviceId = androidId
                    ))

                if (response.success && response.data != null) {
                    val data = response.data
                    val emp = data.employee
                    prefs.saveSession(
                        token = data.token,
                        name = emp?.name,
                        email = emp?.email,
                        position = emp?.position,
                        branch = emp?.branch,
                        branchId = emp?.branchId ?: -1L,
                        employeeId = emp?.employeeId,
                        dbId = emp?.id ?: -1L,
                        photoUrl = emp?.photoUrl,
                        canClockInAnywhere = emp?.canClockInAnywhere ?: false
                    )
                    Toast.makeText(this@MainActivity, "Login berhasil", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                    finish()
                } else {
                    showError(response.message ?: "Login gagal")
                }
            } catch (t: Throwable) {
                showError(UiHelpers.errorMessageFrom(this@MainActivity, t))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        btnLogin.isEnabled = !loading
        btnLogin.text = if (loading) "Memproses..." else "Masuk"
    }

    private fun showError(message: String?) {
        tvError.text = message
        tvError.visibility = if (message.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    private fun loadCompanyLogo() {
        val ivLogo = findViewById<ImageView?>(R.id.ivLogo)
        val tvAppName = findViewById<TextView?>(R.id.tvAppName)
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.getService(this@MainActivity).getCompanyLogo()
                }
                if (response.success && response.data != null) {
                    val companyData = response.data
                    if (!companyData.name.isNullOrBlank()) {
                        tvAppName?.text = companyData.name
                    }
                    if (!companyData.logo.isNullOrEmpty() && ivLogo != null && !isFinishing && !isDestroyed) {
                        var urlStr = companyData.logo!!
                        if (urlStr.startsWith("http://")) {
                            urlStr = urlStr.replaceFirst("http://", "https://")
                        }
                        com.bumptech.glide.Glide.with(this@MainActivity)
                            .load(urlStr)
                            .into(ivLogo)
                        ivLogo.imageTintList = null
                    }
                }
            } catch (e: Exception) {
                // Ignore, fallback to default logo
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Security check dikelola oleh HRMSApplication secara global
    }
}
