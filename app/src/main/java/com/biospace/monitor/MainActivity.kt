package com.biospace.monitor

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biospace.monitor.ui.MainViewModel
import com.biospace.monitor.ui.screens.AppNavigation
import com.biospace.monitor.ui.theme.BioSpaceTheme

class MainActivity : ComponentActivity() {
    private val perms = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.POST_NOTIFICATIONS
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}.launch(perms)
        setContent {
            BioSpaceTheme {
                val vm: MainViewModel = viewModel()
                val useGps by vm.useGps.collectAsState()
                if (useGps) getGpsLocation(vm)
                AppNavigation(vm)
            }
        }
    }
    private fun getGpsLocation(vm: MainViewModel) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        loc?.let { vm.setLocation(it.latitude, it.longitude) }
    }
}
