package com.fenghuo1943.tvcontrol.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fenghuo1943.tvcontrol.MainViewModel
import com.fenghuo1943.tvcontrol.input.GestureEngine
import com.fenghuo1943.tvcontrol.input.MouseActions
import com.fenghuo1943.tvcontrol.protocol.BluetoothMouseActionsImpl
import com.fenghuo1943.tvcontrol.ui.common.StatusBarStyle
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun BluetoothMouseScreen(
    modifier: Modifier = Modifier,
    vm: MainViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showWhitelistDialog by remember { mutableStateOf(false) }
    
    // 初始化蓝牙 HID 服务
    val bleService = remember {
        com.fenghuo1943.tvcontrol.bluetooth.BleHidService(context).apply {
            startHidService(
                onStarted = {
                    // 启动成功
                },
                onError = { error ->
                    // 显示错误
                }
            )
        }
    }
    
    // 清理资源
    DisposableEffect(Unit) {
        onDispose {
            // 检查权限后再停止服务
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            
            if (hasPermission) {
                bleService.stopHidService()
            } else {
                android.util.Log.w("BluetoothMouse", "缺少权限，跳过停止 HID 服务")
            }
        }
    }
    
    val actions = remember(bleService) { 
        BluetoothMouseActionsImpl(bleService) 
    }
    
    StatusBarStyle(isLight = true)
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color(0xFFF0F0F0))
            .padding(12.dp)
    ) {
        // 状态指示
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (bleService.isConnected) Color(0xFF4CAF50) else Color.White
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = if (bleService.isConnected) "✓ 已连接" else "○ 等待连接",
                            fontSize = 16.sp,
                            color = if (bleService.isConnected) Color.White else Color.Black
                        )
                        
                        if (bleService.isConnected) {
                            val deviceAddress = bleService.getConnectedDeviceAddress()
                            val deviceName = bleService.getConnectedDeviceName()
                            if (deviceAddress != null) {
                                Text(
                                    text = "MAC: $deviceAddress",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            if (deviceName != null && deviceName.isNotEmpty()) {
                                Text(
                                    text = "设备: $deviceName",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        } else {
                            Text(
                                text = "请在设备蓝牙设置中搜索并配对",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showWhitelistDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (bleService.isConnected) Color.White.copy(alpha = 0.3f) else Color(0xFFEFEFEF)
                            )
                        ) {
                            Text(
                                "🔒 白名单",
                                fontSize = 12.sp,
                                color = if (bleService.isConnected) Color.White else Color.Black
                            )
                        }
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (bleService.isConnected) Color.White else Color(0xFF1C1C1E)
                            )
                        ) {
                            Text(
                                "返回",
                                color = if (bleService.isConnected) Color(0xFF4CAF50) else Color.White
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 触摸板区域
        BleMouseTouchArea(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            actions = actions
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 鼠标按键
        BleMouseButtons(actions = actions)
    }
    
    // 🔹 白名单对话框
    if (showWhitelistDialog) {
        WhitelistDialog(
            bleService = bleService,
            onDismiss = { showWhitelistDialog = false }
        )
    }
}

@Composable
fun BleMouseTouchArea(
    modifier: Modifier = Modifier,
    actions: MouseActions,
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val engine = remember {
        GestureEngine(actions, scope, haptic)
    }
    Box(
        modifier = modifier
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .background(Color.White)
            .pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent()
                        engine.onEvent(event.changes)
                    }
                }
            }
    )
}

@Composable
fun BleMouseButtons(modifier: Modifier = Modifier, actions: MouseActions) {
    var pressedButtons by remember { mutableStateOf(setOf<com.fenghuo1943.tvcontrol.input.MouseButton>()) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BleMouseButtonItem(
            text = "左键",
            pressed = com.fenghuo1943.tvcontrol.input.MouseButton.Left in pressedButtons,
            onDown = {
                pressedButtons = pressedButtons + com.fenghuo1943.tvcontrol.input.MouseButton.Left
                actions.down(com.fenghuo1943.tvcontrol.input.MouseButton.Left)
            },
            onUp = {
                pressedButtons = pressedButtons - com.fenghuo1943.tvcontrol.input.MouseButton.Left
                actions.up(com.fenghuo1943.tvcontrol.input.MouseButton.Left)
            }
        )
        BleMouseButtonItem(
            text = "中键",
            pressed = com.fenghuo1943.tvcontrol.input.MouseButton.Middle in pressedButtons,
            onDown = {
                pressedButtons = pressedButtons + com.fenghuo1943.tvcontrol.input.MouseButton.Middle
                actions.down(com.fenghuo1943.tvcontrol.input.MouseButton.Middle)
            },
            onUp = {
                pressedButtons = pressedButtons - com.fenghuo1943.tvcontrol.input.MouseButton.Middle
                actions.up(com.fenghuo1943.tvcontrol.input.MouseButton.Middle)
            }
        )
        BleMouseButtonItem(
            text = "右键",
            pressed = com.fenghuo1943.tvcontrol.input.MouseButton.Right in pressedButtons,
            onDown = {
                pressedButtons = pressedButtons + com.fenghuo1943.tvcontrol.input.MouseButton.Right
                actions.down(com.fenghuo1943.tvcontrol.input.MouseButton.Right)
            },
            onUp = {
                pressedButtons = pressedButtons - com.fenghuo1943.tvcontrol.input.MouseButton.Right
                actions.up(com.fenghuo1943.tvcontrol.input.MouseButton.Right)
            }
        )
    }
}

