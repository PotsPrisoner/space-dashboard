package com.biospace.monitor.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.biospace.monitor.ui.MainViewModel
import com.biospace.monitor.ui.theme.*

@Composable
fun WeatherScreen(vm: MainViewModel) {
    val wx      by vm.wx.collectAsState()
    val useGps  by vm.useGps.collectAsState()
    val locName by vm.locName.collectAsState()
    val lat     by vm.lat.collectAsState()
    val lon     by vm.lon.collectAsState()
    var mLat  by remember { mutableStateOf(lat.toString()) }
    var mLon  by remember { mutableStateOf(lon.toString()) }
    var mName by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(BgColor).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("WEATHER", color=CyanColor, fontSize=11.sp, letterSpacing=3.sp, fontFamily=FontFamily.Monospace)
        Text("OPEN-METEO · REAL-TIME", color=DimColor, fontSize=8.sp, fontFamily=FontFamily.Monospace)

        Card(colors=CardDefaults.cardColors(containerColor=CardBg), border=BorderStroke(1.dp,BorderColor), modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("LOCATION SOURCE", color=DimColor, fontSize=9.sp, letterSpacing=2.sp, fontFamily=FontFamily.Monospace)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Switch(checked=useGps, onCheckedChange={ vm.setUseGps(it) },
                        colors=SwitchDefaults.colors(checkedThumbColor=CyanColor, checkedTrackColor=CyanColor.copy(0.3f)))
                    Spacer(Modifier.width(8.dp))
                    Text(if(useGps)"GPS (AUTOMATIC)" else "MANUAL ENTRY",
                        color=if(useGps) GreenColor else AmberColor, fontSize=10.sp, fontFamily=FontFamily.Monospace)
                }
                if (!useGps) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value=mLat, onValueChange={mLat=it},
                        label={ Text("LATITUDE", fontSize=8.sp, fontFamily=FontFamily.Monospace) },
                        modifier=Modifier.fillMaxWidth(), singleLine=true,
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
                        colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=CyanColor,unfocusedBorderColor=BorderColor,
                            focusedTextColor=TextColor,unfocusedTextColor=TextColor,containerColor=CardBg,cursorColor=CyanColor,
                            focusedLabelColor=CyanColor,unfocusedLabelColor=DimColor))
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value=mLon, onValueChange={mLon=it},
                        label={ Text("LONGITUDE", fontSize=8.sp, fontFamily=FontFamily.Monospace) },
                        modifier=Modifier.fillMaxWidth(), singleLine=true,
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
                        colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=CyanColor,unfocusedBorderColor=BorderColor,
                            focusedTextColor=TextColor,unfocusedTextColor=TextColor,containerColor=CardBg,cursorColor=CyanColor,
                            focusedLabelColor=CyanColor,unfocusedLabelColor=DimColor))
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value=mName, onValueChange={mName=it},
                        label={ Text("LOCATION NAME (optional)", fontSize=8.sp, fontFamily=FontFamily.Monospace) },
                        modifier=Modifier.fillMaxWidth(), singleLine=true,
                        colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=CyanColor,unfocusedBorderColor=BorderColor,
                            focusedTextColor=TextColor,unfocusedTextColor=TextColor,containerColor=CardBg,cursorColor=CyanColor,
                            focusedLabelColor=CyanColor,unfocusedLabelColor=DimColor))
                    Spacer(Modifier.height(10.dp))
                    Button(onClick={
                        val lt=mLat.toDoubleOrNull()?:return@Button
                        val ln=mLon.toDoubleOrNull()?:return@Button
                        vm.setLocation(lt,ln,mName)
                    }, modifier=Modifier.fillMaxWidth(),
                        colors=ButtonDefaults.buttonColors(containerColor=CyanColor.copy(0.2f))) {
                        Text("FETCH WEATHER", color=CyanColor, fontSize=9.sp, fontFamily=FontFamily.Monospace)
                    }
                }
                if (locName.isNotBlank()) { Spacer(Modifier.height(6.dp)); Text("📍 $locName", color=TextColor, fontSize=10.sp, fontFamily=FontFamily.Monospace) }
            }
        }

        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            SCard("TEMPERATURE","${wx.temp.toInt()}","°F",if(wx.temp>90)"HOT" else if(wx.temp>78)"WARM" else "MODERATE",
                if(wx.temp>90) RedColor else if(wx.temp>78) AmberColor else CyanColor, Modifier.weight(1f))
            SCard("HEAT INDEX","${wx.heatIndex.toInt()}","°F","FEELS LIKE",
                if(wx.heatIndex>95) RedColor else if(wx.heatIndex>80) AmberColor else CyanColor, Modifier.weight(1f))
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            SCard("HUMIDITY","${wx.humidity.toInt()}","%",if(wx.humidity>70)"HIGH" else if(wx.humidity>55)"MODERATE" else "LOW",
                if(wx.humidity>70) AmberColor else CyanColor, Modifier.weight(1f))
            SCard("DEWPOINT","${wx.dewpoint.toInt()}","°F","GAP: ${"%.0f".format(wx.temp-wx.dewpoint)}°",
                if(wx.temp-wx.dewpoint<10) AmberColor else CyanColor, Modifier.weight(1f))
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            SCard("PRESSURE","${"%.1f".format(wx.pressure)}","hPa",
                if(wx.pressureTrend < -2)"RAPID DROP" else if(wx.pressureTrend<0)"FALLING" else "STABLE",
                if(kotlin.math.abs(wx.pressureTrend)>2) RedColor else if(kotlin.math.abs(wx.pressureTrend)>0.5f) AmberColor else GreenColor, Modifier.weight(1f))
            SCard("WIND","${wx.wind.toInt()}","mph",if(wx.wind>20)"HIGH" else if(wx.wind>10)"MODERATE" else "LOW",
                if(wx.wind>20) AmberColor else CyanColor, Modifier.weight(1f))
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            SCard("UV INDEX","${"%.1f".format(wx.uvIndex)}","",if(wx.uvIndex>8)"VERY HIGH" else if(wx.uvIndex>5)"HIGH" else "MODERATE",
                if(wx.uvIndex>8) RedColor else if(wx.uvIndex>5) AmberColor else GreenColor, Modifier.weight(1f))
            SCard("AIR QUALITY","${wx.airQuality}","AQI",if(wx.airQuality>150)"UNHEALTHY" else if(wx.airQuality>100)"SENSITIVE" else "GOOD",
                if(wx.airQuality>150) RedColor else if(wx.airQuality>100) AmberColor else GreenColor, Modifier.weight(1f))
        }

        Card(colors=CardDefaults.cardColors(containerColor=CardBg),border=BorderStroke(1.dp,BorderColor),modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("ANS WEATHER TRIGGERS", color=CyanColor, fontSize=9.sp, letterSpacing=2.sp, fontFamily=FontFamily.Monospace)
                Spacer(Modifier.height(6.dp))
                listOf(
                    "Pressure drop >1 hPa/hr → orthostatic instability flare",
                    "Humidity >70% → thermoregulatory stress, blood pooling",
                    "Heat index >90°F → autonomic overload",
                    "AQI >100 → vagal withdrawal, inflammatory cascade"
                ).forEach { Text("◈ $it", color=TextColor, fontSize=10.sp, fontFamily=FontFamily.Monospace, lineHeight=15.sp) }
            }
        }
    }
}
