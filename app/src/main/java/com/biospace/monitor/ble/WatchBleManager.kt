package com.biospace.monitor.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.biospace.monitor.ble.WatchProtocol.WatchReading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "WatchBleManager"
private const val SCAN_TIMEOUT_MS = 15_000L

@SuppressLint("MissingPermission")
class WatchBleManager(private val context: Context) {

    // ─── Public state ────────────────────────────────────────────────────────
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _readings = MutableStateFlow<WatchReading?>(null)
    val readings: StateFlow<WatchReading?> = _readings.asStateFlow()

    private val _batteryLevel = MutableStateFlow(-1)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _lastKnownDevice = MutableStateFlow<BluetoothDevice?>(null)
    val lastKnownDevice: StateFlow<BluetoothDevice?> = _lastKnownDevice.asStateFlow()

    enum class ConnectionState { DISCONNECTED, SCANNING, CONNECTING, CONNECTED }

    // ─── Internal ────────────────────────────────────────────────────────────
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager.adapter
    private var bluetoothGatt: BluetoothGatt? = null
    private var scanner: BluetoothLeScanner? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var scanCallback: ScanCallback? = null

    // ─── Scan ────────────────────────────────────────────────────────────────
    fun startScan() {
        if (_connectionState.value != ConnectionState.DISCONNECTED) return
        val adapter = bluetoothAdapter ?: run {
            Log.e(TAG, "Bluetooth not available")
            return
        }
        if (!adapter.isEnabled) {
            Log.e(TAG, "Bluetooth disabled")
            return
        }

        _connectionState.value = ConnectionState.SCANNING
        scanner = adapter.bluetoothLeScanner

        val filter = ScanFilter.Builder()
            .setServiceUuid(android.os.ParcelUuid(WatchProtocol.NUS_SERVICE))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                Log.i(TAG, "Found watch: ${result.device.address} rssi=${result.rssi}")
                stopScan()
                connect(result.device)
            }
            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed code=$errorCode")
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
        scanCallback = cb
        scanner?.startScan(listOf(filter), settings, cb)

        // Stop scan after timeout
        mainHandler.postDelayed({ stopScan() }, SCAN_TIMEOUT_MS)
        Log.i(TAG, "BLE scan started")
    }

    fun stopScan() {
        scanCallback?.let { scanner?.stopScan(it) }
        scanCallback = null
        if (_connectionState.value == ConnectionState.SCANNING) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    // ─── Connect to known MAC ─────────────────────────────────────────────
    fun connectTo(macAddress: String) {
        val adapter = bluetoothAdapter ?: return
        val device = try { adapter.getRemoteDevice(macAddress) } catch (e: Exception) {
            Log.e(TAG, "Bad MAC $macAddress: $e"); return
        }
        connect(device)
    }

    private fun connect(device: BluetoothDevice) {
        _connectionState.value = ConnectionState.CONNECTING
        _lastKnownDevice.value = device
        Log.i(TAG, "Connecting to ${device.address}")
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    fun close() {
        stopScan()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    // ─── GATT callback ───────────────────────────────────────────────────────
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "GATT connected, discovering services")
                    mainHandler.post { _connectionState.value = ConnectionState.CONNECTED }
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "GATT disconnected status=$status")
                    mainHandler.post { _connectionState.value = ConnectionState.DISCONNECTED }
                    gatt.close()
                    bluetoothGatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Service discovery failed status=$status")
                return
            }
            logServices(gatt)
            enableNusNotifications(gatt)
                    enableAutoMeasure()
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val raw = characteristic.value ?: return
            handleIncomingBytes(raw)
        }

        // API 33+
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleIncomingBytes(value)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "CCCD written — NUS notifications enabled")
            } else {
                Log.e(TAG, "CCCD write failed status=$status")
            }
        }
    }

    // ─── Enable NUS RX notifications ─────────────────────────────────────────
    private fun enableNusNotifications(gatt: BluetoothGatt) {
        val service = gatt.getService(WatchProtocol.NUS_SERVICE) ?: run {
            Log.e(TAG, "NUS service not found")
            return
        }
        val rxChar = service.getCharacteristic(WatchProtocol.NUS_RX_NOTIFY) ?: run {
            Log.e(TAG, "NUS RX characteristic not found")
            return
        }
        gatt.setCharacteristicNotification(rxChar, true)
        val cccd = rxChar.getDescriptor(WatchProtocol.CCCD) ?: run {
            Log.e(TAG, "CCCD descriptor not found")
            return
        }
        cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        val ok = gatt.writeDescriptor(cccd)
        Log.i(TAG, "Write CCCD result=$ok")
    }

    // ─── Send command to watch (NUS TX) ──────────────────────────────────────
    private fun enableAutoMeasure() {
        // Enable hourly auto-measurement so watch pushes live readings
        sendCommand(byteArrayOf(0xAB.toByte(), 0x00, 0x04, 0xFF.toByte(), 0x78, 0x80.toByte(), 0x01))
    }

    fun sendCommand(bytes: ByteArray): Boolean {
        val gatt = bluetoothGatt ?: return false
        val service = gatt.getService(WatchProtocol.NUS_SERVICE) ?: return false
        val txChar = service.getCharacteristic(WatchProtocol.NUS_TX_WRITE) ?: return false
        txChar.value = bytes
        txChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        return gatt.writeCharacteristic(txChar)
    }

    // ─── Incoming byte handler ────────────────────────────────────────────────
    private fun handleIncomingBytes(raw: ByteArray) {
        // Always log raw hex to Logcat so we can map unknown command bytes
        Log.d("WatchPacket", raw.joinToString(" ") { "%02X".format(it) })

        val reading = WatchProtocol.parse(raw)

        // Log unknown packets prominently so we can identify missing cmd bytes
        if (reading is WatchReading.Unknown) {
            Log.w(TAG, "Unknown cmd=0x${reading.cmdByte.toString(16).uppercase()} " +
                    "raw=${raw.joinToString(" ") { "%02X".format(it) }}")
        } else {
            Log.i(TAG, "Reading: $reading")
        }

        // Update battery separately
        if (reading is WatchReading.Battery) {
            _batteryLevel.value = reading.percent
        }

        _readings.value = reading
    }

    // ─── Debug helpers ────────────────────────────────────────────────────────
    private fun logServices(gatt: BluetoothGatt) {
        gatt.services.forEach { svc ->
            Log.d(TAG, "Service: ${svc.uuid}")
            svc.characteristics.forEach { chr ->
                Log.d(TAG, "  Char: ${chr.uuid}  props=0x${chr.properties.toString(16)}")
            }
        }
    }
}
