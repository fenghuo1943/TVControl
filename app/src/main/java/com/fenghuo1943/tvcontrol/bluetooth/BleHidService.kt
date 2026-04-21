package com.fenghuo1943.tvcontrol.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import java.util.*

@RequiresApi(Build.VERSION_CODES.P)
class BleHidService(private val context: Context) {
    
    companion object {
        private const val TAG = "BleHidService"
        
        // HID over GATT UUIDs
        private val HOGP_UUID = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb")
        private val REPORT_MAP_UUID = UUID.fromString("00002A4B-0000-1000-8000-00805f9b34fb")
        private val REPORT_UUID = UUID.fromString("00002A4D-0000-1000-8000-00805f9b34fb")
        private val PROTOCOL_MODE_UUID = UUID.fromString("00002A4E-0000-1000-8000-00805f9b34fb")
        private val HID_INFORMATION_UUID = UUID.fromString("00002A4A-0000-1000-8000-00805f9b34fb")
        private val HID_CONTROL_POINT_UUID = UUID.fromString("00002A4C-0000-1000-8000-00805f9b34fb")
        
        private val DEVICE_INFO_UUID = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb")
        private val PNP_ID_UUID = UUID.fromString("00002A50-0000-1000-8000-00805f9b34fb")
        
        const val DEFAULT_DEVICE_NAME = "TVControl Mouse"
    }
    
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var gattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    
    private var connectedDevice: BluetoothDevice? = null
    private var reportCharacteristic: BluetoothGattCharacteristic? = null
    
    private var isAdvertising = false
    var isConnected = false
        private set
    
    private var deviceName: String = DEFAULT_DEVICE_NAME
    
    // 🔹 MAC 地址白名单（为空则允许所有设备）
    private var allowedMacAddresses: MutableSet<String> = mutableSetOf()
    
    // SharedPreferences 用于持久化存储白名单
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("ble_whitelist", Context.MODE_PRIVATE)
    private val whitelistKey = "mac_whitelist"
    
    /**
     * 检查是否有 BLUETOOTH_CONNECT 权限
     */
    private fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 11 及以下不需要运行时权限
        }
    }
    
    // HID 报告描述符（标准鼠标）
    private val hidReportMap = byteArrayOf(
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x02.toByte(), // Usage (Mouse)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x01.toByte(), //   Report ID (1) - Mouse
        
        // 按钮 (3 bits)
        0x05.toByte(), 0x09.toByte(), //   Usage Page (Button)
        0x19.toByte(), 0x01.toByte(), //   Usage Minimum (Button 1)
        0x29.toByte(), 0x03.toByte(), //   Usage Maximum (Button 3)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), //   Logical Maximum (1)
        0x95.toByte(), 0x03.toByte(), //   Report Count (3)
        0x75.toByte(), 0x01.toByte(), //   Report Size (1)
        0x81.toByte(), 0x02.toByte(), //   Input (Data, Variable, Absolute)
        
        // 填充 (5 bits)
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x75.toByte(), 0x05.toByte(), //   Report Size (5)
        0x81.toByte(), 0x01.toByte(), //   Input (Constant)
        
        // X/Y 轴 (各 16 bits, signed)
        0x05.toByte(), 0x01.toByte(), //   Usage Page (Generic Desktop)
        0x09.toByte(), 0x30.toByte(), //   Usage (X)
        0x09.toByte(), 0x31.toByte(), //   Usage (Y)
        0x16.toByte(), 0x00.toByte(), 0x80.toByte(), //   Logical Minimum (-32768)
        0x26.toByte(), 0xFF.toByte(), 0x7F.toByte(), //   Logical Maximum (32767)
        0x75.toByte(), 0x10.toByte(), //   Report Size (16)
        0x95.toByte(), 0x02.toByte(), //   Report Count (2)
        0x81.toByte(), 0x06.toByte(), //   Input (Data, Variable, Relative)
        
        // 滚轮 (8 bits, signed)
        0x09.toByte(), 0x38.toByte(), //   Usage (Wheel)
        0x15.toByte(), 0x81.toByte(), //   Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(), //   Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x81.toByte(), 0x06.toByte(), //   Input (Data, Variable, Relative)
        
        0xC0.toByte()                  // End Collection
    )
    
    init {
        initializeBluetooth()
        loadWhitelistFromPreferences() // 从 SharedPreferences 加载白名单
    }
    
    private fun initializeBluetooth() {
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e(TAG, "蓝牙未启用")
        }
    }
    
    /**
     * 设置设备名称
     */
    fun setDeviceName(name: String) {
        deviceName = name
        Log.d(TAG, "设备名称设置为: $name")
    }
    
    /**
     * 获取设备名称
     */
    fun getDeviceName(): String {
        return deviceName
    }
    
    /**
     * 获取已连接设备的 MAC 地址
     */