@Composable
fun BleMouseButtonItem(
    text: String,
    pressed: Boolean = false,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp, 50.dp)
            .background(
                if (pressed) Color.DarkGray else Color.Gray,
                RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    onDown()
                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.find { it.id == pointerId } ?: continue
                        if (!change.pressed) {
                            change.consume()
                            onUp()
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White)
    }
}

/**
 * 白名单设置对话框
 */
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun WhitelistDialog(
    bleService: com.fenghuo1943.tvcontrol.bluetooth.BleHidService,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var scannedDevices by remember { mutableStateOf<List<com.fenghuo1943.tvcontrol.bluetooth.ScannedDevice>>(emptyList()) }
    var currentWhitelist by remember { mutableStateOf(bleService.getAllowedMacAddresses()) }
    
    val scanner = remember { com.fenghuo1943.tvcontrol.bluetooth.BluetoothScanner(context) }
    
    AlertDialog(
        onDismissRequest = {
            // 不再需要 stopScan，直接关闭
            onDismiss()
        },
        title = { 
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔒 MAC 地址白名单")
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // 说明文字
                Text(
                    "只允许白名单中的设备连接，其他设备将被自动拒绝",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 当前白名单
                Text("当前白名单 (${currentWhitelist.size} 个设备):", fontWeight = FontWeight.Bold)
                if (currentWhitelist.isEmpty()) {
                    Text("  （空 - 允许所有设备连接）", fontSize = 12.sp, color = Color.Gray)
                } else {
                    currentWhitelist.forEach { mac ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("  • $mac", fontSize = 12.sp)
                            IconButton(onClick = {
                                bleService.removeAllowedMacAddress(mac)
                                currentWhitelist = bleService.getAllowedMacAddresses()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "移除", tint = Color.Red)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 检查权限
                val hasConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                } else {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                }
                
                if (!hasConnectPermission) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "⚠️ 缺少蓝牙权限",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "请在设置中授予蓝牙权限，或重启应用以重新请求权限",
                                fontSize = 12.sp,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // 加载设备按钮
                Button(
                    onClick = {
                        if (!hasConnectPermission) {
                            android.widget.Toast.makeText(
                                context,
                                "缺少蓝牙权限，请重启应用以重新请求",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }
                        
                        isLoading = true
                        scope.launch {
                            // 模拟加载延迟，提供更好的用户体验
                            kotlinx.coroutines.delay(300)
                            scannedDevices = scanner.getBondedDevices()
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasConnectPermission && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isLoading) "加载中..." else "📋 加载已配对设备")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 扫描结果列表
                if (scannedDevices.isNotEmpty()) {
                    Text("发现的设备 (${scannedDevices.size}):", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    scannedDevices.forEach { device ->
                        val isInWhitelist = device.address.uppercase().replace(":", "") in 
                            currentWhitelist.map { it.uppercase().replace(":", "") }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isInWhitelist) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        device.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "${device.address} | 信号: ${device.getSignalStrength()}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                if (!isInWhitelist) {
                                    Button(
                                        onClick = {
                                            bleService.addAllowedMacAddress(device.address)
                                            currentWhitelist = bleService.getAllowedMacAddresses()
                                        },
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("添加", fontSize = 11.sp)
                                    }
                                } else {
                                    Text("✓ 已在白名单", fontSize = 11.sp, color = Color.Green)
                                }
                            }
                        }
                    }
                } else if (!isLoading && scannedDevices.isEmpty()) {
                    // 加载结束但没有设备
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "💡 未找到已配对设备",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "请确保：\n" +
                                "1. 目标设备已与手机配对\n" +
                                "2. 在系统蓝牙设置中完成配对\n" +
                                "3. 配对成功后再点击加载按钮",
                                fontSize = 11.sp,
                                color = Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Log.e("WhitelistDialog", "打开蓝牙设置失败", e)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("前往蓝牙设置")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 使用当前连接的设备
                val connectedMac = bleService.getConnectedDeviceAddress()
                if (connectedMac != null) {
                    Button(
                        onClick = {
                            bleService.addAllowedMacAddress(connectedMac)
                            currentWhitelist = bleService.getAllowedMacAddresses()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Text("➕ 使用当前连接的设备 ($connectedMac)")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 清除白名单按钮
                if (currentWhitelist.isNotEmpty()) {
                    Button(
                        onClick = {
                            bleService.clearAllowedMacAddresses()
                            currentWhitelist = bleService.getAllowedMacAddresses()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5722)
                        )
                    ) {
                        Text("🗑 清除白名单（允许所有设备）")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                // 不再需要 stopScan，直接关闭
                onDismiss()
            }) {
                Text("关闭")
            }
        }
    )
}
