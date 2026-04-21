package com.fenghuo1943.tvcontrol

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.fenghuo1943.tvcontrol.bluetooth.BleHidService
import com.fenghuo1943.tvcontrol.ui.BluetoothMouseScreen
import com.fenghuo1943.tvcontrol.ui.common.AppScaffold
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BluetoothMouseActivity : ComponentActivity() {
    
    private var originalBluetoothName: String? = null
    
    private val requestBluetoothPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "需要蓝牙权限才能使用", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private val enableBluetooth = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // 蓝牙已启用
        } else {
            Toast.makeText(this, "需要启用蓝牙", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 检查并请求权限
        if (!checkAndRequestPermissions()) {
            return
        }
        
        setContent {
            AppScaffold {
                BluetoothMouseScreen(
                    onBack = {
                        restoreBluetoothName()
                        finish() 
                    }
                )
            }
        }
    }
    
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onResume() {
        super.onResume()
        setBluetoothName()
    }
    
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onDestroy() {
        super.onDestroy()
        restoreBluetoothName()
    }
    
    /**
     * 设置蓝牙名称为 TVControl Mouse
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun setBluetoothName() {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                originalBluetoothName = bluetoothAdapter.name
                val newName = BleHidService.DEFAULT_DEVICE_NAME
                bluetoothAdapter.setName(newName)
                Log.d("BluetoothMouse", "蓝牙名称已设置为: $newName")
            }
        } catch (e: Exception) {
            Log.e("BluetoothMouse", "设置蓝牙名称失败", e)
        }
    }
    
    /**
     * 恢复原始蓝牙名称
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun restoreBluetoothName() {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled && originalBluetoothName != null) {
                bluetoothAdapter.setName(originalBluetoothName)
                Log.d("BluetoothMouse", "蓝牙名称已恢复为: $originalBluetoothName")
            }
        } catch (e: Exception) {
            Log.e("BluetoothMouse", "恢复蓝牙名称失败", e)
        }
    }
    
    private fun checkAndRequestPermissions(): Boolean {
        // 检查蓝牙是否启用
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetooth.launch(enableBtIntent)
            return false
        }
        
        // Android 12+ 需要运行时权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
            
            val notGranted = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            
            if (notGranted.isNotEmpty()) {
                Log.d("BluetoothMouse", "请求蓝牙权限: ${notGranted.joinToString()}")
                requestBluetoothPermission.launch(notGranted.toTypedArray())
                return false
            }
        } else {
            // Android 11 及以下需要位置权限来扫描 BLE
            val permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            
            val notGranted = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            
            if (notGranted.isNotEmpty()) {
                Log.d("BluetoothMouse", "请求位置权限: ${notGranted.joinToString()}")
                requestBluetoothPermission.launch(notGranted.toTypedArray())
                return false
            }
        }
        
        return true
    }
}
