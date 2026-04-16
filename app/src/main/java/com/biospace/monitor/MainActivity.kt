package com.biospace.monitor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biospace.monitor.ui.MainViewModel
import com.biospace.monitor.ui.screens.*
import com.biospace.monitor.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class MainActivity : ComponentActivity() {

    private var onLocationReady: ((android.location.Location) -> Unit)? = null

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) getGpsLocation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            BioSpaceTheme {
                BioSpaceApp(
                    onRequestGps = { callback ->
                        onLocationReady = callback
                        requestLocation()
                    }
                )
            }
        }
    }

    private fun requestLocation() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        if (ContextCompat.checkSelfPermission(this, fine) == PackageManager.PERMISSION_GRANTED) {
            getGpsLocation()
        } else {
            locationPermissionRequest.launch(arrayOf(fine, coarse))
        }
    }

    private fun getGpsLocation() {
        try {
            val client = LocationServices.getFusedLocationProviderClient(this)
            @Suppress("MissingPermission")
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    location?.let { onLocationReady?.invoke(it) }
                }
        } catch (e: Exception) { /* permission revoked */ }
    }
}

private enum class NavTab(val label: String) {
    DASHBOARD("SPACE"), IMF("IMF"), SCHUMANN("SR"), ANS("ANS"),
    ENVIRONMENT("ENV"), CME("CME"), IMAGES("IMG"),
    ASSESSMENT("ASSESS"), ALERTS("ALERTS"), CHAT("CHAT")
}

@Composable
fun BioSpaceApp(onRequestGps: (callback: (android.location.Location) -> Unit) -> Unit) {
    val vm: MainViewModel = viewModel()
    val sw by vm.spaceWeather.collectAsState()
    val weather by vm.weather.collectAsState()
    val sr by vm.srMetrics.collectAsState()
    val ans by vm.ansState.collectAsState()
    val assessment by vm.assessment.collectAsState()
    val chat by vm.chatMessages.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    var selectedTab by remember { mutableStateOf(NavTab.DASHBOARD) }
    var showLocationDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        ScanlineEffect()
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(sw.kp, sw.lastUpdated, isRefreshing, onRefresh = { vm.refresh() })
            NavTabRow(selectedTab) { selectedTab = it }
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                    .padding(horizontal = 14.dp).padding(bottom = 24.dp)
            ) {
                Spacer(Modifier.height(11.dp))
                when (selectedTab) {
                    NavTab.DASHBOARD -> DashboardScreen(sw)
                    NavTab.IMF -> ImfScreen(sw)
                    NavTab.SCHUMANN -> SchumannScreen(sr, sw)
                    NavTab.ANS -> AnsScreen(ans)
                    NavTab.ENVIRONMENT -> EnvironmentScreen(
                        weather,
                        onRequestLocation = { onRequestGps { loc -> vm.setGpsLocation(loc) } },
                        onSearchCity = { showLocationDialog = true }
                    )
                    NavTab.CME -> CmeScreen(sw.cmeEvents)
                    NavTab.IMAGES -> SolarImagesScreen()
                    NavTab.ASSESSMENT -> AssessmentScreen(assessment)
                    NavTab.ALERTS -> AlertsScreen(sw.alerts)
                    NavTab.CHAT -> ChatScreen(chat) { text, nick -> vm.sendChatMessage(text, nick) }
                }
            }
        }
        if (showLocationDialog) {
            LocationSearchDialog(
                onDismiss = { showLocationDialog = false },
                onUseGps = { onRequestGps { loc -> vm.setGpsLocation(loc) } },
                onCitySelected = { result ->
                    vm.setLocation(result.latitude, result.longitude,
                        "${result.name}${if (result.admin1 != null) ", ${result.admin1}" else ""}")
                },
                onSearch = { query -> vm.searchCity(query) }
            )
        }
    }
}

@Composable
private fun AppHeader(kp: Double, lastUpdated: Long, isRefreshing: Boolean, onRefresh: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "g"
    )
    Column(
        modifier = Modifier.fillMaxWidth().background(BgColor)
            .padding(horizontal = 14.dp).padding(top = 12.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("AUTONOMOUS SPACE", color = DimColor, fontSize = 9.sp, letterSpacing = 5.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("BIOSPACE MONITOR", color = Color(0xFFE8F4FF).copy(alpha = glowAlpha),
                fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
            if (isRefreshing) {
                CircularProgressIndicator(color = CyanColor, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onRefresh, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CyanColor, modifier = Modifier.size(16.dp))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Kp ${String.format("%.2f", kp)}", color = kpColor(kp), fontSize = 9.sp, letterSpacing = 2.sp)
            Box(modifier = Modifier.size(5.dp).clip(RoundedCornerShape(50)).background(CyanColor))
            if (lastUpdated > 0) {
                val timeStr = remember(lastUpdated) {
                    java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(lastUpdated))
                }
                Text("UPDATED $timeStr", color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(
            Brush.horizontalGradient(listOf(Color.Transparent, CyanColor.copy(0.3f), Color.Transparent))))
    }
}

@Composable
private fun NavTabRow(selected: NavTab, onSelect: (NavTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        NavTab.values().forEach { tab ->
            val active = tab == selected
            Box(
                modifier = Modifier.clip(RoundedCornerShape(7.dp))
                    .background(if (active) Color(0xFF071828) else CardColor)
                    .border(1.dp, if (active) CyanColor else BorderColor, RoundedCornerShape(7.dp))
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(tab.label, color = if (active) CyanColor else DimColor,
                    fontSize = 9.sp, letterSpacing = 2.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun ScanlineEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "scanY"
    )
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(2.dp)
            .offset(y = (maxHeight * offset))
            .background(Brush.horizontalGradient(
                listOf(Color.Transparent, CyanColor.copy(0.13f), Color.Transparent))))
    }
}
