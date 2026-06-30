package com.xapk.installer.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xapk.installer.ui.components.GlassmorphicCard
import com.xapk.installer.ui.components.GlowingButton
import com.xapk.installer.ui.components.PermissionCard
import com.xapk.installer.ui.theme.CyanSecondary
import com.xapk.installer.ui.theme.DarkPurpleBg
import com.xapk.installer.ui.theme.PurplePrimary
import com.xapk.installer.ui.theme.TextPrimary
import com.xapk.installer.ui.theme.TextSecondary
import com.xapk.installer.viewmodel.InstallViewModel

@Composable
fun MainScreen(
    viewModel: InstallViewModel,
    onSelectFileClick: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onRequestInstallPermission: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkPurpleBg)
            .verticalScroll(scrollState)
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "XINSTALLER",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            letterSpacing = 2.sp
        )
        Text(
            text = "Sederhana • Cepat • Open Source",
            fontSize = 14.sp,
            color = CyanSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Permissions Section
        val hasStorage = viewModel.storagePermissionGranted
        val hasInstall = viewModel.installPermissionGranted
        val allPermissionsGranted = hasStorage && hasInstall

        if (!allPermissionsGranted) {
            GlassmorphicCard {
                Text(
                    text = "Izin Diperlukan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = "Aplikasi memerlukan beberapa izin agar dapat membaca berkas XAPK dan memasangnya ke perangkat Anda.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (!hasStorage) {
                    PermissionCard(
                        title = "Akses File (Semua Berkas)",
                        description = "Diperlukan untuk membaca berkas .xapk dari memori dan memindahkan data OBB ke Android/obb.",
                        isGranted = false,
                        onGrantClick = onRequestStoragePermission
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (!hasInstall) {
                    PermissionCard(
                        title = "Pasang Aplikasi Tidak Dikenal",
                        description = "Diperlukan untuk menjalankan installer sistem Android dalam memasang Split APK.",
                        isGranted = false,
                        onGrantClick = onRequestInstallPermission
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // File Selector or Info Card
        val xapkInfo = viewModel.xapkInfo

        if (xapkInfo == null) {
            // Upload Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    .border(
                        2.dp,
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(
                                PurplePrimary.copy(alpha = 0.5f),
                                CyanSecondary.copy(alpha = 0.5f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(enabled = allPermissionsGranted, onClick = onSelectFileClick),
                contentAlignment = Alignment.Center
            ) {
                if (allPermissionsGranted) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "Pilih File",
                            tint = CyanSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Pilih File XAPK",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tekan untuk menelusuri memori perangkat",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Terkunci",
                            tint = TextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Fitur Dikunci",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Berikan izin di atas terlebih dahulu",
                            fontSize = 12.sp,
                            color = TextSecondary.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // File Detail Card
            GlassmorphicCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App Icon
                    if (xapkInfo.iconBitmap != null) {
                        Image(
                            bitmap = xapkInfo.iconBitmap.asImageBitmap(),
                            contentDescription = "Icon Aplikasi",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PurplePrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Default Icon",
                                tint = PurplePrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = xapkInfo.appName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = xapkInfo.packageName,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                // Detail Specs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailItem(label = "Versi", value = xapkInfo.versionName)
                    DetailItem(label = "Kode Versi", value = xapkInfo.versionCode.toString())
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailItem(label = "Jumlah APK", value = "${xapkInfo.apks.size} APK")
                    DetailItem(label = "Berkas OBB", value = "${xapkInfo.obbFiles.size} File")
                }

                Spacer(modifier = Modifier.height(24.dp))

                GlowingButton(
                    text = "MULAI INSTAL",
                    onClick = { viewModel.startInstallation(viewModel.selectedUri?.let { viewModel.getApplication() } ?: return@GlowingButton) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.resetState(viewModel.getApplication()) },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Batal / Ganti File", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
