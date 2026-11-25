package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BenchmarkTariff
import com.example.data.local.EnergyBill
import com.example.data.local.MonthlyEnergyTrendPoint
import com.example.data.local.UserProfile
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.CedarOrangeDark
import com.example.ui.theme.CedarOrangePrimary
import com.example.ui.theme.EnergyBattery
import com.example.ui.theme.EnergySolar
import com.example.ui.theme.LuxuryCardBorder
import com.example.ui.theme.LuxurySurfaceDark
import com.example.ui.theme.LuxurySurfaceElevated
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import java.util.Locale

enum class ChartMetricMode {
    CONSUMPTION_KWH,
    TARIFF_BENCHMARK,
    SPEND_BREAKDOWN
}

/**
 * High-fidelity interactive chart component that visualizes monthly energy consumption
 * and tariff trends against official Lebanese Ministry of Energy regulated baselines.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlyEnergyTrendsChart(
    trends: List<MonthlyEnergyTrendPoint>,
    benchmarkTariff: BenchmarkTariff,
    userProfile: UserProfile,
    onSelectBillById: ((Long) -> Unit)? = null,
    onFileComplaintForMonth: ((MonthlyEnergyTrendPoint) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var activeMode by remember { mutableIntStateOf(ChartMetricMode.CONSUMPTION_KWH.ordinal) }
    var selectedIndex by remember(trends) {
        mutableIntStateOf(if (trends.isNotEmpty()) trends.lastIndex else 0)
    }

    val selectedPoint = trends.getOrNull(selectedIndex)

    // Household Consumption Benchmark based on Subscribed Amperage
    val (lowBenchKwh, typicalBenchKwh, highBenchKwh) = remember(userProfile.subscribedAmps) {
        when {
            userProfile.subscribedAmps <= 5 -> Triple(100.0, 180.0, 260.0)
            userProfile.subscribedAmps <= 10 -> Triple(200.0, 310.0, 450.0)
            userProfile.subscribedAmps <= 15 -> Triple(320.0, 480.0, 680.0)
            else -> Triple(450.0, 650.0, 950.0)
        }
    }

    val avgConsumption by remember(trends) {
        derivedStateOf {
            if (trends.isNotEmpty()) trends.map { it.kwhConsumption }.average() else 0.0
        }
    }

    val avgVariancePct by remember(trends) {
        derivedStateOf {
            if (trends.isNotEmpty()) trends.map { it.variancePct }.average().toInt() else 0
        }
    }

    val totalOverchargeSum by remember(trends) {
        derivedStateOf {
            trends.sumOf { it.overchargeUsd }
        }
    }

    LuxuryGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_energy_trends_card"),
        cornerRadius = 22.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // 1. Header & Context Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = CedarOrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.monthlyTrendsTitle,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = strings.monthlyTrendsSubtitle,
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        lineHeight = 15.sp
                    )
                }

                // Amperage Breaker Tag
                Surface(
                    color = CedarOrangePrimary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CedarOrangePrimary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${userProfile.subscribedAmps}A Subscribed",
                        color = CedarOrangePrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 2. View Mode Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LuxurySurfaceElevated)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ChartTabButton(
                    title = strings.tabConsumptionKwh,
                    icon = Icons.Default.ElectricBolt,
                    isSelected = activeMode == ChartMetricMode.CONSUMPTION_KWH.ordinal,
                    onClick = { activeMode = ChartMetricMode.CONSUMPTION_KWH.ordinal },
                    modifier = Modifier.weight(1f)
                )
                ChartTabButton(
                    title = strings.tabTariffRates,
                    icon = Icons.Default.AccountBalance,
                    isSelected = activeMode == ChartMetricMode.TARIFF_BENCHMARK.ordinal,
                    onClick = { activeMode = ChartMetricMode.TARIFF_BENCHMARK.ordinal },
                    modifier = Modifier.weight(1f)
                )
                ChartTabButton(
                    title = strings.tabSpendingCost,
                    icon = Icons.Default.ReceiptLong,
                    isSelected = activeMode == ChartMetricMode.SPEND_BREAKDOWN.ordinal,
                    onClick = { activeMode = ChartMetricMode.SPEND_BREAKDOWN.ordinal },
                    modifier = Modifier.weight(1f)
                )
            }

            // 3. Dynamic Interactive Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .testTag("canvas_trends_chart")
            ) {
                if (trends.isNotEmpty()) {
                    TrendsCanvas(
                        trends = trends,
                        mode = ChartMetricMode.values()[activeMode],
                        selectedIndex = selectedIndex,
                        typicalBenchmarkKwh = typicalBenchKwh,
                        officialTariffCap = benchmarkTariff.officialGeneratorKwhUsd,
                        onBarSelected = { selectedIndex = it }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.noBillsYetTitle,
                            color = TextSecondaryDark,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Chart Legends & Quick Tap Helper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (ChartMetricMode.values()[activeMode]) {
                        ChartMetricMode.CONSUMPTION_KWH -> {
                            ChartLegendItem(color = CedarOrangePrimary, label = "Billed kWh")
                            ChartLegendItem(color = EnergySolar, label = "Amperage Baseline", isDashed = true)
                        }
                        ChartMetricMode.TARIFF_BENCHMARK -> {
                            ChartLegendItem(color = StatusRed, label = "Billed Rate")
                            ChartLegendItem(color = StatusGreen, label = "Ministry Cap", isDashed = true)
                            ChartLegendItem(color = SunsetCoral.copy(alpha = 0.6f), label = "EDL Grid", isDashed = true)
                        }
                        ChartMetricMode.SPEND_BREAKDOWN -> {
                            ChartLegendItem(color = StatusGreen, label = "Fair Cost")
                            ChartLegendItem(color = StatusRed, label = "Overcharge Mark-up")
                        }
                    }
                }

                Text(
                    text = strings.tapToInspectPrompt,
                    fontSize = 10.sp,
                    color = TextSecondaryDark.copy(alpha = 0.8f)
                )
            }

            // 4. Detailed Inspected Month Metrics Card
            AnimatedVisibility(
                visible = selectedPoint != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedPoint?.let { point ->
                    InspectedMonthCard(
                        point = point,
                        benchmarkTariff = benchmarkTariff,
                        onViewBill = {
                            point.associatedBillId?.let { billId ->
                                onSelectBillById?.invoke(billId)
                            }
                        },
                        onFileComplaint = {
                            onFileComplaintForMonth?.invoke(point)
                        }
                    )
                }
            }

            // 5. Household Benchmarking & Efficiency Analysis Card
            HouseholdBenchmarkSummary(
                avgKwh = avgConsumption,
                typicalKwh = typicalBenchKwh,
                lowKwh = lowBenchKwh,
                highKwh = highBenchKwh,
                avgVariancePct = avgVariancePct,
                totalOverchargeSum = totalOverchargeSum,
                userAmps = userProfile.subscribedAmps
            )
        }
    }
}

/**
 * Tab switcher button inside the chart header.
 */
