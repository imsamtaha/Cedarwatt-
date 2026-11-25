package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BenchmarkTariff
import com.example.data.local.BillFairnessStatus
import com.example.data.local.EnergyBill
import com.example.data.local.MonthlyEnergyTrendPoint
import com.example.data.local.UserProfile
import com.example.ui.theme.CedarOrangeDark
import com.example.ui.theme.CedarOrangePrimary
import com.example.ui.theme.EnergySolar
import com.example.ui.theme.LuxuryCardBorder
import com.example.ui.theme.LuxurySurfaceDark
import com.example.ui.theme.LuxurySurfaceElevated
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberContainer
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainer
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedContainer
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.localization.LocalAppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BillFilterOption {
    ALL,
    OVERCHARGED,
    FAIR,
    COMPLAINT_FILED
}

enum class BillSortOption {
    NEWEST_SCAN,
    HIGHEST_SPEND,
    LARGEST_OVERCHARGE
}

/**
 * Dashboard UI that retrieves and displays processed 'EnergyBill' entities from the Room database.
 * Features comprehensive summary cards for total spending (USD & LBP), legal fairness baseline,
 * identified overcharges, energy consumption metrics, and an interactive bills list.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardView(
    bills: List<EnergyBill>,
    userProfile: UserProfile,
    benchmarkTariff: BenchmarkTariff,
    monthlyTrends: List<MonthlyEnergyTrendPoint> = emptyList(),
    activeUser: com.example.data.remote.supabase.AuthUser? = null,
    onOpenAuth: () -> Unit = {},
    onNavigateToScanner: () -> Unit,
    onNavigateToCopilot: () -> Unit,
    onNavigateToComplaints: () -> Unit,
    onSelectBill: (EnergyBill) -> Unit,
    onOpenSettings: () -> Unit,
    onFileComplaint: ((EnergyBill) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val scrollState = rememberScrollState()


    // Filter and Sort state for Room entities
    var activeFilter by remember { mutableStateOf(BillFilterOption.ALL) }
    var activeSort by remember { mutableStateOf(BillSortOption.NEWEST_SCAN) }

    // Spending summary aggregations from Room EnergyBill entities
    val totalSpendUsd by remember(bills) {
        derivedStateOf { bills.sumOf { it.totalAmountUsd } }
    }
    val totalSpendLbp by remember(bills) {
        derivedStateOf { bills.sumOf { it.totalAmountLbp } }
    }
    val totalOverchargeUsd by remember(bills) {
        derivedStateOf { bills.sumOf { it.overchargeAmountUsd } }
    }
    val totalKwh by remember(bills) {
        derivedStateOf { bills.sumOf { it.kwhConsumption } }
    }
    val fairSpendBaselineUsd by remember(totalSpendUsd, totalOverchargeUsd) {
        derivedStateOf { (totalSpendUsd - totalOverchargeUsd).coerceAtLeast(0.0) }
    }
    val avgRatePerKwh by remember(totalSpendUsd, totalKwh) {
        derivedStateOf {
            if (totalKwh > 0) totalSpendUsd / totalKwh else 0.0
        }
    }
    val overchargedCount by remember(bills) {
        derivedStateOf { bills.count { it.overchargeAmountUsd > 0 } }
    }
    val fairCount by remember(bills) {
        derivedStateOf { bills.count { it.overchargeAmountUsd <= 0 } }
    }
    val complaintFiledCount by remember(bills) {
        derivedStateOf { bills.count { it.isComplaintFiled } }
    }
    val highestBillSpend by remember(bills) {
        derivedStateOf { bills.maxOfOrNull { it.totalAmountUsd } ?: 0.0 }
    }

    // Filtered and sorted bills
    val displayedBills by remember(bills, activeFilter, activeSort) {
        derivedStateOf {
            val filtered = when (activeFilter) {
                BillFilterOption.ALL -> bills
                BillFilterOption.OVERCHARGED -> bills.filter { it.overchargeAmountUsd > 0 }
                BillFilterOption.FAIR -> bills.filter { it.overchargeAmountUsd <= 0 }
                BillFilterOption.COMPLAINT_FILED -> bills.filter { it.isComplaintFiled }
            }

            when (activeSort) {
                BillSortOption.NEWEST_SCAN -> filtered.sortedByDescending { it.scanDate }
                BillSortOption.HIGHEST_SPEND -> filtered.sortedByDescending { it.totalAmountUsd }
                BillSortOption.LARGEST_OVERCHARGE -> filtered.sortedByDescending { it.overchargeAmountUsd }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header: User Location & Room Database Status Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(CedarOrangePrimary, SunsetCoral)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = String.format(strings.greetingUser, userProfile.fullName),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${userProfile.location} • ${userProfile.subscribedAmps}A Sub",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Room Database Synced Tag
                Surface(
                    color = LuxurySurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LuxuryCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(StatusGreen)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = String.format(strings.billsCountTag, bills.size),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LuxurySurfaceElevated)
                        .testTag("dashboard_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = strings.settingsTitle,
                        tint = TextPrimaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // SUPABASE AUTHENTICATION ACCOUNT BANNER
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenAuth() }
                .testTag("dashboard_auth_banner"),
            shape = RoundedCornerShape(16.dp),
            color = LuxurySurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (activeUser != null && !activeUser.isGuest) StatusGreen.copy(alpha = 0.5f) else CedarOrangePrimary.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (activeUser != null && !activeUser.isGuest) StatusGreen.copy(alpha = 0.2f) else CedarOrangePrimary.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (activeUser != null && !activeUser.isGuest) Icons.Default.CheckCircle else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (activeUser != null && !activeUser.isGuest) StatusGreen else CedarOrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (activeUser != null && !activeUser.isGuest) {
                                    "Supabase Auth • ${activeUser.fullName}"
                                } else {
                                    "Supabase Authentication"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (activeUser != null && !activeUser.isGuest) StatusGreen.copy(alpha = 0.2f) else StatusAmber.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (activeUser != null && !activeUser.isGuest) "LINKED" else "GUEST",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeUser != null && !activeUser.isGuest) StatusGreen else StatusAmber,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (activeUser != null && !activeUser.isGuest) {
                                "All bills & complaints safely linked to ${activeUser.email}"
                            } else {
                                "Sign in or register to secure & link your energy records"
                            },
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                Button(
                    onClick = onOpenAuth,
                    modifier = Modifier.testTag("btn_dashboard_manage_auth"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeUser != null && !activeUser.isGuest) LuxurySurfaceDark else CedarOrangePrimary,
                        contentColor = if (activeUser != null && !activeUser.isGuest) TextPrimaryDark else Color.Black
                    ),
                    border = if (activeUser != null && !activeUser.isGuest) androidx.compose.foundation.BorderStroke(1.dp, LuxuryCardBorder) else null
                ) {
                    Text(
                        text = if (activeUser != null && !activeUser.isGuest) "Account" else "Sign In",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 1. PRIMARY HERO SUMMARY CARD: Total Spending & Legal Fairness

        LuxuryGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dashboard_hero_spending_card"),
            cornerRadius = 24.dp,
            borderColor = CedarOrangePrimary.copy(alpha = 0.5f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Card Header Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = CedarOrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.totalEnergySpending,
                            color = CedarOrangePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Surface(
                        color = if (totalOverchargeUsd > 0) StatusRedContainer else StatusGreenContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (totalOverchargeUsd > 0) Icons.Default.Warning else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (totalOverchargeUsd > 0) StatusRed else StatusGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (totalOverchargeUsd > 0) strings.overchargeDetected else strings.fairBilled100,
                                color = if (totalOverchargeUsd > 0) StatusRed else StatusGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Total Spend Large Figures
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = strings.cumulativeInvoiced,
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", totalSpendUsd)} USD",
                            color = TextPrimaryDark,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "≈ ${String.format(Locale.US, "%,.0f", totalSpendLbp)} LBP",
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = strings.identifiedOvercharge,
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "+$${String.format(Locale.US, "%.2f", totalOverchargeUsd)} USD",
                            color = if (totalOverchargeUsd > 0) StatusRed else StatusGreen,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(strings.billsContestedSummary, overchargedCount, bills.size),
                            color = if (totalOverchargeUsd > 0) StatusRed.copy(alpha = 0.85f) else TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }

                // Spending Proportion Bar (Fair Baseline vs Overcharge)
                if (totalSpendUsd > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val overchargeRatio = (totalOverchargeUsd / totalSpendUsd).toFloat().coerceIn(0f, 1f)
                        val fairRatio = 1f - overchargeRatio

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format(strings.legalFairPortion, "$${String.format(Locale.US, "%.2f", fairSpendBaselineUsd)}"),
                                fontSize = 11.sp,
                                color = StatusGreen
                            )
                            if (totalOverchargeUsd > 0) {
                                Text(
                                    text = String.format(strings.excessPercentage, (overchargeRatio * 100).toInt()),
                                    fontSize = 11.sp,
                                    color = StatusRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { fairRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = StatusGreen,
                            trackColor = if (totalOverchargeUsd > 0) StatusRed else LuxurySurfaceElevated
                        )
                    }
                }

                // Scan New Bill Action inside Hero
                Button(
                    onClick = onNavigateToScanner,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_hero_scan_now"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CedarOrangePrimary,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.scanAuditNewBill, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // 2. SECONDARY SPENDING SUMMARY METRICS (4 Metric Grid)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Metric 1: Total Energy Consumed
            SpendingMetricCard(
                title = strings.energyConsumed,
                value = "${totalKwh.toInt()} kWh",
                subtitle = String.format(strings.avgRatePerKwh, "$${String.format(Locale.US, "%.3f", avgRatePerKwh)}"),
                icon = Icons.Default.ElectricBolt,
                tint = CedarOrangePrimary,
                modifier = Modifier.weight(1f)
            )

            // Metric 2: Legal Fair Baseline
            SpendingMetricCard(
                title = strings.fairCeilingCost,
                value = "$${String.format(Locale.US, "%.2f", fairSpendBaselineUsd)}",
                subtitle = strings.officialMinistryCap,
                icon = Icons.Default.AccountBalance,
                tint = StatusGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Metric 3: Claimable Overcharge
            SpendingMetricCard(
                title = strings.claimableRefund,
                value = "$${String.format(Locale.US, "%.2f", totalOverchargeUsd)}",
                subtitle = if (totalOverchargeUsd > 0) strings.eligibleForGrievance else strings.noClaimsPending,
                icon = Icons.Default.Gavel,
                tint = if (totalOverchargeUsd > 0) StatusRed else StatusGreen,
                modifier = Modifier.weight(1f)
            )

            // Metric 4: Invoices Audited
            SpendingMetricCard(
                title = strings.auditedInvoices,
                value = "${bills.size} Records",
                subtitle = String.format(strings.invoicesFairContestedSummary, fairCount, overchargedCount),
                icon = Icons.Default.ReceiptLong,
                tint = SunsetCoral,
                modifier = Modifier.weight(1f)
            )
        }

        // 3. MONTHLY CONSUMPTION & TARIFF TRENDS CHART SUITE
        MonthlyEnergyTrendsChart(
            trends = monthlyTrends,
            benchmarkTariff = benchmarkTariff,
            userProfile = userProfile,
            onSelectBillById = { billId ->
                bills.firstOrNull { it.id == billId }?.let { onSelectBill(it) }
            },
            onFileComplaintForMonth = { trendPoint ->
                val matchingBill = bills.firstOrNull { it.id == trendPoint.associatedBillId }
                    ?: EnergyBill(
                        providerName = trendPoint.providerName,
                        billMonth = trendPoint.fullMonthName,
                        kwhConsumption = trendPoint.kwhConsumption,
                        amperage = trendPoint.amperage,
                        ratePerKwhUsd = trendPoint.billedRateUsd,
                        ratePerKwhLbp = trendPoint.billedRateUsd * 89500,
                        officialBenchmarkRateUsd = trendPoint.benchmarkRateUsd,
                        fixedFeeUsd = 12.0,
                        fuelSurchargeUsd = 0.0,
                        totalAmountUsd = trendPoint.totalBilledUsd,
                        totalAmountLbp = trendPoint.totalBilledUsd * 89500,
                        overchargeAmountUsd = trendPoint.overchargeUsd,
                        fairnessStatus = if (trendPoint.variancePct > 25) BillFairnessStatus.RED else BillFairnessStatus.AMBER,
                        aiAuditSummary = "Audited from historical monthly trend: +${trendPoint.variancePct}% above Ministry ceiling."
                    )
                onFileComplaint?.invoke(matchingBill)
            },
            modifier = Modifier.fillMaxWidth()
        )

        // 4. QUICK NAVIGATION CARDS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickNavCard(
                title = strings.quickCopilotTitle,
                subtitle = strings.quickCopilotSubtitle,
                icon = Icons.Default.Timeline,
                tint = EnergySolar,
                onClick = onNavigateToCopilot,
                modifier = Modifier.weight(1f)
            )

            QuickNavCard(
                title = strings.quickMunicipalityTitle,
                subtitle = strings.quickMunicipalitySubtitle,
                icon = Icons.Default.Gavel,
                tint = StatusRed,
                onClick = onNavigateToComplaints,
                modifier = Modifier.weight(1f)
            )
        }

        // 4. OFFICIAL MINISTRY BENCHMARK TARIFF WIDGET
        LuxuryGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(strings.ministryBenchmarkTitle, benchmarkTariff.monthName),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Surface(
                        color = CedarOrangePrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = strings.lebanonRegulationTag,
                            color = CedarOrangePrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    BenchmarkMetric(
                        label = strings.maxGenTariff,
                        value = "$${String.format(Locale.US, "%.3f", benchmarkTariff.officialGeneratorKwhUsd)}",
                        unit = "/kWh",
                        color = CedarOrangePrimary
                    )
                    BenchmarkMetric(
                        label = strings.edlOffPeak,
                        value = "$${String.format(Locale.US, "%.3f", benchmarkTariff.edlGridOffPeakUsd)}",
                        unit = "/kWh",
                        color = StatusGreen
                    )
                    BenchmarkMetric(
                        label = strings.diesel20L,
                        value = "$${String.format(Locale.US, "%.2f", benchmarkTariff.dieselPrice20LUsd)}",
                        unit = "USD",
                        color = SunsetCoral
                    )
                }
            }
        }

        // 5. PROCESSED 'EnergyBill' ENTITIES LIST FROM ROOM DATABASE
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.processedEnergyBills,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = strings.roomSyncSubtitle,
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }

                // Sort toggle button
                Surface(
                    color = LuxurySurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LuxuryCardBorder),
                    modifier = Modifier.clickable {
                        activeSort = when (activeSort) {
                            BillSortOption.NEWEST_SCAN -> BillSortOption.HIGHEST_SPEND
                            BillSortOption.HIGHEST_SPEND -> BillSortOption.LARGEST_OVERCHARGE
                            BillSortOption.LARGEST_OVERCHARGE -> BillSortOption.NEWEST_SCAN
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Sort Bills",
                            tint = CedarOrangePrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (activeSort) {
                                BillSortOption.NEWEST_SCAN -> strings.sortLatest
                                BillSortOption.HIGHEST_SPEND -> strings.sortSpend
                                BillSortOption.LARGEST_OVERCHARGE -> strings.sortOvercharge
                            },
                            fontSize = 11.sp,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Filter Chips Row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = activeFilter == BillFilterOption.ALL,
                    onClick = { activeFilter = BillFilterOption.ALL },
                    label = { Text("${strings.filterAll} (${bills.size})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CedarOrangePrimary.copy(alpha = 0.2f),
                        selectedLabelColor = CedarOrangePrimary,
                        containerColor = LuxurySurfaceDark,
                        labelColor = TextSecondaryDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = activeFilter == BillFilterOption.ALL,
                        borderColor = LuxuryCardBorder,
                        selectedBorderColor = CedarOrangePrimary
                    )
                )

                FilterChip(
                    selected = activeFilter == BillFilterOption.OVERCHARGED,
                    onClick = { activeFilter = BillFilterOption.OVERCHARGED },
                    label = { Text("${strings.filterOvercharged} ($overchargedCount)", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StatusRedContainer,
                        selectedLabelColor = StatusRed,
                        containerColor = LuxurySurfaceDark,
                        labelColor = TextSecondaryDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = activeFilter == BillFilterOption.OVERCHARGED,
                        borderColor = LuxuryCardBorder,
                        selectedBorderColor = StatusRed
                    )
                )

                FilterChip(
                    selected = activeFilter == BillFilterOption.FAIR,
                    onClick = { activeFilter = BillFilterOption.FAIR },
                    label = { Text("${strings.filterFair} ($fairCount)", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StatusGreenContainer,
                        selectedLabelColor = StatusGreen,
                        containerColor = LuxurySurfaceDark,
                        labelColor = TextSecondaryDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = activeFilter == BillFilterOption.FAIR,
                        borderColor = LuxuryCardBorder,
                        selectedBorderColor = StatusGreen
                    )
                )

                FilterChip(
                    selected = activeFilter == BillFilterOption.COMPLAINT_FILED,
                    onClick = { activeFilter = BillFilterOption.COMPLAINT_FILED },
                    label = { Text("${strings.filterClaimFiled} ($complaintFiledCount)", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LuxurySurfaceElevated,
                        selectedLabelColor = TextPrimaryDark,
                        containerColor = LuxurySurfaceDark,
                        labelColor = TextSecondaryDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = activeFilter == BillFilterOption.COMPLAINT_FILED,
                        borderColor = LuxuryCardBorder,
                        selectedBorderColor = LuxuryCardBorder
                    )
                )
            }

            // Bills Items List
            if (displayedBills.isEmpty()) {
                LuxuryGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (bills.isEmpty()) strings.noBillsYetTitle else strings.noBillsFilterTitle,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (bills.isEmpty()) strings.noBillsYetSubtitle else strings.noBillsFilterSubtitle,
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                        if (bills.isEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onNavigateToScanner,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CedarOrangePrimary,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text(strings.scanFirstBillButton, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                displayedBills.forEach { bill ->
                    EnergyBillCard(
                        bill = bill,
                        onClick = { onSelectBill(bill) },
                        onFileComplaint = if (bill.overchargeAmountUsd > 0 && !bill.isComplaintFiled && onFileComplaint != null) {
                            { onFileComplaint(bill) }
                        } else null
                    )
                }
            }
        }
    }
}

/**
 * Secondary spending summary metric card
 */
