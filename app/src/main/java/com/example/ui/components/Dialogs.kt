package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.localization.LocalAppStrings
import com.example.data.local.BillFairnessStatus
import com.example.data.local.ComplaintStatus
import com.example.data.local.MunicipalityComplaint
import com.example.data.local.ScannedBill
import com.example.data.local.UserProfile
import com.example.ui.theme.CedarOrangePrimary
import com.example.ui.theme.LuxuryCardBorder
import com.example.ui.theme.LuxurySurfaceDark
import com.example.ui.theme.LuxurySurfaceElevated
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BillDetailDialog(
    bill: ScannedBill,
    onDismiss: () -> Unit,
    onFileComplaint: (ScannedBill) -> Unit,
    onDelete: (ScannedBill) -> Unit,
    onAskCopilot: (ScannedBill) -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .imePadding()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, LuxuryCardBorder, RoundedCornerShape(24.dp))
                .testTag("bill_detail_dialog"),
            color = LuxurySurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = bill.providerName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = bill.billMonth + " • " + SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(bill.scanDate)),
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark)
                    }
                }

                // Traffic Light Status Hero
                TrafficLightBadge(status = bill.fairnessStatus)

                // Audit Breakdown Card
                Surface(
                    color = LuxurySurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailRow("Subscribed Capacity", "${bill.amperage} Amperes")
                        DetailRow("Metered Consumption", "${bill.kwhConsumption} kWh")
                        DetailRow("Billed Unit Rate", "$${String.format(Locale.US, "%.3f", bill.ratePerKwhUsd)} / kWh")
                        DetailRow("Official Ministry Rate", "$${String.format(Locale.US, "%.3f", bill.officialBenchmarkRateUsd)} / kWh")
                        DetailRow("Fixed Base Fee", "$${String.format(Locale.US, "%.2f", bill.fixedFeeUsd)}")
                        DetailRow("Fuel Surcharge", "$${String.format(Locale.US, "%.2f", bill.fuelSurchargeUsd)}")
                        DetailRow("Total Billed USD", "$${String.format(Locale.US, "%.2f", bill.totalAmountUsd)}", isHighlight = true)
                        DetailRow("Total in LBP", "${String.format(Locale.US, "%,.0f", bill.totalAmountLbp)} LBP")

                        if (bill.overchargeAmountUsd > 0.0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(LuxuryCardBorder)
                            )
                            DetailRow("Illegal Overcharge", "+$${String.format(Locale.US, "%.2f", bill.overchargeAmountUsd)} USD", isHighlight = true)
                        }
                    }
                }

                // Audit Summary
                Text(
                    text = bill.aiAuditSummary,
                    fontSize = 13.sp,
                    color = TextSecondaryDark,
                    lineHeight = 18.sp
                )

                // Copilot Energy Analysis Action
                Button(
                    onClick = {
                        onDismiss()
                        onAskCopilot(bill)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ask_copilot_explain_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CedarOrangePrimary,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.askCopilotButton,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (bill.overchargeAmountUsd > 0.0 && !bill.isComplaintFiled) {
                        Button(
                            onClick = {
                                onDismiss()
                                onFileComplaint(bill)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusRed,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("File Complaint", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onDelete(bill)
                            onDismiss()
                        },
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintFilingDialog(
    bill: ScannedBill,
    userProfile: UserProfile,
    onDismiss: () -> Unit,
    onSubmitComplaint: (bill: ScannedBill, municipality: String, violationType: String) -> Unit
) {
    var municipality by remember { mutableStateOf(userProfile.municipalityName) }
    var violationType by remember { mutableStateOf("Price Gouging (>25% official tariff limit)") }
    val violations = listOf(
        "Price Gouging (>25% official tariff limit)",
        "Illegal Fixed Amperage Fee (> legal ceiling)",
        "Unjustified Arbitrary Fuel Surcharge",
        "Refusal to Install / Read Energy Meter"
    )
    var expanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .imePadding()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, LuxuryCardBorder, RoundedCornerShape(24.dp))
                .testTag("complaint_filing_dialog"),
            color = LuxurySurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Lodge Municipal Complaint",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Ministry of Energy Tariff Violation",
                            fontSize = 12.sp,
                            color = StatusRed
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark)
                    }
                }

                // Summary Pill
                Surface(
                    color = StatusRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Target Provider: ${bill.providerName}", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Claim Amount: $${String.format(Locale.US, "%.2f", bill.overchargeAmountUsd)} USD", color = StatusRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Municipality Input
                OutlinedTextField(
                    value = municipality,
                    onValueChange = { municipality = it },
                    label = { Text("Designated Municipality") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors()
                )

                // Violation Selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = violationType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Primary Violation Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = outlinedFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        violations.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item, fontSize = 13.sp) },
                                onClick = {
                                    violationType = item
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "A legally structured complaint dossier will be generated citing Ministry of Energy decisions and sent for municipal tracking.",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )

                Button(
                    onClick = {
                        onSubmitComplaint(bill, municipality, violationType)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_submit_complaint_confirm"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusRed,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate & File Complaint Dossier", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ComplaintDetailDialog(
    complaint: MunicipalityComplaint,
    onDismiss: () -> Unit,
    onAdvanceStatus: (MunicipalityComplaint, ComplaintStatus) -> Unit,
    onDelete: (MunicipalityComplaint) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .imePadding()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, LuxuryCardBorder, RoundedCornerShape(24.dp))
                .testTag("complaint_detail_dialog"),
            color = LuxurySurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Case #${complaint.caseNumber}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CedarOrangePrimary
                        )
                        Text(
                            text = complaint.municipalityName,
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark)
                    }
                }

                // Status Tracker
                Surface(
                    color = LuxurySurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Current Case Status", color = TextSecondaryDark, fontSize = 11.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (complaint.status) {
                                            ComplaintStatus.DRAFT -> StatusAmber
                                            ComplaintStatus.SUBMITTED -> SunsetCoral
                                            ComplaintStatus.UNDER_REVIEW -> StatusAmber
                                            ComplaintStatus.MUNICIPALITY_INVESTIGATING -> CedarOrangePrimary
                                            ComplaintStatus.REFUND_ISSUED, ComplaintStatus.RESOLVED -> StatusGreen
                                        }
                                    )
                            )
                            Text(
                                text = complaint.status.name.replace("_", " "),
                                color = TextPrimaryDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = complaint.trackingNotes,
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                // Status Advancer (Interactive Demo Control)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Simulate Municipal Status Update:", fontSize = 12.sp, color = TextSecondaryDark)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ComplaintStatus.entries.take(4).forEach { st ->
                            FilterChip(
                                selected = complaint.status == st,
                                onClick = { onAdvanceStatus(complaint, st) },
                                label = { Text(st.name.take(6), fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CedarOrangePrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                // Formatted Letter Text
                Text("Generated Legal Complaint Letter:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = complaint.complaintLetterText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimaryDark.copy(alpha = 0.85f),
                        modifier = Modifier.padding(12.dp),
                        lineHeight = 16.sp
                    )
                }

                // Action Buttons: Copy, Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Complaint Letter", complaint.complaintLetterText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Complaint letter copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CedarOrangePrimary,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Letter", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            onDelete(complaint)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSettingsDialog(
    userProfile: UserProfile,
    onDismiss: () -> Unit,
    currentLanguage: AppLanguage = LocalAppLanguage.current,
    onLanguageChange: ((AppLanguage) -> Unit)? = null,
    onSave: (name: String, loc: String, mun: String, amps: Int, hasSolar: Boolean, solarKwp: Double, hasBat: Boolean, batKwh: Double) -> Unit
) {
    val strings = LocalAppStrings.current
    var name by remember { mutableStateOf(userProfile.fullName) }
    var location by remember { mutableStateOf(userProfile.location) }
    var municipality by remember { mutableStateOf(userProfile.municipalityName) }
    var amps by remember { mutableIntStateOf(userProfile.subscribedAmps) }
    var hasSolar by remember { mutableStateOf(userProfile.hasSolar) }
    var solarKwp by remember { mutableDoubleStateOf(userProfile.solarCapacityKwp) }
    var hasBattery by remember { mutableStateOf(userProfile.hasBattery) }
    var batteryKwh by remember { mutableDoubleStateOf(userProfile.batteryCapacityKwh) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .imePadding()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, LuxuryCardBorder, RoundedCornerShape(24.dp)),
            color = LuxurySurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.profileSettingsTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = strings.close, tint = TextSecondaryDark)
                    }
                }

                // Language Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = strings.languageSectionTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondaryDark
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentLanguage == AppLanguage.ARABIC,
                            onClick = { onLanguageChange?.invoke(AppLanguage.ARABIC) },
                            label = { Text("🇱🇧 العربية", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CedarOrangePrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = LuxurySurfaceElevated,
                                labelColor = TextPrimaryDark
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = currentLanguage == AppLanguage.ENGLISH,
                            onClick = { onLanguageChange?.invoke(AppLanguage.ENGLISH) },
                            label = { Text("🇬🇧 English", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CedarOrangePrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = LuxurySurfaceElevated,
                                labelColor = TextPrimaryDark
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.fullNameField) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(strings.neighborhoodField) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors()
                )

                OutlinedTextField(
                    value = municipality,
                    onValueChange = { municipality = it },
                    label = { Text(strings.municipalityField) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors()
                )

                // Amperage
                Text(strings.subscribedAmpsField, fontSize = 13.sp, color = TextSecondaryDark)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 20).forEach { a ->
                        FilterChip(
                            selected = amps == a,
                            onClick = { amps = a },
                            label = { Text("${a}A") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CedarOrangePrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                // Solar Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.solarInstalledToggle, color = TextPrimaryDark, fontSize = 14.sp)
                    Switch(
                        checked = hasSolar,
                        onCheckedChange = { hasSolar = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CedarOrangePrimary)
                    )
                }

                if (hasSolar) {
                    OutlinedTextField(
                        value = solarKwp.toString(),
                        onValueChange = { solarKwp = it.toDoubleOrNull() ?: solarKwp },
                        label = { Text(strings.solarCapacityField) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors()
                    )
                }

                // Battery Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.batteryInstalledToggle, color = TextPrimaryDark, fontSize = 14.sp)
                    Switch(
                        checked = hasBattery,
                        onCheckedChange = { hasBattery = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CedarOrangePrimary)
                    )
                }

                if (hasBattery) {
                    OutlinedTextField(
                        value = batteryKwh.toString(),
                        onValueChange = { batteryKwh = it.toDoubleOrNull() ?: batteryKwh },
                        label = { Text(strings.batteryCapacityField) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors()
                    )
                }

                Button(
                    onClick = {
                        onSave(name, location, municipality, amps, hasSolar, solarKwp, hasBattery, batteryKwh)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CedarOrangePrimary,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.saveProfileButton, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
