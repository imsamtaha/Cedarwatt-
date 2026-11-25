package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.ui.platform.LocalContext
import com.example.data.remote.ocr.CloudOcrProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import com.example.data.local.BillFairnessStatus
import com.example.ui.ScannerUiState
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.CedarOrangeDark
import com.example.ui.theme.CedarOrangePrimary
import com.example.ui.theme.LuxuryCardBorder
import com.example.ui.theme.LuxurySurfaceDark
import com.example.ui.theme.LuxurySurfaceElevated
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberContainer
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainer
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedContainer
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import java.util.Locale

@Composable
fun AiBillScanner(
    scannerState: ScannerUiState,
    officialBenchmarkRateUsd: Double,
    onFieldUpdate: (provider: String?, month: String?, kwh: String?, amps: Int?, rateUsd: String?, fixedUsd: String?, fuelUsd: String?) -> Unit,
    onLoadPreset: (String) -> Unit,
    onTriggerScan: () -> Unit,
    onResetScan: () -> Unit,
    onSaveBill: () -> Unit,
    onFileComplaint: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectOcrProvider: (CloudOcrProvider) -> Unit = {},
    onTriggerCloudOcr: (Uri?) -> Unit = { onTriggerScan() }
) {
    val strings = LocalAppStrings.current
    val scrollState = rememberScrollState()
    var showLiveCamera by remember { mutableStateOf(false) }

    if (showLiveCamera) {
        BillCameraCaptureView(
            onPhotoCaptured = { uri ->
                showLiveCamera = false
                onTriggerCloudOcr(uri)
            },
            onCancel = {
                showLiveCamera = false
            }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = strings.scannerTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = strings.scannerSubtitle,
                    fontSize = 13.sp,
                    color = TextSecondaryDark
                )
            }

            Surface(
                color = CedarOrangePrimary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Powered",
                        tint = CedarOrangePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "CedarWatt AI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CedarOrangePrimary
                    )
                }
            }
        }

        // Cloud OCR Provider Selector
        LuxuryGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cloud_ocr_provider_selector"),
            cornerRadius = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = CedarOrangePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cloud OCR Engine",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                    Surface(
                        color = StatusGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(StatusGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Cloud Active",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusGreen
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CloudOcrProvider.values().forEach { provider ->
                        val isSelected = scannerState.selectedOcrProvider == provider
                        Surface(
                            onClick = { onSelectOcrProvider(provider) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ocr_chip_${provider.id}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) CedarOrangePrimary.copy(alpha = 0.2f) else LuxurySurfaceDark,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) CedarOrangePrimary else LuxuryCardBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = when (provider) {
                                        CloudOcrProvider.GOOGLE_CLOUD_VISION -> "Google Vision"
                                        CloudOcrProvider.GEMINI_DOCUMENT_AI -> "Gemini OCR"
                                        CloudOcrProvider.AWS_TEXTRACT -> "AWS Textract"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) CedarOrangePrimary else TextPrimaryDark
                                )
                                Text(
                                    text = when (provider) {
                                        CloudOcrProvider.GOOGLE_CLOUD_VISION -> "Vision API"
                                        CloudOcrProvider.GEMINI_DOCUMENT_AI -> "Multimodal"
                                        CloudOcrProvider.AWS_TEXTRACT -> "Textract Pipeline"
                                    },
                                    fontSize = 9.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                    }
                }
            }
        }

        // Stepper: 0 -> Viewfinder / Preset, 1 -> Scanning Animation, 2 -> Result & Actions
        when (scannerState.activeStep) {
            0 -> {
                // Viewfinder & Preset Selector
                ScannerViewfinderView(
                    onLoadPreset = onLoadPreset,
                    onTriggerScan = onTriggerScan,
                    onOpenLiveCamera = { showLiveCamera = true },
                    onPickPhoto = { uri -> onTriggerCloudOcr(uri) },
                    onTriggerCloudOcr = { onTriggerCloudOcr(null) }
                )
            }
            1 -> {
                // Scanning / Analyzing Animation View
                ScannerProcessingView(
                    progress = scannerState.scanProgress,
                    provider = scannerState.selectedOcrProvider,
                    stage = scannerState.ocrProcessingStage
                )
            }
            2 -> {
                // Validated Result & Traffic Light Feedback
                ScannerResultValidationView(
                    scannerState = scannerState,
                    officialBenchmarkRateUsd = officialBenchmarkRateUsd,
                    onFieldUpdate = onFieldUpdate,
                    onSaveBill = onSaveBill,
                    onFileComplaint = onFileComplaint,
                    onResetScan = onResetScan
                )
            }
        }
    }
}

