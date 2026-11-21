# Build dan verifikasi iOS

Proyek ini memakai Compose Multiplatform, AVFoundation untuk kamera absensi, dan Firebase Messaging melalui APNs. Perubahan sumber di server atau APK Android tidak memperbarui aplikasi iPhone yang sudah terpasang: build iOS baru harus dipasang.

## Konfigurasi yang dibutuhkan

1. Buka `iosApp.xcodeproj` di Mac dengan Xcode yang mendukung Firebase Apple SDK 12.12.0 (Swift 6 / Xcode 16.2 atau lebih baru), serta toolchain Kotlin proyek ini. Resolve Swift Packages. FirebaseCore dan FirebaseMessaging sudah ditautkan ke target.
2. Pilih Apple Developer Team yang memiliki bundle ID `com.jangeby.system`. Pastikan App ID dan provisioning profile mengaktifkan Push Notifications. Entitlement menggunakan `development` pada Debug dan `production` pada Release/TestFlight.
3. Di Firebase project yang dipakai server, daftarkan/pilih Apple app dengan bundle ID yang sama. Unduh konfigurasi asli ke `iosApp/GoogleService-Info.plist`. File ini sengaja tidak dilacak Git dan sudah terdaftar sebagai resource target. Jangan mengganti konfigurasi Android untuk mengisi file Apple.
4. Unggah APNs authentication key yang valid berikut Key ID dan Team ID ke Firebase Project Settings > Cloud Messaging > Apple app configuration. Jangan memasukkan private key APNs ke repositori/aplikasi.
5. Server harus memiliki service-account Firebase yang valid untuk project yang sama. Kegagalan OAuth `invalid_grant / Invalid JWT Signature` perlu diperbaiki pada kredensial; perubahan payload tidak bisa memperbaiki key yang tidak valid.

## Alur push

Swift AppDelegate mengurus izin notifikasi, registrasi APNs, pemetaan token APNs ke Firebase, refresh token, dan banner foreground. Swizzling dimatikan secara eksplisit. Bridge Kotlin menyimpan token FCM terbaru dan mengirimkannya melalui endpoint autentikasi yang sudah ada setelah login; kegagalan koneksi sementara dicoba ulang. Token tidak dicetak ke log.

## Pengujian wajib pada iPhone fisik

- Instal build iOS baru. Uji izin kamera pertama kali: preview kamera depan terlihat, tepat satu wajah terdeteksi, tombol capture menghasilkan JPEG valid dan form memperoleh foto. Jangan kirim presensi produksi hanya untuk pengujian kamera.
- Tutup/buka kamera berulang, pindah background/foreground, dan rotasi/perubahan ukuran. Uji juga akses kamera ditolak lalu diaktifkan lewat Settings. Preview tidak boleh tetap hitam tanpa pesan; capture yang gagal harus bisa dicoba ulang.
- Masuk dengan akun uji dan izinkan notifikasi. Pastikan APNs berhasil terdaftar, token FCM disinkronkan, dan server dapat memperoleh access token Google.
- Kirim push ke perangkat uji dengan persetujuan pemiliknya: uji aplikasi foreground, background, dan ditutup. Verifikasi banner, suara (sesuai pengaturan perangkat), serta pembukaan aplikasi saat notifikasi diketuk.
- Periksa Notification Settings/Focus/Silent Mode bila pesan sudah diterima APNs tetapi banner/suara dibatasi sistem.

Build Android atau pemeriksaan statis tidak membuktikan kamera/APNs iPhone bekerja. Status akhir harus menyebutkan hasil build Xcode dan pengujian fisik, bukan menganggapnya lulus.