@Composable
fun SpendingMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    LuxuryGlassCard(
        modifier = modifier.testTag("spending_metric_${title.lowercase().replace(" ", "_")}"),
        cornerRadius = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextSecondaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Rich card representing an EnergyBill entity retrieved from Room
 */
@Composable
fun EnergyBillCard(
    bill: EnergyBill,
    onClick: () -> Unit,
    onFileComplaint: (() -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    val scanDateFormatted = remember(bill.scanDate) {
        SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(bill.scanDate))
    }

    LuxuryGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("energy_bill_card_${bill.id}"),
        cornerRadius = 20.dp,
        borderColor = if (bill.overchargeAmountUsd > 0) StatusRed.copy(alpha = 0.4f) else LuxuryCardBorder
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Top Row: Provider, Month Badge & Fairness Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = bill.providerName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${bill.billMonth} • ${String.format(strings.scannedOn, scanDateFormatted)}",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }

                TrafficLightBadge(status = bill.fairnessStatus)
            }

            // Middle Section: Total Spending, Consumption & Billed Rate
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LuxurySurfaceElevated.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Total Amount
                Column {
                    Text(strings.totalBilled, fontSize = 10.sp, color = TextSecondaryDark)
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", bill.totalAmountUsd)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CedarOrangePrimary
                    )
                    Text(
                        text = "≈ ${String.format(Locale.US, "%,.0f", bill.totalAmountLbp)} LBP",
                        fontSize = 10.sp,
                        color = TextSecondaryDark
                    )
                }

                // Energy Usage
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(strings.consumption, fontSize = 10.sp, color = TextSecondaryDark)
                    Text(
                        text = "${bill.kwhConsumption.toInt()} kWh",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = String.format(strings.subscribedAmps, bill.amperage),
                        fontSize = 10.sp,
                        color = TextSecondaryDark
                    )
                }

                // Rate vs Benchmark
                Column(horizontalAlignment = Alignment.End) {
                    Text(strings.billedRate, fontSize = 10.sp, color = TextSecondaryDark)
                    Text(
                        text = "$${String.format(Locale.US, "%.3f", bill.ratePerKwhUsd)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (bill.ratePerKwhUsd > bill.officialBenchmarkRateUsd) StatusRed else StatusGreen
                    )
                    Text(
                        text = String.format(strings.benchmarkCap, "$${String.format(Locale.US, "%.3f", bill.officialBenchmarkRateUsd)}"),
                        fontSize = 10.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            // Surcharges & Overcharge Highlight
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fixed Fee & Fuel
                Text(
                    text = String.format(strings.fixedAndFuelFee, "$${String.format(Locale.US, "%.2f", bill.fixedFeeUsd)}", "$${String.format(Locale.US, "%.2f", bill.fuelSurchargeUsd)}"),
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )

                // Overcharge Badge
                if (bill.overchargeAmountUsd > 0) {
                    Surface(
                        color = StatusRedContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = String.format(strings.overchargedBadge, "$${String.format(Locale.US, "%.2f", bill.overchargeAmountUsd)}"),
                            color = StatusRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        color = StatusGreenContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = strings.fairRateBadge,
                            color = StatusGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // AI Audit Summary Preview
            if (bill.aiAuditSummary.isNotBlank()) {
                Text(
                    text = bill.aiAuditSummary,
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Footer: Complaint Status / Quick Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (bill.isComplaintFiled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.grievanceFiledBadge,
                            color = StatusGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (bill.overchargeAmountUsd > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = StatusAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.eligibleForClaimNotice,
                            color = StatusAmber,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onFileComplaint != null) {
                        Surface(
                            color = StatusRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable(onClick = onFileComplaint)
                        ) {
                            Text(
                                text = strings.fileClaimButton,
                                color = StatusRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        color = LuxurySurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable(onClick = onClick)
                    ) {
                        Text(
                            text = strings.auditDetailsButton,
                            color = TextPrimaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LuxuryGlassCard(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("quick_nav_${title.lowercase().replace(" ", "_")}"),
        cornerRadius = 20.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondaryDark
            )
        }
    }
}

@Composable
fun BenchmarkMetric(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = TextSecondaryDark)
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = unit, fontSize = 10.sp, color = TextSecondaryDark, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}