@Composable
fun ScannerViewfinderView(
    onLoadPreset: (String) -> Unit,
    onTriggerScan: () -> Unit,
    onOpenLiveCamera: () -> Unit = {},
    onPickPhoto: (Uri) -> Unit = {},
    onTriggerCloudOcr: () -> Unit = onTriggerScan
) {
    val strings = LocalAppStrings.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickPhoto(uri)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    var flashEnabled by remember { mutableStateOf(false) }

    LuxuryGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .testTag("scanner_viewfinder"),
        backgroundColor = Color(0xFF0D1016)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Simulated Camera Feed Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Viewfinder Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = strings.frameGuideTitle,
                            color = CedarOrangePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = { flashEnabled = !flashEnabled },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Flash",
                            tint = if (flashEnabled) CedarOrangePrimary else TextSecondaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Simulated Bill Document Template in Center
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E232E).copy(alpha = 0.6f))
                        .border(1.dp, CedarOrangePrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ACHRAFIEH POWER CO.", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("AUG 2026", color = CedarOrangePrimary, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(6.dp)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Consumption: 285 kWh", color = TextSecondaryDark, fontSize = 11.sp)
                            Text("10 Amperes", color = TextSecondaryDark, fontSize = 11.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tariff: $0.520 / kWh", color = StatusRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Total: $178.70", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Laser Sweep Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.TopStart)
                            .padding(top = (180 * laserY).dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        CedarOrangePrimary,
                                        SunsetCoral,
                                        CedarOrangePrimary,
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // Viewfinder Footer
                Text(
                    text = strings.frameGuideSubtitle,
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    // Action Buttons Row: Live CameraX, Photo Picker, and Cloud OCR
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onOpenLiveCamera,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_open_live_camera"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CedarOrangePrimary,
                contentColor = Color.Black
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.capturePhoto, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("btn_upload_bill_gallery"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimaryDark
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload Bill", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            OutlinedButton(
                onClick = onTriggerCloudOcr,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("btn_capture_bill"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CedarOrangePrimary
                ),
                border = BorderStroke(1.dp, CedarOrangePrimary.copy(alpha = 0.7f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Run Cloud OCR", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Quick Test Presets Card
    LuxuryGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = strings.quickPresets,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark
            )
            Text(
                text = strings.fastDemoPresetsSubtitle,
                fontSize = 12.sp,
                color = TextSecondaryDark
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Preset Red
                Button(
                    onClick = {
                        onLoadPreset("RED")
                        onTriggerScan()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preset_red_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusRedContainer,
                        contentColor = StatusRed
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(strings.filterOvercharged, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("$0.54/kWh", fontSize = 10.sp)
                    }
                }

                // Preset Amber
                Button(
                    onClick = {
                        onLoadPreset("AMBER")
                        onTriggerScan()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preset_amber_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusAmberContainer,
                        contentColor = StatusAmber
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(strings.statusModerate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("$0.40/kWh", fontSize = 10.sp)
                    }
                }

                // Preset Green
                Button(
                    onClick = {
                        onLoadPreset("GREEN")
                        onTriggerScan()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preset_green_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusGreenContainer,
                        contentColor = StatusGreen
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(strings.statusFair, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("$0.35/kWh", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerProcessingView(
    progress: Float,
    provider: CloudOcrProvider = CloudOcrProvider.GOOGLE_CLOUD_VISION,
    stage: String = "Processing with Cloud OCR..."
) {
    val strings = LocalAppStrings.current
    LuxuryGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .testTag("scanner_processing_card")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(CedarOrangePrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = CedarOrangePrimary,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(60.dp)
                )
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = CedarOrangePrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                color = CedarOrangePrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = provider.displayName,
                    color = CedarOrangePrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = strings.analyzingTitle,
                color = TextPrimaryDark,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stage.ifBlank { strings.analyzingSubtitle },
                color = TextSecondaryDark,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CedarOrangePrimary,
                trackColor = LuxurySurfaceElevated
            )
        }
    }
}

@Composable
fun ScannerResultValidationView(
    scannerState: ScannerUiState,
    officialBenchmarkRateUsd: Double,
    onFieldUpdate: (provider: String?, month: String?, kwh: String?, amps: Int?, rateUsd: String?, fixedUsd: String?, fuelUsd: String?) -> Unit,
    onSaveBill: () -> Unit,
    onFileComplaint: () -> Unit,
    onResetScan: () -> Unit
) {
    val strings = LocalAppStrings.current
    var isEditing by remember { mutableStateOf(false) }

    // 1. Traffic Light Hero Banner
    val (statusBg, statusBorder, statusTitle, statusSubtitle, statusIcon) = when (scannerState.fairnessStatus) {
        BillFairnessStatus.RED -> Quintuple(
            StatusRedContainer,
            StatusRed,
            strings.criticalOverchargeDetected,
            String.format(Locale.US, strings.criticalOverchargeSubtitle, "$${scannerState.ratePerKwhUsd}", "$${String.format(Locale.US, "%.3f", officialBenchmarkRateUsd)}"),
            Icons.Default.Warning
        )
        BillFairnessStatus.AMBER -> Quintuple(
            StatusAmberContainer,
            StatusAmber,
            strings.moderateWarningDetected,
            strings.moderateWarningSubtitle,
            Icons.Default.Warning
        )
        BillFairnessStatus.GREEN -> Quintuple(
            StatusGreenContainer,
            StatusGreen,
            strings.fairCompliantDetected,
            strings.fairCompliantSubtitle,
            Icons.Default.CheckCircle
        )
    }

    LuxuryGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("traffic_light_result_banner"),
        backgroundColor = statusBg,
        borderColor = statusBorder,
        cornerRadius = 24.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusBorder,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusBorder
                    )
                }

                TrafficLightBadge(status = scannerState.fairnessStatus)
            }

            Text(
                text = statusSubtitle,
                fontSize = 13.sp,
                color = TextPrimaryDark.copy(alpha = 0.9f)
            )

            if (scannerState.overchargeAmountUsd > 0.0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(strings.overchargeDetectedAmount, color = TextSecondaryDark, fontSize = 11.sp)
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", scannerState.overchargeAmountUsd)} USD",
                                color = StatusRed,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "≈ ${String.format(Locale.US, "%,.0f", scannerState.overchargeAmountUsd * 89500)} LBP",
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    // 2. Extracted Data & Validation Details Card
    LuxuryGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.auditResultsTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                IconButton(
                    onClick = { isEditing = !isEditing },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = "Edit Fields",
                        tint = CedarOrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (!isEditing) {
                // Readonly display
                DetailRow(strings.providerField, scannerState.providerName)
                DetailRow(strings.monthField, scannerState.billMonth)
                DetailRow(strings.amperageField, "${scannerState.amperage} A")
                DetailRow(strings.kwhField, "${scannerState.kwhConsumption} kWh")
                DetailRow(strings.billedRateField, "$${scannerState.ratePerKwhUsd} / kWh")
                DetailRow(strings.fairCeilingBenchmark, "$${String.format(Locale.US, "%.3f", officialBenchmarkRateUsd)} / kWh")
                DetailRow(strings.fixedFeeField, "$${scannerState.fixedFeeUsd} + $${scannerState.fuelSurchargeUsd}")
                DetailRow(strings.calculatedTotal, "$${String.format(Locale.US, "%.2f", scannerState.totalAmountUsd)} USD", isHighlight = true)
            } else {
                // Edit mode
                OutlinedTextField(
                    value = scannerState.providerName,
                    onValueChange = { onFieldUpdate(it, null, null, null, null, null, null) },
                    label = { Text(strings.providerField) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = scannerState.kwhConsumption,
                        onValueChange = { onFieldUpdate(null, null, it, null, null, null, null) },
                        label = { Text(strings.kwhField) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )

                    OutlinedTextField(
                        value = scannerState.ratePerKwhUsd,
                        onValueChange = { onFieldUpdate(null, null, null, null, it, null, null) },
                        label = { Text(strings.billedRateField) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = scannerState.fixedFeeUsd,
                        onValueChange = { onFieldUpdate(null, null, null, null, null, it, null) },
                        label = { Text(strings.fixedFeeField) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )

                    OutlinedTextField(
                        value = scannerState.fuelSurchargeUsd,
                        onValueChange = { onFieldUpdate(null, null, null, null, null, null, it) },
                        label = { Text(strings.fuelFeeField) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )
                }

                // Amperage Selector
                Text(strings.amperageField, fontSize = 13.sp, color = TextSecondaryDark)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 20).forEach { amp ->
                        FilterChip(
                            selected = scannerState.amperage == amp,
                            onClick = { onFieldUpdate(null, null, null, amp, null, null, null) },
                            label = { Text("${amp}A") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CedarOrangePrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        }
    }

    // Cloud OCR Intelligence & Raw Text Dossier
    var showRawOcrText by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LuxuryGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_cloud_ocr_telemetry"),
        cornerRadius = 20.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = CedarOrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cloud OCR Telemetry",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Surface(
                    color = LuxurySurfaceElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = scannerState.selectedOcrProvider.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CedarOrangePrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Confidence Score", fontSize = 12.sp, color = TextSecondaryDark)
                Text(
                    text = String.format(Locale.US, "%.1f%%", scannerState.confidenceScore * 100),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusGreen
                )
            }

            if (scannerState.meterNumber.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Detected Meter", fontSize = 12.sp, color = TextSecondaryDark)
                    Text(
                        text = scannerState.meterNumber,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimaryDark
                    )
                }
            }

            if (scannerState.invoiceNumber.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Detected Invoice", fontSize = 12.sp, color = TextSecondaryDark)
                    Text(
                        text = scannerState.invoiceNumber,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimaryDark
                    )
                }
            }

            // Raw Text Expander
            Surface(
                onClick = { showRawOcrText = !showRawOcrText },
                color = LuxurySurfaceDark,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, LuxuryCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_toggle_raw_ocr")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = CedarOrangePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (showRawOcrText) "Hide Raw Extracted OCR Text" else "Inspect Raw Extracted OCR Text",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimaryDark
                        )
                    }

                    Icon(
                        imageVector = if (showRawOcrText) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = showRawOcrText) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = scannerState.ocrRawText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextSecondaryDark,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("CedarWatt OCR Text", scannerState.ocrRawText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Raw OCR text copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Raw Text", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // 3. Action Buttons
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Save to vault button
        Button(
            onClick = onSaveBill,
            enabled = !scannerState.isSaved,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_save_bill"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (scannerState.isSaved) StatusGreen else CedarOrangePrimary,
                contentColor = Color.Black
            )
        ) {
            Icon(
                imageVector = if (scannerState.isSaved) Icons.Default.CheckCircle else Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (scannerState.isSaved) strings.savedToVault else strings.saveAuditedBill,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // File municipality complaint button if overcharged
        if (scannerState.fairnessStatus == BillFairnessStatus.RED || scannerState.fairnessStatus == BillFairnessStatus.AMBER) {
            Button(
                onClick = onFileComplaint,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_file_municipality_complaint"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusRed,
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.generateMunicipalityLetter, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Scan another button
        OutlinedButton(
            onClick = onResetScan,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_scan_another"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.scanAnotherBill)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondaryDark, fontSize = 13.sp)
        Text(
            text = value,
            color = if (isHighlight) CedarOrangePrimary else TextPrimaryDark,
            fontSize = if (isHighlight) 15.sp else 13.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CedarOrangePrimary,
    unfocusedBorderColor = LuxuryCardBorder,
    focusedLabelColor = CedarOrangePrimary,
    unfocusedLabelColor = TextSecondaryDark,
    focusedTextColor = TextPrimaryDark,
    unfocusedTextColor = TextPrimaryDark,
    cursorColor = CedarOrangePrimary
)

data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