@Composable
private fun ChartTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(9.dp),
        color = if (isSelected) CedarOrangePrimary else Color.Transparent,
        contentColor = if (isSelected) Color.Black else TextSecondaryDark
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/**
 * High-performance Canvas rendering routine supporting 3 metrics:
 * 1. Consumption (kWh)
 * 2. Tariff Benchmark ($/kWh vs Ministry ceiling)
 * 3. Spending Breakdown ($ Fair vs Overcharge)
 */
@Composable
private fun TrendsCanvas(
    trends: List<MonthlyEnergyTrendPoint>,
    mode: ChartMetricMode,
    selectedIndex: Int,
    typicalBenchmarkKwh: Double,
    officialTariffCap: Double,
    onBarSelected: (Int) -> Unit
) {
    val count = trends.size
    val maxKwh = (trends.maxOfOrNull { it.kwhConsumption } ?: 350.0).coerceAtLeast(typicalBenchmarkKwh * 1.2)
    val maxTariff = (trends.maxOfOrNull { it.billedRateUsd } ?: 0.50).coerceAtLeast(officialTariffCap * 1.3)
    val maxSpend = (trends.maxOfOrNull { it.totalBilledUsd } ?: 200.0) * 1.15

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(trends, mode) {
                detectTapGestures { offset ->
                    val barWidth = size.width / count
                    val tappedIndex = (offset.x / barWidth).toInt().coerceIn(0, count - 1)
                    onBarSelected(tappedIndex)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val bottomPadding = 32.dp.toPx()
            val chartTop = 20.dp.toPx()
            val availableHeight = canvasHeight - bottomPadding - chartTop

            val barSlotWidth = canvasWidth / count
            val barWidth = (barSlotWidth * 0.52f).coerceIn(16.dp.toPx(), 44.dp.toPx())

            // 1. Draw horizontal reference grid lines
            val gridColor = LuxuryCardBorder.copy(alpha = 0.5f)
            val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

            // 3 horizontal level lines
            for (i in 1..3) {
                val yPos = chartTop + (availableHeight * (i / 4f))
                drawLine(
                    color = gridColor,
                    start = Offset(0f, yPos),
                    end = Offset(canvasWidth, yPos),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 2. Draw Mode-Specific Benchmark Target Line
            when (mode) {
                ChartMetricMode.CONSUMPTION_KWH -> {
                    val benchY = chartTop + availableHeight * (1f - (typicalBenchmarkKwh / maxKwh).toFloat().coerceIn(0f, 1f))
                    drawLine(
                        color = EnergySolar.copy(alpha = 0.85f),
                        start = Offset(0f, benchY),
                        end = Offset(canvasWidth, benchY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = dashedEffect
                    )
                }
                ChartMetricMode.TARIFF_BENCHMARK -> {
                    // Official Ministry Cap line
                    val capY = chartTop + availableHeight * (1f - (officialTariffCap / maxTariff).toFloat().coerceIn(0f, 1f))
                    drawLine(
                        color = StatusGreen.copy(alpha = 0.9f),
                        start = Offset(0f, capY),
                        end = Offset(canvasWidth, capY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = dashedEffect
                    )

                    // EDL Grid Reference line ($0.10)
                    val edlY = chartTop + availableHeight * (1f - (0.10 / maxTariff).toFloat().coerceIn(0f, 1f))
                    drawLine(
                        color = SunsetCoral.copy(alpha = 0.5f),
                        start = Offset(0f, edlY),
                        end = Offset(canvasWidth, edlY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashedEffect
                    )
                }
                ChartMetricMode.SPEND_BREAKDOWN -> {
                    // neutral baseline
                }
            }

            // 3. Draw Monthly Bars
            trends.forEachIndexed { index, point ->
                val centerX = (index * barSlotWidth) + (barSlotWidth / 2f)
                val left = centerX - (barWidth / 2f)
                val isSelected = (index == selectedIndex)

                when (mode) {
                    ChartMetricMode.CONSUMPTION_KWH -> {
                        val ratio = (point.kwhConsumption / maxKwh).toFloat().coerceIn(0.04f, 1f)
                        val barHeight = availableHeight * ratio
                        val top = (chartTop + availableHeight) - barHeight

                        val barBrush = Brush.verticalGradient(
                            colors = if (isSelected) {
                                listOf(CedarOrangePrimary, SunsetCoral)
                            } else {
                                listOf(CedarOrangePrimary.copy(alpha = 0.65f), CedarOrangeDark.copy(alpha = 0.45f))
                            },
                            startY = top,
                            endY = chartTop + availableHeight
                        )

                        // Draw Bar
                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // Selection glow
                        if (isSelected) {
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(left - 2.dp.toPx(), top - 2.dp.toPx()),
                                size = Size(barWidth + 4.dp.toPx(), barHeight + 2.dp.toPx()),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    ChartMetricMode.TARIFF_BENCHMARK -> {
                        val ratio = (point.billedRateUsd / maxTariff).toFloat().coerceIn(0.05f, 1f)
                        val barHeight = availableHeight * ratio
                        val top = (chartTop + availableHeight) - barHeight

                        val isOvercharged = point.variancePct > 10
                        val barColor = when {
                            point.variancePct > 25 -> StatusRed
                            point.variancePct > 5 -> StatusAmber
                            else -> StatusGreen
                        }

                        val barBrush = Brush.verticalGradient(
                            colors = if (isSelected) {
                                listOf(barColor, barColor.copy(alpha = 0.7f))
                            } else {
                                listOf(barColor.copy(alpha = 0.65f), barColor.copy(alpha = 0.35f))
                            },
                            startY = top,
                            endY = chartTop + availableHeight
                        )

                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        if (isSelected) {
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(left - 2.dp.toPx(), top - 2.dp.toPx()),
                                size = Size(barWidth + 4.dp.toPx(), barHeight + 2.dp.toPx()),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    ChartMetricMode.SPEND_BREAKDOWN -> {
                        val totalRatio = (point.totalBilledUsd / maxSpend).toFloat().coerceIn(0.05f, 1f)
                        val totalBarHeight = availableHeight * totalRatio
                        val fairRatio = (point.fairCostUsd / point.totalBilledUsd).toFloat().coerceIn(0.1f, 1f)
                        val fairBarHeight = totalBarHeight * fairRatio
                        val overchargeBarHeight = (totalBarHeight - fairBarHeight).coerceAtLeast(0f)

                        val baseY = chartTop + availableHeight

                        // Fair Cost Base (Green)
                        val fairTop = baseY - fairBarHeight
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(StatusGreen.copy(alpha = if (isSelected) 0.9f else 0.6f), StatusGreen.copy(alpha = 0.35f)),
                                startY = fairTop,
                                endY = baseY
                            ),
                            topLeft = Offset(left, fairTop),
                            size = Size(barWidth, fairBarHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Overcharge Surcharge Cap (Red)
                        if (overchargeBarHeight > 0f) {
                            val overchargeTop = fairTop - overchargeBarHeight
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    listOf(StatusRed.copy(alpha = if (isSelected) 0.95f else 0.7f), StatusRed.copy(alpha = 0.45f)),
                                    startY = overchargeTop,
                                    endY = fairTop
                                ),
                                topLeft = Offset(left, overchargeTop),
                                size = Size(barWidth, overchargeBarHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }

                        if (isSelected) {
                            drawRoundRect(
                                color = Color.White,
                                topLeft = Offset(left - 2.dp.toPx(), (baseY - totalBarHeight) - 2.dp.toPx()),
                                size = Size(barWidth + 4.dp.toPx(), totalBarHeight + 2.dp.toPx()),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        // X-Axis Month Labels Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            trends.forEachIndexed { index, point ->
                val isSelected = (index == selectedIndex)
                Surface(
                    onClick = { onBarSelected(index) },
                    color = if (isSelected) CedarOrangePrimary.copy(alpha = 0.22f) else Color.Transparent,
                    shape = RoundedCornerShape(6.dp),
                    border = if (isSelected) BorderStroke(1.dp, CedarOrangePrimary) else null,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = point.monthLabel,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) CedarOrangePrimary else TextSecondaryDark
                        )
                        if (point.isFromScannedBill) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(StatusGreen)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Inspected Month detail drawer showing full audit & tariff breakdown for the selected period.
 */
@Composable
private fun InspectedMonthCard(
    point: MonthlyEnergyTrendPoint,
    benchmarkTariff: BenchmarkTariff,
    onViewBill: () -> Unit,
    onFileComplaint: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isOvercharged = point.variancePct > 10

    Surface(
        color = LuxurySurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isOvercharged) StatusRed.copy(alpha = 0.5f) else LuxuryCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_inspected_month")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Month, Provider & Compliance Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = point.fullMonthName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (point.isFromScannedBill) {
                            Surface(
                                color = StatusGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Room Scanned",
                                    color = StatusGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Surface(
                                color = TextSecondaryDark.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Verified Baseline",
                                    color = TextSecondaryDark,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = point.providerName,
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }

                // Variance Badge
                Surface(
                    color = if (isOvercharged) StatusRed.copy(alpha = 0.15f) else StatusGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isOvercharged) StatusRed.copy(alpha = 0.6f) else StatusGreen.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOvercharged) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isOvercharged) StatusRed else StatusGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (point.variancePct > 0) "+${point.variancePct}% Over Cap" else "Legal & Fair",
                            color = if (isOvercharged) StatusRed else StatusGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 4-Grid Audit Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InspectedMetricBox(
                    label = "Consumption",
                    value = "${point.kwhConsumption.toInt()} kWh",
                    sub = "${point.amperage}A Breaker",
                    accent = CedarOrangePrimary,
                    modifier = Modifier.weight(1f)
                )
                InspectedMetricBox(
                    label = "Billed Unit Rate",
                    value = "$${String.format(Locale.US, "%.3f", point.billedRateUsd)}",
                    sub = "Cap: $${String.format(Locale.US, "%.3f", point.benchmarkRateUsd)}",
                    accent = if (isOvercharged) StatusRed else StatusGreen,
                    modifier = Modifier.weight(1f)
                )
                InspectedMetricBox(
                    label = "Total Invoiced",
                    value = "$${String.format(Locale.US, "%.1f", point.totalBilledUsd)}",
                    sub = "≈ ${String.format(Locale.US, "%,.0f", point.totalBilledUsd * 89500)} LBP",
                    accent = TextPrimaryDark,
                    modifier = Modifier.weight(1f)
                )
                InspectedMetricBox(
                    label = "Excess Overcharge",
                    value = "+$${String.format(Locale.US, "%.2f", point.overchargeUsd)}",
                    sub = if (point.overchargeUsd > 0) "Claimable" else "No Grievance",
                    accent = if (point.overchargeUsd > 0) StatusRed else StatusGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            // Action Shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (point.isFromScannedBill && point.associatedBillId != null) {
                    OutlinedButton(
                        onClick = onViewBill,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_inspect_view_bill"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CedarOrangePrimary.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CedarOrangePrimary)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Scanned Invoice", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (isOvercharged) {
                    Button(
                        onClick = onFileComplaint,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_inspect_file_complaint"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusRed,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("File Grievance Letter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectedMetricBox(
    label: String,
    value: String,
    sub: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = LuxurySurfaceDark,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LuxuryCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = label, fontSize = 10.sp, color = TextSecondaryDark, maxLines = 1)
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1
            )
            Text(text = sub, fontSize = 9.sp, color = TextSecondaryDark.copy(alpha = 0.8f), maxLines = 1)
        }
    }
}

/**
 * Benchmarking analysis comparing user's consumption to the Lebanese typical household brackets.
 */
@Composable
private fun HouseholdBenchmarkSummary(
    avgKwh: Double,
    typicalKwh: Double,
    lowKwh: Double,
    highKwh: Double,
    avgVariancePct: Int,
    totalOverchargeSum: Double,
    userAmps: Int
) {
    val strings = LocalAppStrings.current
    val progress = (avgKwh / highKwh).toFloat().coerceIn(0f, 1f)

    val tierTitle = when {
        avgKwh < lowKwh * 1.1 -> "Efficient Household"
        avgKwh <= typicalKwh * 1.15 -> "Typical Lebanese Apartment"
        else -> "High AC Peak Load"
    }

    val tierColor = when {
        avgKwh < lowKwh * 1.1 -> StatusGreen
        avgKwh <= typicalKwh * 1.15 -> EnergySolar
        else -> StatusAmber
    }

    Surface(
        color = LuxurySurfaceElevated.copy(alpha = 0.6f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, LuxuryCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CedarOrangePrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.benchmarkComparisonInsight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Surface(
                    color = tierColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = tierTitle,
                        color = tierColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Segmented Gauge Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = tierColor,
                    trackColor = LuxurySurfaceDark
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Low (<${lowKwh.toInt()} kWh)",
                        fontSize = 9.sp,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = "Typical (${typicalKwh.toInt()} kWh)",
                        fontSize = 9.sp,
                        color = EnergySolar,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Peak (> ${highKwh.toInt()} kWh)",
                        fontSize = 9.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            // Summary Text
            Text(
                text = "Your average monthly usage is ${avgKwh.toInt()} kWh across the audited period. Generator tariff rates averaged +$avgVariancePct% above the legal Ministry ceiling, accumulating $${String.format(Locale.US, "%.2f", totalOverchargeSum)} USD in potential recoverable overcharges.",
                fontSize = 11.sp,
                color = TextSecondaryDark,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun ChartLegendItem(
    color: Color,
    label: String,
    isDashed: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(if (isDashed) 12.dp else 8.dp)
                .height(if (isDashed) 2.dp else 8.dp)
                .clip(if (isDashed) RoundedCornerShape(1.dp) else CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = TextSecondaryDark)
    }
}
