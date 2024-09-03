package com.example.turing1

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class DeviceDetailActivity : AppCompatActivity() {

    private var bluetoothGatt: BluetoothGatt? = null
    private val batteryLevelCharacteristicUUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb") // UUID for Battery Level

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize a TextView to display device details
        val detailsTextView = TextView(this)
        setContentView(detailsTextView)

        // Retrieve the device name, address, and connection status passed from MainActivity
        val deviceName = intent.getStringExtra("device_name")
        val deviceAddress = intent.getStringExtra("device_address")
        val isCurrentlyConnected = intent.getBooleanExtra("is_currently_connected", false)

        // Check for necessary permissions
        if (isCurrentlyConnected && checkBluetoothPermissions()) {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            connectToDevice(device)
            updateDeviceDetails(detailsTextView, deviceName, deviceAddress)
        } else {
            detailsTextView.text = "Bluetooth permissions not granted or device not connected."
        }
    }

    private fun checkBluetoothPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )



        val permissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (permissionsNeeded.isEmpty()) {
            true
        } else {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), 1)
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    bluetoothGatt?.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val batteryService = gatt?.getService(batteryLevelCharacteristicUUID)
                    val batteryLevelCharacteristic = batteryService?.getCharacteristic(batteryLevelCharacteristicUUID)
                    if (batteryLevelCharacteristic != null && checkBluetoothPermissions()) {
                        gatt.readCharacteristic(batteryLevelCharacteristic)
                    }
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (characteristic?.uuid == batteryLevelCharacteristicUUID) {
                        val batteryLevel = characteristic?.value?.get(0)?.toInt() ?: 0
                        runOnUiThread {
                            updateBatteryLevel(batteryLevel)
                        }
                    }
                }
            }
        })
    }

    private fun updateDeviceDetails(
        detailsTextView: TextView,
        deviceName: String?,
        deviceAddress: String?
    ) {
        // Use a handler to periodically update the UI
        val handler = Handler(Looper.getMainLooper())
        val updateTask = object : Runnable {
            @SuppressLint("MissingPermission")
            override fun run() {
                val batteryCharacteristic = bluetoothGatt?.getService(batteryLevelCharacteristicUUID)
                    ?.getCharacteristic(batteryLevelCharacteristicUUID)
                if (batteryCharacteristic != null && checkBluetoothPermissions()) {
                    bluetoothGatt?.readCharacteristic(batteryCharacteristic)
                }
                handler.postDelayed(this, 60000) // Update every minute
            }
        }
        handler.post(updateTask)
    }

    private fun updateBatteryLevel(batteryLevel: Int) {
        // Update UI with dynamic battery level
        val detailsTextView = findViewById<TextView>(android.R.id.content)
        val currentText = detailsTextView.text.toString()
        val updatedText = currentText.replace(Regex("Battery: \\d+%"), "Battery: $batteryLevel%")
        detailsTextView.text = updatedText
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            recreate() // Recreate the activity to apply permissions
        }
    }
}
