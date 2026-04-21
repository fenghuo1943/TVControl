package com.fenghuo1943.tvcontrol.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

/**
 * 蓝牙设备管理器
 * 用于获取已配对的蓝牙设备列表
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class BluetoothScanner(private val context: Context) {
    
    companion object {
        private const val TAG = "BluetoothScanner"
    }
    
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    
    /**
     * 检查是否有必要的蓝牙权限
     */
    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ 需要新的蓝牙权限
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 11 及以下只需要旧权限
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 获取已配对的蓝牙设备列表
     */
    fun getBondedDevices(): List<ScannedDevice> {
        if (!hasBluetoothPermissions()) {
            Log.w(TAG, "缺少蓝牙权限，无法获取设备列表")
            return emptyList()
        }
        
        return try {
            val bondedDevices = bluetoothAdapter?.bondedDevices ?: emptySet()
            val devices = bondedDevices.mapNotNull { device ->
                try {
                    ScannedDevice(
                        name = device.name ?: "未知设备",
                        address = device.address,
                        rssi = -50, // 已配对设备没有 RSSI，使用默认值
                        isBonded = true
                    )
                } catch (e: SecurityException) {
                    Log.e(TAG, "访问设备信息失败", e)
                    null
                }
            }
            
            Log.d(TAG, "获取到 ${devices.size} 个已配对设备")
            devices
        } catch (e: SecurityException) {
            Log.e(TAG, "获取已配对设备失败：权限不足", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "获取已配对设备失败", e)
            emptyList()
        }
    }
}

/**
 * 扫描到的蓝牙设备
 */
data class ScannedDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isBonded: Boolean = false // 是否已配对
) {
    /**
     * 格式化显示名称
     */
    fun getDisplayName(): String {
        val bondStatus = if (isBonded) " [已配对]" else ""
        return if (name.isNotEmpty() && name != "未知设备") {
            "$name$bondStatus ($address)"
        } else {
            "未知设备$bondStatus ($address)"
        }
    }
    
    /**
     * 信号强度描述
     */
    fun getSignalStrength(): String {
        return if (isBonded) {
            "已配对" // 已配对设备不显示信号强度
        } else {
            when {
                rssi > -50 -> "极强"
                rssi > -60 -> "强"
                rssi > -70 -> "中等"
                rssi > -80 -> "弱"
                else -> "极弱"
            }
        }
    }
}