//    fun getConnectedDeviceAddress(): String? {
//        return connectedDevice?.address
//    }
    
    /**
     * 获取已连接设备的名称
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun getConnectedDeviceName(): String? {
        if (!hasBluetoothConnectPermission()) {
            Log.w(TAG, "缺少 BLUETOOTH_CONNECT 权限")
            return null
        }
        return try {
            connectedDevice?.name
        } catch (e: SecurityException) {
            Log.e(TAG, "获取设备名称失败", e)
            null
        }
    }
    
    /**
     * 获取已连接设备的 MAC 地址
     */
    fun getConnectedDeviceAddress(): String? {
        if (!hasBluetoothConnectPermission()) {
            Log.w(TAG, "缺少 BLUETOOTH_CONNECT 权限")
            return null
        }
        return try {
            connectedDevice?.address
        } catch (e: SecurityException) {
            Log.e(TAG, "获取设备地址失败", e)
            null
        }
    }
    
    /**
     * 设置允许连接的 MAC 地址白名单
     */
    fun setAllowedMacAddresses(macAddresses: Set<String>) {
        allowedMacAddresses = macAddresses.map { it.uppercase().replace(":", "") }.toMutableSet()
        saveWhitelistToPreferences() // 保存到 SharedPreferences
        Log.d(TAG, "设置 MAC 地址白名单: ${allowedMacAddresses.size} 个设备")
    }
    
    /**
     * 添加单个 MAC 地址到白名单
     */
    fun addAllowedMacAddress(macAddress: String) {
        val normalized = macAddress.uppercase().replace(":", "")
        allowedMacAddresses.add(normalized)
        saveWhitelistToPreferences() // 保存到 SharedPreferences
        Log.d(TAG, "添加 MAC 地址到白名单: $normalized")
    }
    
    /**
     * 从白名单中移除 MAC 地址
     */
    fun removeAllowedMacAddress(macAddress: String) {
        val normalized = macAddress.uppercase().replace(":", "")
        allowedMacAddresses.remove(normalized)
        saveWhitelistToPreferences() // 保存到 SharedPreferences
        Log.d(TAG, "从白名单移除 MAC 地址: $normalized")
    }
    
    /**
     * 清除 MAC 地址白名单（允许所有设备连接）
     */
    fun clearAllowedMacAddresses() {
        allowedMacAddresses.clear()
        saveWhitelistToPreferences() // 保存到 SharedPreferences
        Log.d(TAG, "已清除 MAC 地址白名单")
    }
    
    /**
     * 获取当前白名单
     */
    fun getAllowedMacAddresses(): Set<String> {
        return allowedMacAddresses.toSet()
    }
    
    /**
     * 检查 MAC 地址是否在白名单中
     */
    fun isMacAllowed(macAddress: String): Boolean {
        if (allowedMacAddresses.isEmpty()) return true
        val normalized = macAddress.uppercase().replace(":", "")
        return normalized in allowedMacAddresses
    }
    
    /**
     * 从 SharedPreferences 加载白名单
     */
    private fun loadWhitelistFromPreferences() {
        try {
            val whitelistString = sharedPreferences.getString(whitelistKey, "")
            if (!whitelistString.isNullOrEmpty()) {
                val addresses = whitelistString.split(",").filter { it.isNotEmpty() }.toMutableSet()
                allowedMacAddresses = addresses
                Log.d(TAG, "从 SharedPreferences 加载白名单: ${allowedMacAddresses.size} 个设备")
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载白名单失败", e)
        }
    }
    
    /**
     * 将白名单保存到 SharedPreferences
     */
    private fun saveWhitelistToPreferences() {
        try {
            val whitelistString = allowedMacAddresses.joinToString(",")
            sharedPreferences.edit().putString(whitelistKey, whitelistString).apply()
            Log.d(TAG, "白名单已保存到 SharedPreferences: ${allowedMacAddresses.size} 个设备")
        } catch (e: Exception) {
            Log.e(TAG, "保存白名单失败", e)
        }
    }
    
    /**
     * 启动 HID 广播和服务
     */
    fun startHidService(onStarted: () -> Unit = {}, onError: (String) -> Unit = {}) {
        if (!checkBluetoothEnabled()) {
            onError("蓝牙未启用")
            return
        }
        
        try {
            setupGattServer()
            startAdvertising(onStarted, onError)
        } catch (e: Exception) {
            Log.e(TAG, "启动 HID 服务失败", e)
            onError("启动失败: ${e.message}")
        }
    }
    
    /**
     * 停止 HID 服务
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun stopHidService() {
        try {
            stopAdvertising()
            gattServer?.close()
            gattServer = null
            connectedDevice = null
            isConnected = false
            Log.d(TAG, "HID 服务已停止")
        } catch (e: SecurityException) {
            Log.e(TAG, "停止 HID 服务失败：权限不足", e)
        }
    }
    
    /**
     * 发送鼠标移动事件
     */
    fun sendMouseMove(dx: Int, dy: Int, wheel: Int = 0) {
        if (!isConnected || reportCharacteristic == null) {
            Log.w(TAG, "未连接或特征值为空")
            return
        }
        
        val buttons = 0 // 无按键
        
        // HID 报告格式: [Report ID][Buttons][X Low][X High][Y Low][Y High][Wheel]
        val report = byteArrayOf(
            0x01.toByte(), // Report ID
            buttons.toByte(),
            (dx and 0xFF).toByte(),
            ((dx shr 8) and 0xFF).toByte(),
            (dy and 0xFF).toByte(),
            ((dy shr 8) and 0xFF).toByte(),
            wheel.toByte()
        )
        
        sendReport(report)
    }
    
    /**
     * 发送鼠标按键事件
     */
    fun sendMouseButton(left: Boolean = false, right: Boolean = false, middle: Boolean = false) {
        if (!isConnected || reportCharacteristic == null) return
        
        var buttons = 0
        if (left) buttons = buttons or 0x01
        if (right) buttons = buttons or 0x02
        if (middle) buttons = buttons or 0x04
        
        val report = byteArrayOf(
            0x01.toByte(),
            buttons.toByte(),
            0x00.toByte(), 0x00.toByte(), // X
            0x00.toByte(), 0x00.toByte(), // Y
            0x00.toByte()                 // Wheel
        )
        
        sendReport(report)
    }
    
    /**
     * 检查是否已连接
     */
    fun isDeviceConnected(): Boolean = isConnected
    
    // ==================== 私有方法 ====================
    
    private fun checkBluetoothEnabled(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter!!.isEnabled
    }
    
    @SuppressLint("MissingPermission")
    private fun setupGattServer() {
        gattServer = bluetoothManager?.openGattServer(context, gattServerCallback)
        
        // 添加 HID 服务
        val hidService = BluetoothGattService(HOGP_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        
        // HID Information
        val hidInfoChar = BluetoothGattCharacteristic(
            HID_INFORMATION_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        // bcdHID=1.1, bCountryCode=0, Flags=RemoteWake|NormallyConnectable
        hidInfoChar.value = byteArrayOf(0x11.toByte(), 0x01.toByte(), 0x00.toByte(), 0x03.toByte())
        hidService.addCharacteristic(hidInfoChar)
        
        // Report Map
        val reportMapChar = BluetoothGattCharacteristic(
            REPORT_MAP_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        reportMapChar.value = hidReportMap
        hidService.addCharacteristic(reportMapChar)
        
        // Protocol Mode
        val protocolModeChar = BluetoothGattCharacteristic(
            PROTOCOL_MODE_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        protocolModeChar.value = byteArrayOf(0x01.toByte()) // Report Protocol
        hidService.addCharacteristic(protocolModeChar)
        
        // Report (Input)
        reportCharacteristic = BluetoothGattCharacteristic(
            REPORT_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or 
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or
            BluetoothGattCharacteristic.PROPERTY_INDICATE,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        // Client Characteristic Configuration Descriptor (CCCD)
        val cccd = BluetoothGattDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        reportCharacteristic!!.addDescriptor(cccd)
        hidService.addCharacteristic(reportCharacteristic!!)
        
        // HID Control Point
        val controlPointChar = BluetoothGattCharacteristic(
            HID_CONTROL_POINT_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        hidService.addCharacteristic(controlPointChar)
        
        gattServer?.addService(hidService)
        
        // 添加设备信息服务
        val deviceInfoService = BluetoothGattService(DEVICE_INFO_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val pnpIdChar = BluetoothGattCharacteristic(
            PNP_ID_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        // VID Source=1(USB), VID=0x0000, PID=0x0001, Version=1.0
        pnpIdChar.value = byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte())
        deviceInfoService.addCharacteristic(pnpIdChar)
        gattServer?.addService(deviceInfoService)
        
        Log.d(TAG, "GATT 服务器设置完成")
    }
    
    @SuppressLint("MissingPermission")
    private fun startAdvertising(onStarted: () -> Unit, onError: (String) -> Unit) {
        if (advertiser == null) {
            onError("设备不支持 BLE 广播")
            return
        }
        
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()
        
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(HOGP_UUID))
            .build()
        
        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .build()
        
        advertiser?.startAdvertising(settings, data, scanResponse, object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                isAdvertising = true
                Log.d(TAG, "BLE 广播启动成功，设备名称: $deviceName")
                onStarted()
            }
            
            override fun onStartFailure(errorCode: Int) {
                Log.e(TAG, "BLE 广播启动失败: $errorCode")
                onError("广播启动失败: $errorCode")
            }
        })
    }
    
    @SuppressLint("MissingPermission")
    private fun stopAdvertising() {
        if (isAdvertising && advertiser != null) {
            advertiser?.stopAdvertising(object : AdvertiseCallback() {})
            isAdvertising = false
            Log.d(TAG, "BLE 广播已停止")
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun sendReport(report: ByteArray) {
        if (connectedDevice == null || reportCharacteristic == null) return
        
        reportCharacteristic?.value = report
        gattServer?.notifyCharacteristicChanged(
            connectedDevice,
            reportCharacteristic,
            false
        )
    }
    
    // ==================== GATT 服务器回调 ====================
    
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            
            val deviceAddress = device?.address ?: "未知"
            val deviceName = device?.name ?: "未知设备"
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    // 🔹 检查 MAC 地址白名单
                    if (!isMacAllowed(deviceAddress)) {
                        Log.w(TAG, "⚠️ 拒绝未授权设备连接")
                        Log.w(TAG, "   - 名称: $deviceName")
                        Log.w(TAG, "   - MAC: $deviceAddress")
                        // 拒绝连接
                        gattServer?.cancelConnection(device)
                        return
                    }
                    
                    connectedDevice = device
                    isConnected = true
                    Log.d(TAG, "✅ 设备已连接（白名单验证通过）")
                    Log.d(TAG, "   - 名称: $deviceName")
                    Log.d(TAG, "   - MAC: $deviceAddress")
                    Log.d(TAG, "   - 状态码: $status")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    val previousDevice = connectedDevice
                    connectedDevice = null
                    isConnected = false
                    Log.d(TAG, "❌ 设备已断开")
                    if (previousDevice != null) {
                        Log.d(TAG, "   - 名称: ${previousDevice.name ?: "未知"}")
                        Log.d(TAG, "   - MAC: ${previousDevice.address}")
                    }
                }
            }
        }
        
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            
            if (!hasBluetoothConnectPermission()) {
                Log.w(TAG, "缺少权限，拒绝特征值读取请求")
                return
            }
            
            try {
                if (characteristic != null) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.value)
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "发送响应失败", e)
            }
        }
        
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onDescriptorReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor?
        ) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor)
            
            if (!hasBluetoothConnectPermission()) {
                Log.w(TAG, "缺少权限，拒绝描述符读取请求")
                return
            }
            
            try {
                if (descriptor != null) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.value)
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "发送响应失败", e)
            }
        }
        
        @SuppressLint("MissingPermission")
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value)
            
            if (descriptor != null && value != null) {
                descriptor.value = value
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                }
                Log.d(TAG, "描述符写入: ${value.contentToString()}")
            }
        }
    }
}
