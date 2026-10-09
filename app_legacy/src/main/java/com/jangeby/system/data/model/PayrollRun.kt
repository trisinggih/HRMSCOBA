package com.jangeby.system.data.model

data class PayrollRunListData(
    val items: List<PayrollRun>,
    val pagination: com.jangeby.system.data.remote.PaginationMeta?
)

data class PayrollRun(
    val id: Int,
    val employee_id: Int,
    val payroll_period_id: Int,
    val gross_salary: Double,
    val net_salary: Double,
    val total_tax: Double,
    val total_bpjs: Double,
    val status: String,
    val period: PayrollPeriod?
)

data class PayrollPeriod(
    val id: Int,
    val name: String,
    val period: String,
    val start_date: String,
    val end_date: String,
    val payment_date: String?
)
