package com.xapk.installer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xapk.installer.ui.components.GlassmorphicCard
import com.xapk.installer.ui.components.GlowingButton
import com.xapk.installer.ui.components.PremiumProgressBar
import com.xapk.installer.ui.theme.CyanSecondary
import com.xapk.installer.ui.theme.DarkPurpleBg
import com.xapk.installer.ui.theme.ErrorColor
import com.xapk.installer.ui.theme.PurplePrimary
import com.xapk.installer.ui.theme.SuccessColor
import com.xapk.installer.ui.theme.TextPrimary
import com.xapk.installer.ui.theme.TextSecondary
import com.xapk.installer.viewmodel.InstallViewModel
import com.xapk.installer.viewmodel.UiState

@Composable
fun InstallScreen(
    viewModel: InstallViewModel,
    onDoneClick: () -> Unit
) {
    val uiState = viewModel.uiState
    val statusText = viewModel.statusMessage
    val progress = viewModel.progress
    val xapkInfo = viewModel.xapkInfo

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkPurpleBg)
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is UiState.Extracting, is UiState.Installing -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Animated spinning ring for loading
                    val infiniteTransition = rememberInfiniteTransition(label = "RotationTransition")
                    val angle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "AngleAnimation"
                    )

                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .rotate(angle)
                            .background(
                                brush = Brush.sweepGradient(
                                    colors = listOf(PurplePrimary, CyanSecondary, PurplePrimary)
                                ),
                                shape = CircleShape
                            )
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(DarkPurpleBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsSuggest,
                            contentDescription = "Memproses",
                            tint = CyanSecondary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(36.dp))
                    Text(
                        text = if (uiState is UiState.Extracting) "Mengekstrak File..." else "Menginstal Aplikasi...",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = xapkInfo?.appName ?: "Mengurai XAPK",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(36.dp))

                    GlassmorphicCard {
                        PremiumProgressBar(progress = progress, statusText = statusText)
                    }
                }
            }

            is UiState.Success -> {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(500)),
                    exit = fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Success Glowing Icon
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(SuccessColor.copy(alpha = 0.2f), shape = CircleShape)
                                .padding(12.dp)
                                .background(SuccessColor, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selesai",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "Instalasi Berhasil!",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aplikasi ${xapkInfo?.appName ?: ""} telah berhasil dipasang ke sistem Android Anda.",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(48.dp))

                        GlowingButton(
                            text = "SELESAI",
                            onClick = onDoneClick
                        )
                    }
                }
            }

            is UiState.Failed -> {
                val errorMsg = (uiState as UiState.Failed).message
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(500)),
                    exit = fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Failure Glowing Icon
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(ErrorColor.copy(alpha = 0.2f), shape = CircleShape)
                                .padding(12.dp)
                                .background(ErrorColor, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Gagal",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "Instalasi Gagal",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = ErrorColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        GlassmorphicCard {
                            Text(
                                text = "Detail Kesalahan:",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMsg,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                textAlign = TextAlign.Left
                            )
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        GlowingButton(
                            text = "KEMBALI",
                            onClick = onDoneClick
                        )
                    }
                }
            }

            else -> {}
        }
    }
}
