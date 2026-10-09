package com.jangeby.system.ui.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.jangeby.system.MainActivity
import com.jangeby.system.R
import com.jangeby.system.data.local.SecurePrefs
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.ui.UiHelpers
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ProfileActivity : AppCompatActivity() {

    private lateinit var prefs: SecurePrefs
    
    private lateinit var ivProfile: ImageView
    private lateinit var fabEditPhoto: FloatingActionButton
    private lateinit var tvNameHeader: TextView
    private lateinit var tvPositionHeader: TextView
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etBirthDate: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var layoutLoading: FrameLayout

    private var photoFile: File? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                photoFile = File(uri.path ?: "")
                ivProfile.setImageURI(uri)
            }
        } else if (result.resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(result.data), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        
        prefs = SecurePrefs(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        ivProfile = findViewById(R.id.ivProfile)
        fabEditPhoto = findViewById(R.id.fabEditPhoto)
        tvNameHeader = findViewById(R.id.tvNameHeader)
        tvPositionHeader = findViewById(R.id.tvPositionHeader)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etBirthDate = findViewById(R.id.etBirthDate)
        etAddress = findViewById(R.id.etAddress)
        btnSave = findViewById(R.id.btnSave)
        btnLogout = findViewById(R.id.btnLogout)
        layoutLoading = findViewById(R.id.layoutLoading)

        setupData()
        
        fabEditPhoto.setOnClickListener {
            ImagePicker.with(this)
                .cropSquare()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .createIntent { intent ->
                    imagePickerLauncher.launch(intent)
                }
        }

        val btnChangePassword = findViewById<MaterialButton>(R.id.btnChangePassword)

        btnSave.setOnClickListener {
            updateProfile()
        }

        btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        btnLogout.setOnClickListener {
            confirmLogout()
        }
        
        etBirthDate.isFocusable = false
        etBirthDate.isClickable = true
        etBirthDate.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val date = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    etBirthDate.setText(date)
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupData() {
        tvNameHeader.text = prefs.employeeName ?: "Unknown User"
        tvPositionHeader.text = prefs.employeePosition ?: "No Position"
        etEmail.setText(prefs.employeeEmail ?: "")

        val allCapsFilter = android.text.InputFilter.AllCaps()
        etName.filters = arrayOf(allCapsFilter)
        etPhone.filters = arrayOf(allCapsFilter)
        etAddress.filters = arrayOf(allCapsFilter)
        etEmail.filters = arrayOf(allCapsFilter)

        etName.setText(prefs.employeeName ?: "")

        
        // In a real app we might fetch the latest profile from API to fill phone and address
        // For simplicity, if we don't have them in prefs, we fetch them now
        fetchProfile()
    }

    private fun fetchProfile() {
        layoutLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(this@ProfileActivity).getProfile()
                if (response.success && response.data != null) {
                    val emp = response.data
                    etName.setText(emp.name ?: "")
                    etPhone.setText(emp.phone ?: "")
                    etAddress.setText(emp.address ?: "")
                    etBirthDate.setText(emp.birthDate ?: "")
                    
                    if (!emp.photoUrl.isNullOrEmpty()) {
                        com.bumptech.glide.Glide.with(this@ProfileActivity)
                            .load(emp.photoUrl)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(ivProfile)
                    }
                }
            } catch (e: Exception) {
                // Ignore error silently
            } finally {
                layoutLoading.visibility = View.GONE
            }
        }
    }

    private fun updateProfile() {
        val name = etName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val birthDate = etBirthDate.text.toString().trim()

        layoutLoading.visibility = View.VISIBLE
        btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                val reqName = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqPhone = phone.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqAddress = address.toRequestBody("text/plain".toMediaTypeOrNull())
                val reqBirthDate = birthDate.toRequestBody("text/plain".toMediaTypeOrNull())
                
                var partPhoto: MultipartBody.Part? = null
                if (photoFile != null) {
                    val reqFile = photoFile!!.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    partPhoto = MultipartBody.Part.createFormData("photo", photoFile!!.name, reqFile)
                }

                val response = ApiClient.getService(this@ProfileActivity).updateProfile(
                    name = reqName,
                    phone = reqPhone,
                    address = reqAddress,
                    birthDate = reqBirthDate,
                    photo = partPhoto
                )

                if (response.success) {
                    prefs.employeeName = name
                    tvNameHeader.text = name
                    Toast.makeText(this@ProfileActivity, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ProfileActivity, response.message ?: "Gagal memperbarui profil", Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                Toast.makeText(this@ProfileActivity, UiHelpers.errorMessageFrom(this@ProfileActivity, t), Toast.LENGTH_SHORT).show()
            } finally {
                layoutLoading.visibility = View.GONE
                btnSave.isEnabled = true
            }
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etCurrentPassword = dialogView.findViewById<EditText>(R.id.etCurrentPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSubmit = dialogView.findViewById<MaterialButton>(R.id.btnSubmit)

        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        dialog.setContentView(dialogView)
        dialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSubmit.setOnClickListener {
            val current = etCurrentPassword.text.toString()
            val newPass = etNewPassword.text.toString()
            val confirm = etConfirmPassword.text.toString()

            if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Semua kolom password harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPass != confirm) {
                Toast.makeText(this, "Konfirmasi password tidak cocok", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            layoutLoading.visibility = View.VISIBLE
            btnSubmit.isEnabled = false
            
            lifecycleScope.launch {
                try {
                    val req = com.jangeby.system.data.remote.ChangePasswordRequest(
                        currentPassword = current,
                        newPassword = newPass,
                        newPasswordConfirmation = confirm
                    )
                    val response = ApiClient.getService(this@ProfileActivity).changePassword(req)
                    if (response.success) {
                        Toast.makeText(this@ProfileActivity, "Password berhasil diubah", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this@ProfileActivity, response.message ?: "Gagal mengubah password", Toast.LENGTH_SHORT).show()
                    }
                } catch (t: Throwable) {
                    Toast.makeText(this@ProfileActivity, UiHelpers.errorMessageFrom(this@ProfileActivity, t), Toast.LENGTH_SHORT).show()
                } finally {
                    layoutLoading.visibility = View.GONE
                    btnSubmit.isEnabled = true
                }
            }
        }
        
        dialog.show()
    }

    private fun confirmLogout() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(com.jangeby.system.R.layout.layout_bottom_sheet_confirmation, null)
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundResource(android.R.color.transparent)
        
        val tvTitle = view.findViewById<android.widget.TextView>(com.jangeby.system.R.id.tvBsTitle)
        val tvMessage = view.findViewById<android.widget.TextView>(com.jangeby.system.R.id.tvBsMessage)
        val btnPositive = view.findViewById<com.google.android.material.button.MaterialButton>(com.jangeby.system.R.id.btnBsPositive)
        val btnNegative = view.findViewById<com.google.android.material.button.MaterialButton>(com.jangeby.system.R.id.btnBsNegative)
        
        tvTitle.text = "Keluar"
        tvMessage.text = "Apakah Anda yakin ingin keluar dari aplikasi?"
        btnPositive.text = "Ya"
        btnNegative.text = "Batal"
        
        btnPositive.setOnClickListener {
            doLogout()
            bottomSheetDialog.dismiss()
        }
        
        btnNegative.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.show()
    }

    private fun doLogout() {
        layoutLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                ApiClient.getService(this@ProfileActivity).logout()
            } catch (e: Exception) {
                // Abaikan error jaringan saat logout
            } finally {
                prefs.clearSession()
                layoutLoading.visibility = View.GONE
                val intent = Intent(this@ProfileActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
}
