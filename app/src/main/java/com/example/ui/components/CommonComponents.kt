package com.example.ui.components

import com.example.ui.localization.LocalAppStrings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BillFairnessStatus
import com.example.data.local.EnergySource
import com.example.ui.theme.CedarOrangePrimary
import com.example.ui.theme.EnergyBattery
import com.example.ui.theme.EnergyBatteryContainer
import com.example.ui.theme.EnergyGenerator
import com.example.ui.theme.EnergyGeneratorContainer
import com.example.ui.theme.EnergyGrid
import com.example.ui.theme.EnergyGridContainer
import com.example.ui.theme.EnergySolar
import com.example.ui.theme.EnergySolarContainer
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

@Composable
fun LuxuryGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderColor: Color = LuxuryCardBorder,
    backgroundColor: Color = LuxurySurfaceDark,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.95f),
                        LuxurySurfaceElevated.copy(alpha = 0.90f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0.45f),
                        borderColor.copy(alpha = 0.10f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun TrafficLightBadge(
    status: BillFairnessStatus,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val strings = LocalAppStrings.current
    val (bgColor, textColor, label, icon) = when (status) {
        BillFairnessStatus.GREEN -> Quadruple(
            StatusGreenContainer,
            StatusGreen,
            strings.statusFair,
            Icons.Default.CheckCircle
        )
        BillFairnessStatus.AMBER -> Quadruple(
            StatusAmberContainer,
            StatusAmber,
            strings.statusModerate,
            Icons.Default.Warning
        )
        BillFairnessStatus.RED -> Quadruple(
            StatusRedContainer,
            StatusRed,
            strings.statusOverchargeAlert,
            Icons.Default.Warning
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.testTag("traffic_light_badge_${status.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            if (showLabel) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun EnergySourceBadge(
    source: EnergySource,
    modifier: Modifier = Modifier
) {
    val (bgColor, tintColor, label, icon) = when (source) {
        EnergySource.SOLAR -> Quadruple(
            EnergySolarContainer,
            EnergySolar,
            "Solar",
            Icons.Default.WbSunny
        )
        EnergySource.EDL_GRID -> Quadruple(
            EnergyGridContainer,
            EnergyGrid,
            "EDL Grid",
            Icons.Default.ElectricBolt
        )
        EnergySource.DIESEL_GENERATOR -> Quadruple(
            EnergyGeneratorContainer,
            EnergyGenerator,
            "Generator",
            Icons.Default.ElectricBolt
        )
        EnergySource.BATTERY -> Quadruple(
            EnergyBatteryContainer,
            EnergyBattery,
            "Battery",
            Icons.Default.BatteryChargingFull
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = tintColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
