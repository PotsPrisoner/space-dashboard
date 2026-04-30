package com.biospace.monitor.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.biospace.monitor.data.Biometrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

@SuppressLint("MissingPermission")
class WatchRepository(private val context: Context) {
    companion object {
        const val TARGET_MAC = "C0:29:AB:60:4D:10"
        val SERVICE_UUID = UUID.fromString("00000001-0000-1001-8001-00805F9B07D0")
        val WRITE_UUID   = UUID.fromString("00000002-0000-1001-8001-00805f9b07d0")
        val NOTIFY_UUID  = UUID.fromString("00000003-0000-1001-8001-00805F9B07D0")
        val CCCD_UUID    = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val _bio = MutableStateFlow(Biometrics())
    val bio: StateFlow<Biometrics> = _bio
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    private var gatt: BluetoothGatt? = null

    private val cb = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) { _connected.value = true; g.discoverServices() }
            else { _connected.value = false; _bio.value = _bio.value.copy(isWatchConnected = false) }
        }
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val svc = g.getService(SERVICE_UUID) ?: return
            val notify = svc.getCharacteristic(NOTIFY_UUID) ?: return
            g.setCharacteristicNotification(notify, true)
            val desc = notify.getDescriptor(CCCD_UUID)
            desc?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            desc?.let { g.writeDescriptor(it) }
        }
        override fun onCharacteristicChanged(g: BluetoothGatt, char: BluetoothGattCharacteristic) {
            parsePacket(char.value)
        }
    }

    private fun parsePacket(b: ByteArray) {
        if (b.size < 7 || b[0] != 0xAB.toByte()) return
        val cur = _bio.value
        val updated = when {
            b[4] == 0x31.toByte() && b[5] == 0x22.toByte() && b.size >= 9 ->
                cur.copy(bpSys = b[6].toInt() and 0xFF, bpDia = b[7].toInt() and 0xFF,
                    bpSource = "WATCH", isWatchConnected = true)
            b[4] == 0x31.toByte() && b[5] == 0x0A.toByte() ->
                cur.copy(heartRate = b[6].toInt() and 0xFF, hrSource = "WATCH", isWatchConnected = true)
            b[4] == 0x31.toByte() && b[5] == 0x12.toByte() ->
                cur.copy(spO2 = b[6].toInt() and 0xFF, spO2Source = "WATCH", isWatchConnected = true)
            b[4] == 0x32.toByte() && b.size >= 11 ->
                cur.copy(heartRate = b[6].toInt() and 0xFF, spO2 = b[7].toInt() and 0xFF,
                    bpSys = b[8].toInt() and 0xFF, bpDia = b[9].toInt() and 0xFF,
                    hrSource = "WATCH", spO2Source = "WATCH", bpSource = "WATCH", isWatchConnected = true)
            b[4] == 0x51.toByte() && b[5] == 0x08.toByte() && b.size >= 10 ->
                cur.copy(steps = ((b[6].toInt() and 0xFF) shl 16) or
                    ((b[7].toInt() and 0xFF) shl 8) or (b[8].toInt() and 0xFF))
            b[4] == 0x52.toByte() && b.size >= 15 ->
                cur.copy(sleepHours = ((b[12].toInt() and 0xFF) shl 8 or (b[13].toInt() and 0xFF)) / 60f)
            else -> null
        }
        updated?.let { _bio.value = it }
    }

    fun connect() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) return
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val dev = adapter.getRemoteDevice(TARGET_MAC)
        gatt = dev.connectGatt(context, false, cb, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() { gatt?.disconnect(); gatt?.close(); gatt = null; _connected.value = false }
    fun updateManual(b: Biometrics) { _bio.value = b }
}
