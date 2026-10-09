package com.jangeby.system.data.model

data class PermitRequest(
    val id: Int,
    val employee_id: Int,
    val type: String,
    val date: String,
    val start_time: String,
    val end_time: String,
    val reason: String,
    val status: String,
    val created_at: String? = null
)

data class CreatePermitRequest(
    val type: String,
    val date: String,
    val start_time: String,
    val end_time: String,
    val reason: String
)
