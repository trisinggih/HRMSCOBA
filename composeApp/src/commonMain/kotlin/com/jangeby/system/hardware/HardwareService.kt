package com.jangeby.system.hardware

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

data class CameraResult(
    val base64Image: String?,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)
