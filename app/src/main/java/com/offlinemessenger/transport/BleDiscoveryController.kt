package com.offlinemessenger.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import java.util.UUID

/** BLE is discovery only. Packet transfer must use an authenticated GATT or Wi-Fi Direct channel. */
class BleDiscoveryController(context: Context, private val onDeviceFound: (String) -> Unit) {
    private val scanner: BluetoothLeScanner? = BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner
    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // A rotating, app-defined node token belongs in service/manufacturer data; never trust device name.
            result.scanRecord?.serviceUuids?.takeIf { BLE_SERVICE_UUID in it }?.let { onDeviceFound(result.device.address) }
        }
    }
    @SuppressLint("MissingPermission") fun start() { scanner?.startScan(callback) }
    @SuppressLint("MissingPermission") fun stop() { scanner?.stopScan(callback) }
    companion object { val BLE_SERVICE_UUID: UUID = UUID.fromString("f9744b8b-113d-4a55-9002-0f2b23f967df") }
}
