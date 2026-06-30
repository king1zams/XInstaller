package com.xapk.installer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log

class InstallReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "InstallReceiver"
        const val ACTION_INSTALL_STATUS = "com.xapk.installer.ACTION_INSTALL_STATUS"
        const val EXTRA_SESSION_ID = "com.xapk.installer.EXTRA_SESSION_ID"
        const val EXTRA_PACKAGE_NAME = "com.xapk.installer.EXTRA_PACKAGE_NAME"

        private val callbacks = HashMap<Int, (status: String, isFinished: Boolean, errorMsg: String?) -> Unit>()

        fun registerCallback(
            sessionId: Int,
            callback: (status: String, isFinished: Boolean, errorMsg: String?) -> Unit
        ) {
            callbacks[sessionId] = callback
        }

        fun unregisterCallback(sessionId: Int) {
            callbacks.remove(sessionId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return

        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: "Aplikasi"

        Log.d(TAG, "Menerima update instalasi. Session ID: $sessionId, Status: $status, Pesan: $message")

        val callback = callbacks[sessionId]

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // System requires user interaction (confirmation dialog)
                Log.d(TAG, "Memerlukan konfirmasi pengguna. Meluncurkan dialog sistem.")
                val confirmIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirmIntent)
                    callback?.invoke("Konfirmasi diperlukan oleh sistem Android...", false, null)
                } else {
                    callback?.invoke("Gagal meluncurkan konfirmasi instalasi sistem.", true, "Intent konfirmasi kosong")
                    unregisterCallback(sessionId)
                }
            }
            
            PackageInstaller.STATUS_SUCCESS -> {
                Log.d(TAG, "Instalasi berhasil untuk paket: $packageName")
                callback?.invoke("Aplikasi berhasil diinstal!", true, null)
                unregisterCallback(sessionId)
            }
            
            else -> {
                // Failure
                val errorMsg = message ?: getFriendlyErrorMessage(status)
                Log.e(TAG, "Instalasi gagal dengan kode status $status: $errorMsg")
                callback?.invoke("Gagal: $errorMsg", true, errorMsg)
                unregisterCallback(sessionId)
            }
        }
    }

    private fun getFriendlyErrorMessage(status: Int): String {
        return when (status) {
            PackageInstaller.STATUS_FAILURE -> "Instalasi ditolak oleh sistem."
            PackageInstaller.STATUS_FAILURE_ABORTED -> "Instalasi dibatalkan oleh pengguna atau sistem."
            PackageInstaller.STATUS_FAILURE_BLOCKED -> "Instalasi diblokir oleh kebijakan sistem (misal: MDM)."
            PackageInstaller.STATUS_FAILURE_CONFLICT -> "Terjadi konflik dengan versi aplikasi yang sudah ada."
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "Aplikasi tidak kompatibel dengan perangkat ini."
            PackageInstaller.STATUS_FAILURE_INVALID -> "Paket APK tidak valid atau rusak."
            PackageInstaller.STATUS_FAILURE_STORAGE -> "Penyimpanan penuh atau tidak tersedia."
            else -> "Terjadi kesalahan yang tidak diketahui (Kode: $status)."
        }
    }
}
