package com.biospace.monitor.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.biospace.monitor.ui.MainViewModel
import com.biospace.monitor.ui.theme.*

@Composable
fun AppNavigation(vm: MainViewModel) {
    val nav = rememberNavController()
    val tabs = listOf(
        Triple("burden",   "BURDEN",    Icons.Filled.Shield),
        Triple("watch",    "BIO",       Icons.Filled.FavoriteBorder),
        Triple("space",    "SPACE",     Icons.Filled.Star),
        Triple("weather",  "WEATHER",   Icons.Filled.Cloud),
        Triple("symptoms", "SYMPTOMS",  Icons.Filled.List),
        Triple("chat",     "CHAT",      Icons.Filled.Forum),
        Triple("report",   "REPORT",    Icons.Filled.Description)
    )
    Scaffold(
        containerColor = BgColor,
        bottomBar = {
            NavigationBar(containerColor = CardBg, tonalElevation = 0.dp) {
                val back by nav.currentBackStackEntryAsState()
                val cur = back?.destination?.route
                tabs.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        selected = cur == route,
                        onClick = { nav.navigate(route) { launchSingleTop = true } },
                        icon = { Icon(icon, null) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyanColor, selectedTextColor = CyanColor,
                            indicatorColor = CardBg, unselectedIconColor = DimColor, unselectedTextColor = DimColor
                        )
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "burden", modifier = Modifier.padding(pad)) {
            composable("burden")   { BurdenScreen(vm) }
            composable("watch")    { WatchScreen(vm) }
            composable("space")    { SpaceScreen(vm) }
            composable("weather")  { WeatherScreen(vm) }
            composable("symptoms") { SymptomsScreen(vm) }
            composable("chat")     { ChatScreen(vm) }
            composable("report")   { ReportScreen(vm) }
        }
    }
}
