package com.biospace.monitor.ble

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import kotlinx.coroutines.flow.*

enum class ConnectionState { DISCONNECTED, SCANNING, CONNECTING, CONNECTED }

class WatchBleManager(private val context: Context) {

    private val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE)
            as BluetoothManager).adapter

    private var gatt: BluetoothGatt? = null
    private var scanCallback: ScanCallback? = null

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _readings = MutableSharedFlow<WatchReading>(replay = 0, extraBufferCapacity = 128)
    val readings: SharedFlow<WatchReading> = _readings.asSharedFlow()

    private val _log = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64)
    val log: SharedFlow<String> = _log.asSharedFlow()

    // ── Public API ────────────────────────────────────────────────────────────

    fun scanAndConnect() {
        if (_state.value != ConnectionState.DISCONNECTED) return
        _state.value = ConnectionState.SCANNING
        log("Scanning for BP Doctor FIT…")

        val scanner = adapter?.bluetoothLeScanner ?: run {
            log("Bluetooth not available")
            _state.value = ConnectionState.DISCONNECTED
            return
        }

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(WatchProtocol.SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                scanner.stopScan(this)
                scanCallback = null
                log("Found: ${result.device.address}")
                connect(result.device.address)
            }
            override fun onScanFailed(errorCode: Int) {
                log("Scan failed: $errorCode")
                _state.value = ConnectionState.DISCONNECTED
            }
        }
        scanCallback = cb
        scanner.startScan(listOf(filter), settings, cb)
    }

    fun connect(macAddress: String) {
        disconnect()
        _state.value = ConnectionState.CONNECTING
        log("Connecting to $macAddress…")
        val device = adapter?.getRemoteDevice(macAddress) ?: return
        gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        else
            device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        scanCallback?.let {
            adapter?.bluetoothLeScanner?.stopScan(it)
            scanCallback = null
        }
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _state.value = ConnectionState.DISCONNECTED
    }

    // ── GATT Callbacks ────────────────────────────────────────────────────────

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                log("Connected — discovering services…")
                _state.value = ConnectionState.CONNECTING
                g.discoverServices()
            } else {
                log("Disconnected (status=$status)")
                _state.value = ConnectionState.DISCONNECTED
                gatt?.close(); gatt = null
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                log("Service discovery failed: $status")
                return
            }
            val service = g.getService(WatchProtocol.SERVICE_UUID)
            if (service == null) { log("NUS service not found"); return }

            val notifyChr = service.getCharacteristic(WatchProtocol.CHAR_NOTIFY_UUID)
            g.setCharacteristicNotification(notifyChr, true)

            val descriptor = notifyChr.getDescriptor(WatchProtocol.DESCRIPTOR_UUID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                g.writeDescriptor(descriptor)
            }

            _state.value = ConnectionState.CONNECTED
            log("Watch ready — receiving data")
        }

        // API < 33
        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(g: BluetoothGatt, chr: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            handleData(chr.value)
        }

        // API >= 33
        override fun onCharacteristicChanged(g: BluetoothGatt, chr: BluetoothGattCharacteristic, value: ByteArray) {
            handleData(value)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun handleData(raw: ByteArray) {
        WatchProtocol.parse(raw)?.let { _readings.tryEmit(it) }
    }

    private fun log(msg: String) { _log.tryEmit(msg) }
}
