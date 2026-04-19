package com.biospace.monitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biospace.monitor.model.SpaceWeatherState
import com.biospace.monitor.ui.components.*
import com.biospace.monitor.ui.theme.*

private enum class ImfTab { BZ, BT, COMPS, ALL }

@Composable
fun ImfScreen(sw: SpaceWeatherState) {
    var selectedTab by remember { mutableStateOf(ImfTab.BZ) }

    BioCard {
        CardTitle("IMF", "// INTERPLANETARY MAGNETIC FIELD · 7-DAY")

        // Tab row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ImfTab.entries.forEach { tab ->
                val active = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) Color(0xFF071828) else Color(0xFF040A18))
                        .border(1.dp, if (active) CyanColor else BorderColor, RoundedCornerShape(6.dp))
                        .clickable { selectedTab = tab }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        tab.name, color = if (active) CyanColor else DimColor,
                        fontSize = 9.sp, letterSpacing = 2.sp, fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        when (selectedTab) {
            ImfTab.BZ -> BzPanel(sw)
            ImfTab.BT -> BtPanel(sw)
            ImfTab.COMPS -> ComponentsPanel(sw)
            ImfTab.ALL -> AllPanel(sw)
        }
    }
}

@Composable
private fun BzPanel(sw: SpaceWeatherState) {
    val bzColor = bzColor(sw.bz)
    Column {
        Text(
            "${if (sw.bz >= 0) "+" else ""}${String.format("%.2f", sw.bz)} nT",
            color = bzColor, fontSize = 30.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(bzLabel(sw.bz), color = bzColor, fontSize = 11.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF020812))
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            if (sw.bzHistory.isNotEmpty()) {
                Sparkline(
                    values = sw.bzHistory,
                    color = bzColor,
                    isBipolar = true,
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Bz · 7-DAY HISTORY · SOUTHWARD = GEOEFFECTIVE",
            color = DimColor, fontSize = 7.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(10.dp))
        BzInterpretation(sw.bz)
    }
}

@Composable
private fun BtPanel(sw: SpaceWeatherState) {
    val btColor = if (sw.bt > 15) OrangeColor else if (sw.bt > 8) AmberColor else GreenColor
    Column {
        Text(
            "${String.format("%.2f", sw.bt)} nT",
            color = btColor, fontSize = 30.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            when {
                sw.bt > 20 -> "VERY STRONG TOTAL FIELD"
                sw.bt > 15 -> "STRONG FIELD"
                sw.bt > 8 -> "MODERATE FIELD"
                else -> "QUIET FIELD"
            }, color = btColor, fontSize = 11.sp, letterSpacing = 2.sp
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF020812))
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            if (sw.btHistory.isNotEmpty()) {
                Sparkline(
                    values = sw.btHistory,
                    color = btColor,
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Bt · TOTAL IMF MAGNITUDE · 7-DAY", color = DimColor, fontSize = 7.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun ComponentsPanel(sw: SpaceWeatherState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf(
            Triple("Bx (GSM)", sw.bx, sw.bxHistory to CyanColor),
            Triple("By (GSM)", sw.by, sw.byHistory to VioletColor)
        ).forEach { (label, value, hist) ->
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, color = DimColor, fontSize = 10.sp, letterSpacing = 2.sp)
                    Text(
                        "${if (value >= 0) "+" else ""}${String.format("%.2f", value)} nT",
                        color = hist.second, fontSize = 12.sp, fontFamily = FontFamily.Monospace
                    )
                }
                if (hist.first.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Sparkline(
                        values = hist.first.takeLast(60),
                        color = hist.second,
                        isBipolar = true,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    )
                }
            }
        }
        MetricRow("PHI (Clock Angle)", "${String.format("%.1f", sw.phi)}°")
        MetricRow("THETA (Elevation)", "${String.format("%.1f", sw.theta)}°")
    }
}

@Composable
private fun AllPanel(sw: SpaceWeatherState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF040A18)).border(1.dp, BorderColor, RoundedCornerShape(7.dp))
                .padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Bz", color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp)
                Text("${if (sw.bz >= 0) "+" else ""}${String.format("%.1f", sw.bz)}", color = bzColor(sw.bz), fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                if (sw.bzHistory.size > 1) Sparkline(values = sw.bzHistory.takeLast(30), color = bzColor(sw.bz), isBipolar = true, modifier = Modifier.fillMaxWidth().height(28.dp))
            }
            Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF040A18)).border(1.dp, BorderColor, RoundedCornerShape(7.dp))
                .padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Bt", color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp)
                val btColor = if (sw.bt > 15) OrangeColor else if (sw.bt > 8) AmberColor else GreenColor
                Text(String.format("%.1f", sw.bt), color = btColor, fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                if (sw.btHistory.size > 1) Sparkline(values = sw.btHistory.takeLast(30), color = btColor, modifier = Modifier.fillMaxWidth().height(28.dp))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF040A18)).border(1.dp, BorderColor, RoundedCornerShape(7.dp))
                .padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Bx", color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp)
                Text("${if (sw.bx >= 0) "+" else ""}${String.format("%.1f", sw.bx)}", color = CyanColor, fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                if (sw.bxHistory.size > 1) Sparkline(values = sw.bxHistory.takeLast(30), color = CyanColor, isBipolar = true, modifier = Modifier.fillMaxWidth().height(28.dp))
            }
            Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF040A18)).border(1.dp, BorderColor, RoundedCornerShape(7.dp))
                .padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("By", color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp)
                Text("${if (sw.by >= 0) "+" else ""}${String.format("%.1f", sw.by)}", color = VioletColor, fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                if (sw.byHistory.size > 1) Sparkline(values = sw.byHistory.takeLast(30), color = VioletColor, isBipolar = true, modifier = Modifier.fillMaxWidth().height(28.dp))
            }
        }
    }
}

@Composable
private fun BzInterpretation(bz: Double) {
    val text = when {
        bz < -10 -> "CRITICAL: Strongly southward Bz is the primary driver of geomagnetic storms. Magnetopause is severely compressed. Substantial energy transfer into the magnetosphere. Maximum ANS loading expected."
        bz < -5 -> "ACTIVE: Sustained southward Bz is opening the magnetosphere to solar wind energy. Significant storm ring current injection in progress. Dysautonomic individuals should expect elevated symptom load."
        bz < -2 -> "MILDLY GEOEFFECTIVE: Southward Bz creating moderate energy input. Enhanced HRV suppression and cortisol axis activation likely in sensitive individuals."
        bz > 5 -> "FAVORABLE: Strong northward Bz seals the magnetosphere. Minimal energy transfer. Best window for autonomic recovery."
        bz > 2 -> "QUIET: Northward Bz is protective. ANS baseline stress from geomagnetic sources is low."
        else -> "UNSTABLE: Near-zero Bz is the most unpredictable state. Any southward excursion can rapidly become geoeffective. Monitor closely. Diaphragmatic 0.1Hz breathing recommended for HRV stabilization."
    }
    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF040A18))
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(text, color = DimColor, fontSize = 9.sp, lineHeight = 15.sp, fontFamily = FontFamily.SansSerif)
    }
}
