package com.jangeby.system.security

sealed class SecurityViolation {
    data object None : SecurityViolation()

    data class DeveloperOptionsActive(
        val title: String = "Opsi Pengembang Aktif",
        val message: String = "Mode Opsi Pengembang (Developer Options) terdeteksi aktif di perangkat Anda. Demi keamanan data dan keabsahan presensi, silakan nonaktifkan fitur ini melalui Pengaturan perangkat untuk melanjutkan."
    ) : SecurityViolation()

    data class MockLocationActive(
        val title: String = "Lokasi Tiruan Terdeteksi",
        val message: String = "Sistem mendeteksi adanya aplikasi atau fitur lokasi tiruan (Fake GPS) yang aktif di perangkat Anda. Penggunaan lokasi palsu tidak diizinkan pada sistem presensi perusahaan."
    ) : SecurityViolation()
}

