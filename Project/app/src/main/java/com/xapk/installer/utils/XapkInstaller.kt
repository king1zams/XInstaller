package com.xapk.installer.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import com.xapk.installer.receiver.InstallReceiver
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream

object XapkInstaller {
    private const val TAG = "XapkInstaller"

    fun install(
        context: Context,
        xapkInfo: XapkInfo,
        onStatusUpdate: (status: String, isFinished: Boolean, errorMsg: String?) -> Unit
    ) {
        val apks = xapkInfo.apks
        if (apks.isEmpty()) {
            onStatusUpdate("Gagal: Tidak ada file APK yang diekstrak", true, "Tidak ada file APK untuk diinstal")
            return
        }

        try {
            onStatusUpdate("Membuat sesi instalasi...", false, null)
            val packageInstaller = context.packageManager.packageInstaller
            
            // Set up Session Params
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(xapkInfo.packageName)
                setAppLabel(xapkInfo.appName)
                xapkInfo.iconBitmap?.let { setAppIcon(it) }
            }

            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            onStatusUpdate("Menyalin APK ke system...", false, null)
            
            // Write each APK into the installation session
            for (apkFile in apks) {
                Log.d(TAG, "Menulis file APK ke sesi: ${apkFile.name} (${apkFile.length()} bytes)")
                val sizeBytes = apkFile.length()
                var out: OutputStream? = null
                var input: InputStream? = null
                try {
                    out = session.openWrite(apkFile.name, 0, sizeBytes)
                    input = FileInputStream(apkFile)
                    
                    val buffer = ByteArray(65536)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        out.write(buffer, 0, bytesRead)
                    }
                    session.fsync(out)
                } finally {
                    input?.close()
                    out?.close()
                }
            }

            // Set up Intent to receive Status updates from the installation process
            // We store the session ID so our receiver knows which session is completed
            val intent = Intent(context, InstallReceiver::class.java).apply {
                action = InstallReceiver.ACTION_INSTALL_STATUS
                putExtra(InstallReceiver.EXTRA_SESSION_ID, sessionId)
                putExtra(InstallReceiver.EXTRA_PACKAGE_NAME, xapkInfo.packageName)
            }

            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                flags
            )

            onStatusUpdate("Meminta konfirmasi instalasi sistem...", false, null)
            
            // Register session ID in static callback map so receiver can update state in real-time
            InstallReceiver.registerCallback(sessionId) { status, isFinished, errorMsg ->
                onStatusUpdate(status, isFinished, errorMsg)
            }

            // Commit the session to system installer
            session.commit(pendingIntent.intentSender)
            session.close()

        } catch (e: Exception) {
            Log.e(TAG, "Instalasi XAPK gagal", e)
            onStatusUpdate("Gagal saat memproses instalasi: ${e.message}", true, e.message)
        }
    }
}
