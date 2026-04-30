package com.biospace.monitor.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.biospace.monitor.ui.MainViewModel
import com.biospace.monitor.ui.theme.*

@Composable
fun SpaceScreen(vm: MainViewModel) {
    val sw by vm.sw.collectAsState()
    Column(Modifier.fillMaxSize().background(BgColor).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("SPACE WEATHER", color = CyanColor, fontSize = 11.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
        Text("NOAA SWPC · NASA DONKI · LIVE", color = DimColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SCard("Kp INDEX", sw.kp.toString(), "",
                when { sw.kp>=7->"SEVERE STORM"; sw.kp>=5->"STORM"; sw.kp>=3->"ACTIVE"; else->"QUIET" },
                when { sw.kp>=5->RedColor; sw.kp>=3->AmberColor; else->GreenColor }, Modifier.weight(1f))
            SCard("GEOMAG STORM", if(sw.gstActive)"ACTIVE" else "QUIET","",
                if(sw.gstActive)"GST DETECTED" else "NOMINAL",
                if(sw.gstActive) RedColor else GreenColor, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SCard("SOLAR WIND", "${sw.swSpeed.toInt()}","km/s", sw.swTrend,
                when{sw.swSpeed>700->RedColor;sw.swSpeed>500->AmberColor;else->CyanColor}, Modifier.weight(1f))
            SCard("SW DENSITY","${"%.1f".format(sw.swDensity)}","p/cm³","PLASMA",
                if(sw.swDensity>10) RedColor else CyanColor, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SCard("IMF Bz","${"%.2f".format(sw.imfBz)}","nT",sw.imfTrend,
                when{sw.imfBz < -10->RedColor;sw.imfBz < -2->AmberColor;else->GreenColor}, Modifier.weight(1f))
            SCard("IMF Bt","${"%.2f".format(sw.imfBt)}","nT","TOTAL",
                if(sw.imfBt>15) RedColor else CyanColor, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SCard("CME SPEED","${sw.cmeSpeed.toInt()}","km/s",sw.cmeDirection,
                if(sw.cmeSpeed>800) RedColor else if(sw.cmeSpeed>500) AmberColor else GreenColor, Modifier.weight(1f))
            SCard("CME ARRIVAL",if(sw.cmeArrivalHrs>200)"N/A" else "~${sw.cmeArrivalHrs}",
                if(sw.cmeArrivalHrs<=200)"hrs" else "",if(sw.cmeArrivalHrs<48)"IMMINENT" else "DISTANT",
                if(sw.cmeArrivalHrs<48) RedColor else GreenColor, Modifier.weight(1f))
        }
        Text("SOLAR EVENTS (7 DAYS)", color = DimColor, fontSize = 9.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                Triple("FLR","${sw.flares.size}",if(sw.flares.isNotEmpty()) AmberColor else GreenColor),
                Triple("IPS","${sw.ipsCount}",if(sw.ipsCount>0) AmberColor else GreenColor),
                Triple("HSS",if(sw.hssActive)"YES" else "NO",if(sw.hssActive) AmberColor else GreenColor),
                Triple("MPC","${sw.mpcCount}",if(sw.mpcCount>0) AmberColor else GreenColor),
                Triple("RBE","${sw.rbeCount}",if(sw.rbeCount>0) AmberColor else GreenColor),
                Triple("SEP",if(sw.sepActive)"YES" else "NO",if(sw.sepActive) RedColor else GreenColor)
            ).forEach { (label, value, color) ->
                Card(colors=CardDefaults.cardColors(containerColor=CardBg),
                    border=BorderStroke(1.dp,BorderColor), modifier=Modifier.weight(1f)) {
                    Column(Modifier.padding(6.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(label, color=DimColor, fontSize=7.sp, fontFamily=FontFamily.Monospace)
                        Text(value, color=color, fontSize=12.sp, fontWeight=FontWeight.Bold, fontFamily=FontFamily.Monospace)
                    }
                }
            }
        }
        Text("IONOSPHERE & SCHUMANN", color=DimColor, fontSize=9.sp, letterSpacing=2.sp, fontFamily=FontFamily.Monospace)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            SCard("HEMI POWER","${sw.hemisphericPower.toInt()}","GW",sw.fountainDumping,
                when{sw.hemisphericPower>100->RedColor;sw.hemisphericPower>50->AmberColor;else->CyanColor}, Modifier.weight(1f))
            SCard("TEC","${"%.0f".format(sw.tec)}","TECU",
                if(sw.tecDelta>2)"ELEVATED" else "NORMAL",
                if(sw.tecDelta>2) AmberColor else CyanColor, Modifier.weight(1f))
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            SCard("SCHUMANN","${"%.2f".format(sw.srFundamental)}","Hz","FUNDAMENTAL",AmberColor,Modifier.weight(1f))
            SCard("SR DRIFT","${"%.3f".format(sw.srDrift)}","Hz","FROM 7.83",
                if(kotlin.math.abs(sw.srDrift)>0.1) AmberColor else GreenColor, Modifier.weight(1f))
        }
        if (sw.flares.isNotEmpty()) {
            Text("RECENT FLARES", color=DimColor, fontSize=9.sp, letterSpacing=2.sp, fontFamily=FontFamily.Monospace)
            sw.flares.forEach { f ->
                Card(colors=CardDefaults.cardColors(containerColor=CardBg),
                    border=BorderStroke(1.dp,BorderColor), modifier=Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(10.dp), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                        Text(f.flareClass, color=when{f.flareClass.startsWith("X")->RedColor;f.flareClass.startsWith("M")->AmberColor;else->GreenColor},
                            fontSize=14.sp, fontWeight=FontWeight.Bold, fontFamily=FontFamily.Monospace)
                        Text("${f.start}-${f.end}", color=DimColor, fontSize=10.sp, fontFamily=FontFamily.Monospace)
                        Text(f.direction, color=CyanColor, fontSize=10.sp, fontFamily=FontFamily.Monospace)
                        if (f.hasCme) Text("+CME", color=RedColor, fontSize=9.sp, fontFamily=FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun SCard(label:String,value:String,unit:String,status:String,color:Color,modifier:Modifier) {
    Card(colors=CardDefaults.cardColors(containerColor=CardBg),border=BorderStroke(1.dp,BorderColor),modifier=modifier) {
        Column(Modifier.padding(10.dp)) {
            Text(label,color=DimColor,fontSize=8.sp,fontFamily=FontFamily.Monospace)
            Row(verticalAlignment=Alignment.Bottom) {
                Text(value,color=color,fontSize=20.sp,fontWeight=FontWeight.Bold,fontFamily=FontFamily.Monospace)
                if(unit.isNotBlank()) Text(" $unit",color=DimColor,fontSize=9.sp,fontFamily=FontFamily.Monospace,modifier=Modifier.padding(bottom=2.dp))
            }
            Text(status,color=color.copy(0.7f),fontSize=8.sp,fontFamily=FontFamily.Monospace)
        }
    }
}
