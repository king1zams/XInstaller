package com.xapk.installer.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class XapkInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val apks: List<File>,
    val obbFiles: List<ObbFileInfo>,
    val iconBitmap: Bitmap?
)

data class ObbFileInfo(
    val relativePath: String, // e.g., Android/obb/com.example/main.obb
    val targetFile: File
)

object XapkParser {
    private const val TAG = "XapkParser"
    private const val TEMP_DIR_NAME = "xapk_temp"

    fun parseAndExtract(
        context: Context,
        xapkUri: Uri,
        onProgress: (status: String, percent: Float) -> Unit
    ): XapkInfo {
        val tempDir = File(context.cacheDir, TEMP_DIR_NAME)
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }
        tempDir.mkdirs()

        var manifestJsonString: String? = null
        var iconBytes: ByteArray? = null
        val extractedApkFiles = mutableListOf<File>()
        val extractedObbTempFiles = mutableListOf<Pair<String, File>>() // pair of: zipEntryName (e.g., Android/obb/com.example/file.obb) to tempFile

        // Step 1: Scan and Extract files from ZIP
        onProgress("Membaca file XAPK...", 0.1f)
        
        val inputStream: InputStream = context.contentResolver.openInputStream(xapkUri)
            ?: throw Exception("Gagal membuka file XAPK")
        
        ZipInputStream(inputStream).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                
                if (name == "manifest.json") {
                    manifestJsonString = zip.reader().readText()
                } else if (name == "icon.png" || name == "icon.webp" || name.endsWith("icon.png")) {
                    iconBytes = zip.readBytes()
                } else if (name.endsWith(".apk") && !name.contains("/")) {
                    // Extract APK to local cache
                    val apkFile = File(tempDir, name)
                    FileOutputStream(apkFile).use { out ->
                        zip.copyTo(out)
                    }
                    extractedApkFiles.add(apkFile)
                } else if (name.contains("Android/obb/") && name.endsWith(".obb")) {
                    // Extract OBB to local temporary cache first
                    val obbFileName = name.substringAfterLast("/")
                    val obbTempFile = File(tempDir, "obb_${System.currentTimeMillis()}_$obbFileName")
                    FileOutputStream(obbTempFile).use { out ->
                        zip.copyTo(out)
                    }
                    extractedObbTempFiles.add(Pair(name, obbTempFile))
                }
                
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        if (extractedApkFiles.isEmpty()) {
            throw Exception("Tidak ditemukan file APK di dalam XAPK")
        }

        // Step 2: Parse Manifest or Fallback
        onProgress("Memproses manifes XAPK...", 0.4f)
        
        var packageName = ""
        var appName = ""
        var versionName = ""
        var versionCode: Long = 0

        if (!manifestJsonString.isNullOrEmpty()) {
            try {
                val json = JSONObject(manifestJsonString!!)
                packageName = json.optString("package_name", json.optString("packageName", ""))
                appName = json.optString("name", json.optString("appName", json.optString("title", "")))
                versionName = json.optString("version_name", json.optString("versionName", ""))
                versionCode = json.optLong("version_code", json.optLong("versionCode", 0L))
            } catch (e: Exception) {
                Log.e(TAG, "Gagal mengurai manifest.json, menggunakan mode fallback", e)
            }
        }

        // Fallback: If manifest info is missing, extract from the main APK
        if (packageName.isEmpty() || appName.isEmpty()) {
            val mainApk = extractedApkFiles.firstOrNull { !it.name.contains("config") } ?: extractedApkFiles.first()
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(mainApk.absolutePath, 0)
            if (info != null) {
                packageName = info.packageName
                versionName = info.versionName ?: "1.0"
                versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    info.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    info.versionCode.toLong()
                }
                
                // Get app label
                info.applicationInfo.sourceDir = mainApk.absolutePath
                info.applicationInfo.publicSourceDir = mainApk.absolutePath
                appName = pm.getApplicationLabel(info.applicationInfo).toString()
            }
        }

        if (packageName.isEmpty()) {
            packageName = "com.unknown.app"
        }
        if (appName.isEmpty()) {
            appName = "Aplikasi Tanpa Nama"
        }

        // Step 3: Handle OBB Files
        onProgress("Menyiapkan file OBB...", 0.6f)
        val obbList = mutableListOf<ObbFileInfo>()
        val baseObbPath = Environment.getExternalStorageDirectory().absolutePath + "/Android/obb/$packageName"
        val obbDir = File(baseObbPath)
        
        if (extractedObbTempFiles.isNotEmpty()) {
            // Ensure target OBB directory exists
            try {
                if (!obbDir.exists()) {
                    obbDir.mkdirs()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal membuat direktori OBB: $baseObbPath", e)
            }

            for ((zipEntryName, tempFile) in extractedObbTempFiles) {
                val fileName = zipEntryName.substringAfterLast("/")
                val finalObbFile = File(obbDir, fileName)
                
                // Copy from temp file to actual OBB path
                try {
                    tempFile.inputStream().use { input ->
                        finalObbFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    obbList.add(ObbFileInfo(zipEntryName, finalObbFile))
                    tempFile.delete() // Clean up temp file
                } catch (e: Exception) {
                    Log.e(TAG, "Gagal menyalin file OBB ke $finalObbPath", e)
                    // If target write failed (possibly Scoped Storage restriction), keep it in cache
                    // and we will handle or report it
                    obbList.add(ObbFileInfo(zipEntryName, tempFile))
                }
            }
        }

        // Decode App Icon
        val iconBitmap = iconBytes?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }

        onProgress("Ekstraksi XAPK selesai!", 1.0f)
        
        return XapkInfo(
            packageName = packageName,
            appName = appName,
            versionName = versionName,
            versionCode = versionCode,
            apks = extractedApkFiles,
            obbFiles = obbList,
            iconBitmap = iconBitmap
        )
    }

    fun cleanCache(context: Context) {
        val tempDir = File(context.cacheDir, TEMP_DIR_NAME)
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }
    }
}
