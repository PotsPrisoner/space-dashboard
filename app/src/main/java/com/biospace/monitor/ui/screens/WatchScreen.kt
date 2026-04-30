package com.biospace.monitor.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.biospace.monitor.data.Biometrics
import com.biospace.monitor.ui.MainViewModel
import com.biospace.monitor.ui.theme.*

@Composable
fun WatchScreen(vm: MainViewModel) {
    val bio by vm.bio.collectAsState()
    var showManual by remember { mutableStateOf(!bio.isWatchConnected) }
    var mHr    by remember { mutableStateOf(if(bio.heartRate>0) bio.heartRate.toString() else "") }
    var mSys   by remember { mutableStateOf(if(bio.bpSys>0) bio.bpSys.toString() else "") }
    var mDia   by remember { mutableStateOf(if(bio.bpDia>0) bio.bpDia.toString() else "") }
    var mSpo2  by remember { mutableStateOf(if(bio.spO2>0) bio.spO2.toString() else "") }
    var mStress by remember { mutableStateOf(if(bio.stressScore>0) bio.stressScore.toString() else "") }
    var mSleep  by remember { mutableStateOf(if(bio.sleepHours>0f) bio.sleepHours.toString() else "") }
    var mResp   by remember { mutableStateOf(if(bio.respirationRate>0) bio.respirationRate.toString() else "") }
    var mRmssd  by remember { mutableStateOf(if(bio.rmssd>0f) bio.rmssd.toString() else "") }
    var mSteps  by remember { mutableStateOf(if(bio.steps>0) bio.steps.toString() else "") }

    Column(Modifier.fillMaxSize().background(BgColor).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("BIOMETRICS", color = CyanColor, fontSize = 11.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
                Text(if (bio.isWatchConnected) "● WATCH CONNECTED" else "○ DISCONNECTED",
                    color = if (bio.isWatchConnected) GreenColor else DimColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
            Button(onClick = { if (bio.isWatchConnected) vm.watchRepo.disconnect() else vm.watchRepo.connect() },
                colors = ButtonDefaults.buttonColors(containerColor = if (bio.isWatchConnected) RedColor.copy(0.2f) else CyanColor.copy(0.2f))) {
                Text(if (bio.isWatchConnected) "DISCONNECT" else "CONNECT WATCH",
                    color = if (bio.isWatchConnected) RedColor else CyanColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = showManual, onCheckedChange = { showManual = it },
                colors = SwitchDefaults.colors(checkedThumbColor = CyanColor, checkedTrackColor = CyanColor.copy(0.3f)))
            Spacer(Modifier.width(8.dp))
            Text("MANUAL INPUT", color = if (showManual) CyanColor else DimColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }

        if (showManual) {
            Card(colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CyanColor.copy(0.3f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ENTER YOUR VITALS", color = CyanColor, fontSize = 9.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
                    Text("Watch data overrides manual when connected.", color = DimColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VInput("HEART RATE (bpm)", mHr, { mHr = it }, Modifier.weight(1f))
                        VInput("SpO2 (%)", mSpo2, { mSpo2 = it }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VInput("BP SYSTOLIC", mSys, { mSys = it }, Modifier.weight(1f))
                        VInput("BP DIASTOLIC", mDia, { mDia = it }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VInput("HRV RMSSD (ms)", mRmssd, { mRmssd = it }, Modifier.weight(1f))
                        VInput("STRESS (0-100)", mStress, { mStress = it }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VInput("SLEEP (hrs)", mSleep, { mSleep = it }, Modifier.weight(1f))
                        VInput("RESP RATE (brpm)", mResp, { mResp = it }, Modifier.weight(1f))
                    }
                    VInput("STEPS", mSteps, { mSteps = it }, Modifier.fillMaxWidth())
                    Button(onClick = {
                        vm.watchRepo.updateManual(bio.copy(
                            heartRate = mHr.toIntOrNull() ?: bio.heartRate,
                            bpSys = mSys.toIntOrNull() ?: bio.bpSys,
                            bpDia = mDia.toIntOrNull() ?: bio.bpDia,
                            spO2 = mSpo2.toIntOrNull() ?: bio.spO2,
                            stressScore = mStress.toIntOrNull() ?: bio.stressScore,
                            sleepHours = mSleep.toFloatOrNull() ?: bio.sleepHours,
                            respirationRate = mResp.toIntOrNull() ?: bio.respirationRate,
                            rmssd = mRmssd.toFloatOrNull() ?: bio.rmssd,
                            steps = mSteps.toIntOrNull() ?: bio.steps,
                            hrSource = if (mHr.isNotBlank()) "MANUAL" else bio.hrSource,
                            bpSource = if (mSys.isNotBlank()) "MANUAL" else bio.bpSource,
                            spO2Source = if (mSpo2.isNotBlank()) "MANUAL" else bio.spO2Source,
                            hrvSource = if (mRmssd.isNotBlank()) "MANUAL" else bio.hrvSource
                        ))
                    }, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanColor.copy(0.2f))) {
                        Text("APPLY VITALS", color = CyanColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Text("CURRENT READINGS", color = DimColor, fontSize = 9.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BioCard("HEART RATE", if(bio.heartRate>0) "${bio.heartRate}" else "—", "bpm", bio.hrSource,
                when { bio.heartRate>100->RedColor; bio.heartRate in 1..59->AmberColor; bio.heartRate>0->GreenColor; else->DimColor }, Modifier.weight(1f))
            BioCard("SpO2", if(bio.spO2>0) "${bio.spO2}" else "—", "%", bio.spO2Source,
                when { bio.spO2 in 1..92->RedColor; bio.spO2 in 93..95->AmberColor; bio.spO2>95->GreenColor; else->DimColor }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BioCard("BLOOD PRESSURE", if(bio.bpSys>0) "${bio.bpSys}/${bio.bpDia}" else "—", "mmHg", bio.bpSource,
                when { bio.bpSys>140->RedColor; bio.bpSys in 1..90->AmberColor; bio.bpSys>0->GreenColor; else->DimColor }, Modifier.weight(1f))
            BioCard("HRV RMSSD", if(bio.rmssd>0) "${bio.rmssd.toInt()}" else "—", "ms", bio.hrvSource,
                when { bio.rmssd in 0.1f..20f->RedColor; bio.rmssd in 20f..35f->AmberColor; bio.rmssd>35f->GreenColor; else->DimColor }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BioCard("SLEEP", if(bio.sleepHours>0) "${"%.1f".format(bio.sleepHours)}" else "—", "hrs", "WATCH",
                when { bio.sleepHours in 0.1f..5f->RedColor; bio.sleepHours in 5f..7f->AmberColor; bio.sleepHours>7f->GreenColor; else->DimColor }, Modifier.weight(1f))
            BioCard("STRESS", if(bio.stressScore>0) "${bio.stressScore}" else "—", "/100", "WATCH",
                when { bio.stressScore>70->RedColor; bio.stressScore>40->AmberColor; bio.stressScore>0->GreenColor; else->DimColor }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BioCard("STEPS", if(bio.steps>0) "${bio.steps}" else "—", "", "WATCH", CyanColor, Modifier.weight(1f))
            BioCard("RESP RATE", if(bio.respirationRate>0) "${bio.respirationRate}" else "—", "brpm", "MANUAL", CyanColor, Modifier.weight(1f))
        }
    }
}

@Composable
fun VInput(label: String, value: String, onChange: (String)->Unit, modifier: Modifier) {
    OutlinedTextField(value = value, onValueChange = onChange,
        label = { Text(label, fontSize = 8.sp, fontFamily = FontFamily.Monospace) },
        modifier = modifier, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanColor, unfocusedBorderColor = BorderColor,
            focusedTextColor = TextColor, unfocusedTextColor = TextColor, containerColor = CardBg, cursorColor = CyanColor,
            focusedLabelColor = CyanColor, unfocusedLabelColor = DimColor),
        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp))
}

@Composable
fun BioCard(label: String, value: String, unit: String, source: String, color: Color, modifier: Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor), modifier = modifier) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = DimColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text(source, color = if (source=="WATCH") CyanColor else AmberColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                if (unit.isNotBlank()) Text(" $unit", color = DimColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}
