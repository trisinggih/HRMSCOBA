package com.jangeby.system.ui.payslip

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.jangeby.system.R
import com.jangeby.system.data.model.PayrollRun
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.ui.UiHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PayslipActivity : AppCompatActivity() {

    private lateinit var rvPayslips: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: PayslipAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payslip)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        rvPayslips = findViewById(R.id.rvPayslips)
        tvEmpty = findViewById(R.id.tvEmpty)
        progressBar = findViewById(R.id.progressBar)

        adapter = PayslipAdapter(emptyList()) { run ->
            downloadAndOpenPdf(run)
        }
        rvPayslips.adapter = adapter

        loadPayslips()
    }

    private fun loadPayslips() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val res = ApiClient.getService(this@PayslipActivity).myPayslips(perPage = 50)
                val items = res.data?.items ?: emptyList()
                adapter.updateData(items)
                
                if (items.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                }
            } catch (t: Throwable) {
                tvEmpty.text = "Gagal memuat slip gaji.\n${UiHelpers.errorMessageFrom(this@PayslipActivity, t)}"
                tvEmpty.visibility = View.VISIBLE
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun downloadAndOpenPdf(run: PayrollRun) {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.getService(this@PayslipActivity).downloadPayslipPdf(run.id)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Simpan ke cache
                        val fileName = "Slip_Gaji_${run.period?.name?.replace(" ", "_") ?: run.id}.pdf"
                        val file = File(cacheDir, fileName)
                        
                        var fos: FileOutputStream? = null
                        try {
                            fos = FileOutputStream(file)
                            fos.write(body.bytes())
                            fos.flush()
                        } finally {
                            fos?.close()
                        }
                        
                        // Buka Intent di Main Thread
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            openPdfIntent(file)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this@PayslipActivity, "PDF kosong.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@PayslipActivity, "Gagal mengunduh: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@PayslipActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openPdfIntent(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak ada aplikasi untuk membuka PDF", Toast.LENGTH_LONG).show()
        }
    }
}
