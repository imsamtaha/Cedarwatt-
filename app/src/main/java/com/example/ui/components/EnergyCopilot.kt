package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.EnergySavingTip
import com.example.data.local.EnergySource
import com.example.data.local.EnergyTimelineSlot
import com.example.data.local.UserProfile
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.CedarOrangePrimary
import com.example.ui.theme.EnergyBattery
import com.example.ui.theme.EnergyGenerator
import com.example.ui.theme.EnergyGrid
import com.example.ui.theme.EnergySolar
import com.example.ui.theme.LuxuryCardBorder
import com.example.ui.theme.LuxurySurfaceDark
import com.example.ui.theme.LuxurySurfaceElevated
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import java.util.Locale

@Composable
fun EnergyCopilot(
    timelineSlots: List<EnergyTimelineSlot>,
    savingTips: List<EnergySavingTip>,
    userProfile: UserProfile,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val scrollState = rememberScrollState()
    var selectedSlotIndex by remember { mutableIntStateOf(4) } // default 12PM solar peak

    // Solar ROI Calculator States
    var systemSizeKwp by remember { mutableFloatStateOf(userProfile.solarCapacityKwp.toFloat().coerceIn(1f, 10f)) }
    var currentMonthlyBillUsd by remember { mutableFloatStateOf(140f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Copilot Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = strings.copilotTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = strings.copilotSubtitle,
                    fontSize = 13.sp,
                    color = TextSecondaryDark
                )
            }

            Surface(
                color = EnergySolar.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EnergySavingsLeaf,
                        contentDescription = null,
                        tint = EnergySolar,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format(strings.monthlySavingsEstimate, "62"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EnergySolar
                    )
                }
            }
        }

        // Realtime Energy Source Matrix Summary
        LuxuryGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.realTimeSystemDispatch,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = strings.activeOptimization,
                        fontSize = 12.sp,
                        color = StatusGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Solar Status
                    DispatchPill(
                        label = strings.solarOutput,
                        value = "${userProfile.solarCapacityKwp} kWp",
                        source = EnergySource.SOLAR,
                        modifier = Modifier.weight(1f)
                    )

                    // Battery Status
                    DispatchPill(
                        label = strings.sourceBattery,
                        value = "${userProfile.batteryCapacityKwh} kWh",
                        source = EnergySource.BATTERY,
                        modifier = Modifier.weight(1f)
                    )

                    // Generator Subscription
                    DispatchPill(
                        label = strings.sourceGenerator,
                        value = "${userProfile.subscribedAmps} A",
                        source = EnergySource.DIESEL_GENERATOR,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 24-Hour Energy Timeline
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.dispatchTimeline24h,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "24h",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
            }

            LuxuryGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("energy_timeline_card"),
                cornerRadius = 24.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    timelineSlots.forEachIndexed { index, slot ->
                        TimelineSlotRow(
                            slot = slot,
                            isSelected = selectedSlotIndex == index,
                            onClick = { selectedSlotIndex = index }
                        )
                    }

                    // Detailed breakdown of selected slot
                    if (selectedSlotIndex in timelineSlots.indices) {
                        val slot = timelineSlots[selectedSlotIndex]
                        Surface(
                            color = Color(0xFF1E2430),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${slot.timeLabel}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = CedarOrangePrimary
                                    )
                                    EnergySourceBadge(source = slot.primarySource)
                                }

                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", slot.costRatePerKwhUsd)}/kWh  |  ${slot.loadDemandKw} kW",
                                    fontSize = 12.sp,
                                    color = TextSecondaryDark
                                )

                                Text(
                                    text = "⚡ ${slot.recommendedAction}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimaryDark
                                )
                            }
                        }
                    }
                }
            }
        }

        // Smart Savings Recommendations
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.smartSavingTipsTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "${savingTips.size}",
                    fontSize = 12.sp,
                    color = CedarOrangePrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            savingTips.forEach { tip ->
                SmartTipCard(tip = tip)
            }
        }

        // Solar ROI & Generator Breaker Optimization Simulator
        LuxuryGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("solar_roi_simulator"),
            cornerRadius = 24.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.solarRoiTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Icon(
                        imageVector = Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = StatusGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = strings.solarRoiSubtitle,
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )

                // Slider 1: System Size
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.solarPvCapacityLabel, fontSize = 13.sp, color = TextSecondaryDark)
                        Text("${String.format(Locale.US, "%.1f", systemSizeKwp)} kWp", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CedarOrangePrimary)
                    }
                    Slider(
                        value = systemSizeKwp,
                        onValueChange = { systemSizeKwp = it },
                        valueRange = 1.0f..10.0f,
                        steps = 17,
                        colors = SliderDefaults.colors(
                            thumbColor = CedarOrangePrimary,
                            activeTrackColor = CedarOrangePrimary,
                            inactiveTrackColor = LuxurySurfaceElevated
                        )
                    )
                }

                // Slider 2: Generator Monthly Spend
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.currentGenBillLabel, fontSize = 13.sp, color = TextSecondaryDark)
                        Text("$${currentMonthlyBillUsd.toInt()} USD/mo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SunsetCoral)
                    }
                    Slider(
                        value = currentMonthlyBillUsd,
                        onValueChange = { currentMonthlyBillUsd = it },
                        valueRange = 50f..400f,
                        steps = 13,
                        colors = SliderDefaults.colors(
                            thumbColor = SunsetCoral,
                            activeTrackColor = SunsetCoral,
                            inactiveTrackColor = LuxurySurfaceElevated
                        )
                    )
                }

                // Calculation Result Callout
                val monthlyGenOffset = (systemSizeKwp * 135 * 0.36).coerceAtMost(currentMonthlyBillUsd.toDouble())
                val annualSavings = monthlyGenOffset * 12
                val estimatedSystemCost = systemSizeKwp * 650 + 1200 // PV panels + inverter + batteries
                val paybackYears = if (annualSavings > 0) estimatedSystemCost / annualSavings else 0.0

                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(strings.annualSavingsLabel, color = TextSecondaryDark, fontSize = 11.sp)
                            Text(
                                text = "$${String.format(Locale.US, "%,.0f", annualSavings)}/yr",
                                color = StatusGreen,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(LuxuryCardBorder)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(strings.systemPaybackLabel, color = TextSecondaryDark, fontSize = 11.sp)
                            Text(
                                text = "${String.format(Locale.US, "%.1f", paybackYears)} ${strings.yearsLabel}",
                                color = CedarOrangePrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(LuxuryCardBorder)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(strings.lifetimeRoiLabel, color = TextSecondaryDark, fontSize = 11.sp)
                            Text(
                                text = "+340%",
                                color = EnergySolar,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DispatchPill(
    label: String,
    value: String,
    source: EnergySource,
    modifier: Modifier = Modifier
) {
    val tint = when (source) {
        EnergySource.SOLAR -> EnergySolar
        EnergySource.BATTERY -> EnergyBattery
        EnergySource.DIESEL_GENERATOR -> EnergyGenerator
        EnergySource.EDL_GRID -> EnergyGrid
    }

    Surface(
        color = Color(0xFF1B202B),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = label, color = TextSecondaryDark, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = tint,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TimelineSlotRow(
    slot: EnergyTimelineSlot,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val sourceColor = when (slot.primarySource) {
        EnergySource.SOLAR -> EnergySolar
        EnergySource.EDL_GRID -> EnergyGrid
        EnergySource.DIESEL_GENERATOR -> EnergyGenerator
        EnergySource.BATTERY -> EnergyBattery
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) sourceColor.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) sourceColor.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1.2f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(sourceColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = slot.timeLabel,
                color = TextPrimaryDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        EnergySourceBadge(source = slot.primarySource)

        Text(
            text = if (slot.costRatePerKwhUsd == 0.0) "FREE" else "$${String.format(Locale.US, "%.2f", slot.costRatePerKwhUsd)}/kWh",
            color = if (slot.costRatePerKwhUsd == 0.0) StatusGreen else TextSecondaryDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SmartTipCard(tip: EnergySavingTip) {
    var isDismissed by remember { mutableStateOf(false) }

    AnimatedVisibility(visible = !isDismissed) {
        LuxuryGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = CedarOrangePrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = tip.category.name.replace("_", " "),
                            color = CedarOrangePrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = StatusGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Save +$${String.format(Locale.US, "%.2f", tip.estimatedMonthlySavingsUsd)}/mo",
                            color = StatusGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = tip.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Text(
                    text = tip.description,
                    fontSize = 12.sp,
                    color = TextSecondaryDark,
                    lineHeight = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Difficulty: ${tip.difficulty}  |  Impact: ${tip.impactScore}/100",
                        fontSize = 11.sp,
                        color = TextTertiaryDark()
                    )

                    Text(
                        text = "Dismiss",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { isDismissed = true }
                    )
                }
            }
        }
    }
}

@Composable
fun TextTertiaryDark() = Color(0xFF64748B)
