package com.example.turing1

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceListView: ListView
    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private val deviceList = mutableListOf<String>()
    private val deviceMap = mutableMapOf<String, BluetoothDevice>()
    private var currentDevice: BluetoothDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the ListView
        deviceListView = ListView(this)
        setContentView(deviceListView)

        // Initialize Bluetooth Adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Check for Bluetooth permissions on Android 12 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkBluetoothPermissions()
        } else {
            displayPairedDevices()
        }
    }

    @SuppressLint("MissingPermission")
    private fun displayPairedDevices() {
        // Clear the list and map
        deviceList.clear()
        deviceMap.clear()

        // Get currently connected device (if any)
        currentDevice = bluetoothAdapter.bondedDevices.firstOrNull { it.isConnected() }

        // Add header for currently connected device
        deviceList.add("Currently Connected Device:")

        // Add the currently connected device
        currentDevice?.let { device ->
            val deviceLabel = "${device.name} (${device.address}) - Currently Connected"
            deviceList.add(deviceLabel)
            deviceMap[device.name] = device
        }

        // Add header for previous devices
        deviceList.add("\nPrevious Devices:")

        // Get paired devices
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        pairedDevices?.forEach { device ->
            if (device != currentDevice) {
                val deviceLabel = "${device.name} (${device.address})"
                deviceList.add(deviceLabel)
                deviceMap[device.name] = device
            }
        }

        // Set up the ListView adapter
        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList)
        deviceListView.adapter = deviceListAdapter

        // Set item click listener
        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val deviceName = deviceList[position].substringBefore(" (")
            val device = deviceMap[deviceName]

            // Start the details activity
            device?.let {
                val intent = Intent(this, DeviceDetailActivity::class.java).apply {
                    putExtra("device_name", it.name)
                    putExtra("device_address", it.address)
                    putExtra("is_currently_connected", it == currentDevice)
                }
                startActivity(intent)
            }
        }
    }

    private fun checkBluetoothPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )

        val permissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), 1)
        } else {
            displayPairedDevices()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            displayPairedDevices()
        } else {
            // Handle the case where permissions were not granted
        }
    }

    // Extension function to check if a Bluetooth device is currently connected
    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.isConnected(): Boolean {
        return try {
            val method = this.javaClass.getMethod("isConnected")
            method.invoke(this) as Boolean
        } catch (e: Exception) {
            false
        }
    }
}
