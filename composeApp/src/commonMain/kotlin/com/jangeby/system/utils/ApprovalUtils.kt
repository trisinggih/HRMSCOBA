package com.jangeby.system.utils

import androidx.compose.ui.graphics.Color
import com.jangeby.system.data.remote.ApprovalItem
import com.jangeby.system.data.remote.ApprovalLog
import com.jangeby.system.data.remote.ApprovalStep

data class StepStatusInfo(
    val statusText: String,
    val statusColor: Color,
    val actorLine: String,
    val note: String?,
    val dateText: String?
)

object ApprovalUtils {

    /**
     * Mencari log keputusan approval yang cocok untuk sebuah step.
     * Menggunakan 4 kriteria pencocokan:
     * 1. ID step langsung (stepId == step.id)
     * 2. Urutan step (stepOrder == step.order)
     * 3. ID step disimpan sebagai order (stepId == step.order)
     * 4. Nama approver cocok dengan nama approver spesifik pada step
     */
    fun findLogForStep(item: ApprovalItem?, step: ApprovalStep): ApprovalLog? {
        val logs = item?.logs ?: return null
        val stepOrder = step.order
        val stepApproverName = step.approverName ?: step.expectedApproverName

        return logs.findLast { logItem ->
            val logAction = logItem.action?.lowercase()
            if (logAction in listOf("escalate", "submit")) return@findLast false

            val matchStepId = logItem.stepId != null && logItem.stepId == step.id
            val matchStepOrder = logItem.stepOrder != null && stepOrder != null && logItem.stepOrder == stepOrder
            val matchStepIdAsOrder = logItem.stepId != null && stepOrder != null && logItem.stepId == stepOrder.toLong()
            val matchApprover = !logItem.approverName.isNullOrBlank() &&
                !stepApproverName.isNullOrBlank() &&
                stepApproverName.lowercase() !in listOf("approver", "atasan langsung", "hr department", "hrd") &&
                logItem.approverName.trim().equals(stepApproverName.trim(), ignoreCase = true)

            matchStepId || matchStepOrder || matchStepIdAsOrder || matchApprover
        }
    }

    /**
     * Menyelesaikan status, warna, nama aktor, dan catatan untuk setiap step
     * secara konsisten dan akurat.
     */
    fun resolveStepStatus(item: ApprovalItem?, step: ApprovalStep, stepIndex: Int = 0): StepStatusInfo {
        val stepOrder = step.order ?: (stepIndex + 1)
        val current = item?.currentStep ?: 1
        val reqStatus = item?.status?.lowercase() ?: "pending"
        val expectedName = step.approverName ?: step.expectedApproverName ?: step.approverType ?: "Approver"

        val log = findLogForStep(item, step)
        val action = log?.action?.lowercase()

        return when {
            action in listOf("approve", "approved") -> {
                StepStatusInfo(
                    statusText = "DISETUJUI",
                    statusColor = Color(0xFF16A34A),
                    actorLine = "Disetujui oleh: ${log?.approverName ?: expectedName}",
                    note = log?.note,
                    dateText = log?.createdAt
                )
            }
            action in listOf("reject", "rejected") -> {
                StepStatusInfo(
                    statusText = "DITOLAK",
                    statusColor = Color(0xFFDC2626),
                    actorLine = "Ditolak oleh: ${log?.approverName ?: expectedName}",
                    note = log?.note,
                    dateText = log?.createdAt
                )
            }
            action in listOf("cancel", "cancelled") -> {
                StepStatusInfo(
                    statusText = "DIBATALKAN",
                    statusColor = Color(0xFF64748B),
                    actorLine = "Dibatalkan oleh: ${log?.approverName ?: "Pemohon"}",
                    note = log?.note,
                    dateText = log?.createdAt
                )
            }
            // Jika belum ada log keputusan spesifik:
            stepOrder < current -> {
                // Tahap sebelumnya sudah pasti disetujui karena workflow sudah maju ke tahap berikutnya
                StepStatusInfo(
                    statusText = "DISETUJUI",
                    statusColor = Color(0xFF16A34A),
                    actorLine = "Disetujui oleh: $expectedName",
                    note = null,
                    dateText = null
                )
            }
            stepOrder == current -> {
                when (reqStatus) {
                    "approved" -> StepStatusInfo(
                        statusText = "DISETUJUI",
                        statusColor = Color(0xFF16A34A),
                        actorLine = "Disetujui oleh: $expectedName",
                        note = null,
                        dateText = null
                    )
                    "rejected" -> StepStatusInfo(
                        statusText = "DITOLAK",
                        statusColor = Color(0xFFDC2626),
                        actorLine = "Ditolak pada tahap ini: $expectedName",
                        note = null,
                        dateText = null
                    )
                    "cancelled" -> StepStatusInfo(
                        statusText = "DIBATALKAN",
                        statusColor = Color(0xFF64748B),
                        actorLine = "Dibatalkan",
                        note = null,
                        dateText = null
                    )
                    else -> StepStatusInfo(
                        statusText = "MENUNGGU",
                        statusColor = Color(0xFFF59E0B),
                        actorLine = "Menunggu persetujuan: $expectedName",
                        note = null,
                        dateText = null
                    )
                }
            }
            else -> { // stepOrder > current
                if (reqStatus in listOf("rejected", "cancelled")) {
                    StepStatusInfo(
                        statusText = "TIDAK DIPROSES",
                        statusColor = Color(0xFF94A3B8),
                        actorLine = "Tidak diproses (pengajuan telah ditolak/dibatalkan)",
                        note = null,
                        dateText = null
                    )
                } else {
                    StepStatusInfo(
                        statusText = "MENUNGGU",
                        statusColor = Color(0xFF94A3B8),
                        actorLine = "Menunggu tahap sebelumnya selesai",
                        note = null,
                        dateText = null
                    )
                }
            }
        }
    }
}
