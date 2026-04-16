package com.biospace.monitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.biospace.monitor.model.GeoResult
import com.biospace.monitor.ui.theme.*

@Composable
fun LocationSearchDialog(
    onDismiss: () -> Unit,
    onUseGps: () -> Unit,
    onCitySelected: (GeoResult) -> Unit,
    onSearch: suspend (String) -> List<GeoResult>
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<GeoResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF060F22))
                .border(1.dp, CyanColor.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                .padding(22.dp)
        ) {
            // Top glow line
            Box(modifier = Modifier.fillMaxWidth().height(2.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(Color.Transparent, CyanColor.copy(alpha = 0.77f), Color.Transparent)
                    )
                ))
            Spacer(Modifier.height(16.dp))

            Text("SET LOCATION", color = CyanColor, fontSize = 11.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Text("WEATHER · ENVIRONMENTAL DATA", color = DimColor, fontSize = 9.sp, letterSpacing = 3.sp)
            Spacer(Modifier.height(18.dp))

            // GPS Button
            Button(
                onClick = { onUseGps(); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF071828)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanColor.copy(0.55f))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📍  USE GPS", color = CyanColor, fontSize = 11.sp, letterSpacing = 2.sp)
                    Text("Auto-detect device location", color = DimColor, fontSize = 8.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("— OR SEARCH BY CITY —", color = DimColor, fontSize = 8.sp, letterSpacing = 3.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))

            // City search
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Monroe, Louisiana", color = DimColor, fontSize = 9.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanColor.copy(0.55f),
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextColor,
                        unfocusedTextColor = TextColor
                    )
                )
                Button(
                    onClick = {
                        isSearching = true
                        status = "SEARCHING..."
                        kotlinx.coroutines.GlobalScope.launch {
                            results = onSearch(query)
                            isSearching = false
                            status = if (results.isEmpty()) "NO RESULTS" else ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF071828)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanColor.copy(0.55f))
                ) {
                    Text("GO", color = CyanColor, fontSize = 10.sp)
                }
            }

            if (status.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(status, color = if (status.startsWith("NO")) RedColor else CyanColor, fontSize = 9.sp)
            }

            if (results.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                results.forEach { result ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF040A18))
                            .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                            .clickable { onCitySelected(result); onDismiss() }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(result.name, color = TextColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOfNotNull(result.admin1, result.country).joinToString(", "),
                                color = DimColor, fontSize = 9.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("SKIP", color = DimColor, fontSize = 9.sp, letterSpacing = 3.sp)
            }
        }
    }
}
