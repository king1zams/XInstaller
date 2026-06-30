package com.xapk.installer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.xapk.installer.ui.screens.InstallScreen
import com.xapk.installer.ui.screens.MainScreen
import com.xapk.installer.ui.theme.XInstallerTheme
import com.xapk.installer.viewmodel.InstallViewModel
import com.xapk.installer.viewmodel.UiState

class MainActivity : ComponentActivity() {

    private val viewModel: InstallViewModel by viewModels()

    // Activity launcher for choosing files
    private val selectFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectXapk(this, uri)
        } else {
            Toast.makeText(this, "Tidak ada file yang dipilih", Toast.LENGTH_SHORT).show()
        }
    }

    // Activity launcher for legacy storage permissions (Android 10 and below)
    private val requestLegacyStorageLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        viewModel.checkPermissions(this)
        if (!isGranted) {
            Toast.makeText(this, "Izin penyimpanan ditolak", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if app was opened via "Open With" intent (clicking on a .xapk file in explorer)
        handleIntent(intent)

        setContent {
            XInstallerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color.Transparent
                ) {
                    Crossfade(
                        targetState = viewModel.uiState,
                        animationSpec = tween(400),
                        label = "ScreenTransition"
                    ) { state ->
                        when (state) {
                            is UiState.Idle, is UiState.ReadyToInstall -> {
                                MainScreen(
                                    viewModel = viewModel,
                                    onSelectFileClick = { selectFileLauncher.launch("*/*") },
                                    onRequestStoragePermission = { requestStoragePermission() },
                                    onRequestInstallPermission = { requestInstallPermission() }
                                )
                            }
                            else -> {
                                InstallScreen(
                                    viewModel = viewModel,
                                    onDoneClick = { viewModel.resetState(this@MainActivity) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refresh permissions whenever app comes back to focus (e.g. from Settings screen)
        viewModel.checkPermissions(this)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent != null && intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                viewModel.selectXapk(this, uri)
            }
        }
    }

    // Directs user to standard or advanced system permissions screen
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ requires MANAGE_EXTERNAL_STORAGE settings page redirect
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            // Android 10 and below, request runtime WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestLegacyStorageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    // Directs user to settings panel for package installation authority
    private fun requestInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } else {
            // Under Android 8.0, this permission was global and did not require a prompt
            Toast.makeText(this, "Izin pemasangan sudah aktif otomatis.", Toast.LENGTH_SHORT).show()
        }
    }
}
