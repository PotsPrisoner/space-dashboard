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
import com.biospace.monitor.data.SymptomLog
import com.biospace.monitor.ui.MainViewModel
import com.biospace.monitor.ui.theme.*

@Composable
fun SymptomsScreen(vm: MainViewModel) {
    val sw      by vm.sw.collectAsState()
    val burden  by vm.burden.collectAsState()
    val symptoms by vm.symptoms.collectAsState()
    var notes by remember { mutableStateOf("") }
    val fields = listOf("Lightheadedness","Heart Pounding","Fatigue","Brain Fog",
        "Chest Pain","Nausea","Shortness of Breath","Tremors","Blurred Vision","Headache")
    val sliders = fields.associateWith { remember { mutableStateOf(0f) } }
    val alertColor = Color(android.graphics.Color.parseColor(burden.alertLevel.colorHex))

    Column(Modifier.fillMaxSize().background(BgColor).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("SYMPTOM LOG", color=CyanColor, fontSize=11.sp, letterSpacing=3.sp, fontFamily=FontFamily.Monospace)
        Text("CORRELATED WITH LIVE SPACE WEATHER", color=DimColor, fontSize=8.sp, fontFamily=FontFamily.Monospace)

        Card(colors=CardDefaults.cardColors(containerColor=CardBg),border=BorderStroke(1.dp,alertColor.copy(0.4f)),modifier=Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), horizontalArrangement=Arrangement.SpaceEvenly, verticalAlignment=Alignment.CenterVertically) {
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("Kp INDEX",color=DimColor,fontSize=8.sp,fontFamily=FontFamily.Monospace)
                    Text("${sw.kp}",color=CyanColor,fontSize=20.sp,fontWeight=FontWeight.Bold,fontFamily=FontFamily.Monospace)
                }
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("ANS BURDEN",color=DimColor,fontSize=8.sp,fontFamily=FontFamily.Monospace)
                    Text("${burden.overall.toInt()}%",color=alertColor,fontSize=20.sp,fontWeight=FontWeight.Bold,fontFamily=FontFamily.Monospace)
                }
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("ALERT",color=DimColor,fontSize=8.sp,fontFamily=FontFamily.Monospace)
                    Text(burden.alertLevel.name,color=alertColor,fontSize=11.sp,fontWeight=FontWeight.Bold,fontFamily=FontFamily.Monospace)
                }
            }
        }

        Card(colors=CardDefaults.cardColors(containerColor=CardBg),border=BorderStroke(1.dp,BorderColor),modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("RATE YOUR SYMPTOMS  (0=none  10=severe)",color=DimColor,fontSize=9.sp,fontFamily=FontFamily.Monospace)
                Spacer(Modifier.height(10.dp))
                fields.forEach { field ->
                    val s = sliders[field]!!
                    Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                        Text(field, color=TextColor, fontSize=10.sp, fontFamily=FontFamily.Monospace, modifier=Modifier.width(158.dp))
                        Slider(value=s.value, onValueChange={s.value=it}, valueRange=0f..10f, steps=9,
                            modifier=Modifier.weight(1f),
                            colors=SliderDefaults.colors(thumbColor=CyanColor,activeTrackColor=CyanColor,inactiveTrackColor=BorderColor))
                        Text("${s.value.toInt()}", color=CyanColor, fontSize=12.sp, fontFamily=FontFamily.Monospace, modifier=Modifier.width(20.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value=notes, onValueChange={notes=it},
                    label={Text("Notes",fontFamily=FontFamily.Monospace)},
                    modifier=Modifier.fillMaxWidth(), minLines=2,
                    colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=CyanColor,unfocusedBorderColor=BorderColor,
                        focusedTextColor=TextColor,unfocusedTextColor=TextColor,containerColor=CardBg,cursorColor=CyanColor))
                Spacer(Modifier.height(10.dp))
                Button(onClick={
                    val sv = sliders.values.map{it.value.toInt()}
                    vm.logSymptom(SymptomLog(lightheadedness=sv[0],heartPounding=sv[1],fatigue=sv[2],
                        brainFog=sv[3],chestPain=sv[4],nausea=sv[5],shortBreath=sv[6],tremors=sv[7],
                        blurredVision=sv[8],headache=sv[9],notes=notes,kpAtLog=sw.kp,burdenAtLog=burden.overall))
                    sliders.values.forEach{it.value=0f}; notes=""
                }, modifier=Modifier.fillMaxWidth(),
                    colors=ButtonDefaults.buttonColors(containerColor=CyanColor.copy(0.2f))) {
                    Text("LOG SYMPTOMS", color=CyanColor, fontSize=10.sp, fontFamily=FontFamily.Monospace)
                }
            }
        }

        if (symptoms.isNotEmpty()) {
            Text("HISTORY", color=DimColor, fontSize=9.sp, letterSpacing=2.sp, fontFamily=FontFamily.Monospace)
            symptoms.take(20).forEach { s ->
                Card(colors=CardDefaults.cardColors(containerColor=CardBg),border=BorderStroke(1.dp,BorderColor),modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
                            Text(java.text.SimpleDateFormat("MM/dd HH:mm").format(java.util.Date(s.timestamp)),
                                color=DimColor, fontSize=9.sp, fontFamily=FontFamily.Monospace)
                            Text("Kp${s.kpAtLog} · ${s.burdenAtLog.toInt()}%", color=CyanColor, fontSize=9.sp, fontFamily=FontFamily.Monospace)
                        }
                        val total = listOf(s.lightheadedness,s.heartPounding,s.fatigue,s.brainFog,
                            s.chestPain,s.nausea,s.shortBreath,s.tremors,s.blurredVision,s.headache).sum()
                        Text("Total symptom score: $total/100", color=TextColor, fontSize=10.sp, fontFamily=FontFamily.Monospace)
                        if (s.notes.isNotBlank()) Text(s.notes, color=DimColor, fontSize=10.sp, fontFamily=FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
