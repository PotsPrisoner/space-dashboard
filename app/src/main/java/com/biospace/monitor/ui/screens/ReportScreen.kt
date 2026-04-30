package com.biospace.monitor.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import com.biospace.monitor.ui.MainViewModel
import com.biospace.monitor.ui.theme.*

@Composable
fun ReportScreen(vm: MainViewModel) {
    val geminiKey   by vm.geminiKey.collectAsState()
    val reportOutput by vm.reportOutput.collectAsState()
    val reportLoading by vm.reportLoading.collectAsState()
    val burden by vm.burden.collectAsState()
    var keyInput by remember { mutableStateOf(geminiKey) }
    var clinical by remember { mutableStateOf(false) }
    val alertColor = Color(android.graphics.Color.parseColor(burden.alertLevel.colorHex))

    Column(Modifier.fillMaxSize().background(BgColor).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("HEALTH REPORT", color=CyanColor, fontSize=11.sp, letterSpacing=3.sp, fontFamily=FontFamily.Monospace)
        Text("AI-GENERATED · GEMINI 2.5 FLASH", color=DimColor, fontSize=8.sp, fontFamily=FontFamily.Monospace)

        Card(colors=CardDefaults.cardColors(containerColor=CardBg),border=BorderStroke(1.dp,AmberColor.copy(0.4f)),modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("⚠ DISCLAIMER", color=AmberColor, fontSize=10.sp, letterSpacing=2.sp, fontFamily=FontFamily.Monospace)
                Spacer(Modifier.height(4.dp))
                Text("This is an AI-generated informational report. It is NOT medical advice and was NOT produced by a licensed medical professional. The information is for educational purposes only. Always consult your physician or qualified healthcare provider.",
                    color=TextColor, fontSize=10.sp, fontFamily=FontFamily.Monospace, lineHeight=15.sp)
            }
        }

        Card(colors=CardDefaults.cardColors(containerColor=CardBg),border=BorderStroke(1.dp,alertColor.copy(0.4f)),modifier=Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), horizontalArrangement=Arrangement.SpaceEvenly, verticalAlignment=Alignment.CenterVertically) {
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("ANS BURDEN",color=DimColor,fontSize=8.sp,fontFamily=FontFamily.Monospace)
                    Text("${burden.overall.toInt()}%",color=alertColor,fontSize=26.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold,fontFamily=FontFamily.Monospace)
                }
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("ALERT LEVEL",color=DimColor,fontSize=8.sp,fontFamily=FontFamily.Monospace)
                    Text(burden.alertLevel.name,color=alertColor,fontSize=14.sp,fontFamily=FontFamily.Monospace)
                }
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("FLUCTUATION",color=DimColor,fontSize=8.sp,fontFamily=FontFamily.Monospace)
                    Text("${burden.fluctuation.toInt()}%",color=BlueColor,fontSize=20.sp,fontFamily=FontFamily.Monospace)
                }
            }
        }

        OutlinedTextField(value=keyInput, onValueChange={keyInput=it; vm.setGeminiKey(it)},
            label={Text("GEMINI API KEY  (aistudio.google.com — free)",fontSize=8.sp,fontFamily=FontFamily.Monospace)},
            modifier=Modifier.fillMaxWidth(), singleLine=true,
            colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=CyanColor,unfocusedBorderColor=BorderColor,
                focusedTextColor=TextColor,unfocusedTextColor=TextColor,containerColor=CardBg,cursorColor=CyanColor,
                focusedLabelColor=CyanColor,unfocusedLabelColor=DimColor))

        Row(verticalAlignment=Alignment.CenterVertically) {
            Switch(checked=clinical, onCheckedChange={clinical=it},
                colors=SwitchDefaults.colors(checkedThumbColor=CyanColor,checkedTrackColor=CyanColor.copy(0.3f)))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(if(clinical)"CLINICAL REPORT" else "GENERAL REPORT",
                    color=if(clinical) CyanColor else AmberColor, fontSize=10.sp, fontFamily=FontFamily.Monospace)
                Text(if(clinical)"Physiological mechanisms + research citations" else "Plain language, easy to understand",
                    color=DimColor, fontSize=8.sp, fontFamily=FontFamily.Monospace)
            }
        }

        Button(onClick={vm.generateReport(clinical)},
            enabled=!reportLoading && keyInput.isNotBlank(),
            modifier=Modifier.fillMaxWidth(),
            colors=ButtonDefaults.buttonColors(containerColor=CyanColor.copy(0.2f),disabledContainerColor=BorderColor)) {
            if (reportLoading) { CircularProgressIndicator(color=CyanColor,modifier=Modifier.size(16.dp),strokeWidth=2.dp); Spacer(Modifier.width(8.dp)) }
            Text(if(reportLoading)"GENERATING…" else "▸ GENERATE REPORT",
                color=if(reportLoading||keyInput.isBlank()) DimColor else CyanColor, fontSize=10.sp, fontFamily=FontFamily.Monospace)
        }

        if (reportOutput.isNotBlank()) {
            Card(colors=CardDefaults.cardColors(containerColor=CardBg),border=BorderStroke(1.dp,BorderColor),modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("GENERATED REPORT", color=CyanColor, fontSize=9.sp, letterSpacing=2.sp, fontFamily=FontFamily.Monospace)
                    Spacer(Modifier.height(8.dp))
                    Text(reportOutput, color=TextColor, fontSize=11.sp, fontFamily=FontFamily.Monospace, lineHeight=17.sp)
                }
            }
        }
    }
}
