package com.xapk.installer.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xapk.installer.utils.XapkInfo
import com.xapk.installer.utils.XapkInstaller
import com.xapk.installer.utils.XapkParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface UiState {
    object Idle : UiState
    object Extracting : UiState
    object ReadyToInstall : UiState
    object Installing : UiState
    object Success : UiState
    data class Failed(val message: String) : UiState
}

class InstallViewModel(application: Application) : AndroidViewModel(application) {

    var uiState by mutableStateOf<UiState>(UiState.Idle)
        private set

    var statusMessage by mutableStateOf("")
        private set

    var progress by mutableFloatStateOf(0f)
        private set

    var xapkInfo by mutableStateOf<XapkInfo?>(null)
        private set

    var selectedUri by mutableStateOf<Uri?>(null)
        private set

    var storagePermissionGranted by mutableStateOf(false)
    var installPermissionGranted by mutableStateOf(false)

    // Checks permission states dynamically
    fun checkPermissions(context: Context) {
        // 1. Storage Permission check (Android 11+ vs Legacy)
        storagePermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            // Under Android 11, standard check is run in MainActivity, but we can assume true if MainActivity handles it
            true 
        }

        // 2. Install Packages Permission check
        installPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    // Triggers file parsing and extraction
    fun selectXapk(context: Context, uri: Uri) {
        selectedUri = uri
        uiState = UiState.Extracting
        progress = 0f
        statusMessage = "Mulai mengekstrak file..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val info = XapkParser.parseAndExtract(context, uri) { status, percent ->
                    viewModelScope.launch(Dispatchers.Main) {
                        statusMessage = status
                        progress = percent
                    }
                }
                withContext(Dispatchers.Main) {
                    xapkInfo = info
                    uiState = UiState.ReadyToInstall
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    uiState = UiState.Failed(e.message ?: "Gagal mengekstrak berkas XAPK.")
                }
            }
        }
    }

    // Triggers actual package installation session
    fun startInstallation(context: Context) {
        val info = xapkInfo ?: return
        uiState = UiState.Installing
        progress = 0f
        statusMessage = "Memulai sesi instalasi..."

        viewModelScope.launch(Dispatchers.IO) {
            XapkInstaller.install(context, info) { status, isFinished, errorMsg ->
                viewModelScope.launch(Dispatchers.Main) {
                    statusMessage = status
                    if (isFinished) {
                        if (errorMsg != null) {
                            uiState = UiState.Failed(errorMsg)
                        } else {
                            uiState = UiState.Success
                            // Clean up extracted cache
                            XapkParser.cleanCache(context)
                        }
                    }
                }
            }
        }
    }

    // Reset installer state
    fun resetState(context: Context) {
        xapkInfo = null
        selectedUri = null
        progress = 0f
        statusMessage = ""
        uiState = UiState.Idle
        viewModelScope.launch(Dispatchers.IO) {
            XapkParser.cleanCache(context)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean cache just in case
        val context = getApplication<Application>().applicationContext
        XapkParser.cleanCache(context)
    }
}
