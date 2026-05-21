package com.fenghuo1943.tvcontrol.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenghuo1943.tvcontrol.Device
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fenghuo1943.tvcontrol.ConnectionState
import com.fenghuo1943.tvcontrol.MainViewModel
import com.fenghuo1943.tvcontrol.MouseControlActivity
import com.fenghuo1943.tvcontrol.RemoteControlActivity
import com.fenghuo1943.tvcontrol.ui.common.ConnectionStatus
import com.fenghuo1943.tvcontrol.ui.common.StatusBarStyle

@Composable
fun MainScreen(
    onIpChange: (String) -> Unit,
    vm: MainViewModel = hiltViewModel()
    ) {
    var showScanDialog by remember { mutableStateOf(false) }
    val state by vm::connectionState
    StatusBarStyle(isLight = true)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .statusBarsPadding()   // ✅ 关键！
            .padding(12.dp)
    ) {
        // 🔹 连接卡片
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TextField(
                    value = vm.ip,
                    onValueChange = { vm.ip = it },
                    placeholder = { Text("输入IP，例如 192.168.1.100") },
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF1F3F5),   // 背景（选中）
                        unfocusedContainerColor = Color(0xFFF1F3F5), // 背景（未选中）
                        focusedIndicatorColor = Color.Transparent,   // 去掉下划线
                        unfocusedIndicatorColor = Color.Transparent,

                        cursorColor = Color.Black,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),

                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                ConnectionStatus(state)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp) // 按钮间距
                ) {
                    Button(
                        onClick = {
                            when (state) {
                                ConnectionState.DISCONNECTED, ConnectionState.ERROR -> vm.connectToServer(
                                    Device(
                                        ips = listOf(vm.ip),
                                        name = "PC"
                                    )
                                )
                                ConnectionState.CONNECTED -> vm.disconnect() // 👈 新增
                                else -> {} // 连接中/重连中不处理
                            }
                        },
                        enabled = state != ConnectionState.CONNECTING &&
                                state != ConnectionState.RECONNECTING,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (state) {
                                ConnectionState.CONNECTED -> Color(0xFFD32F2F) // 红色（断开）
                                else -> Color(0xFF1C1C1E) // 默认黑色
                            },
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            when (state) {
                                ConnectionState.DISCONNECTED, ConnectionState.ERROR -> "连接电脑"
                                ConnectionState.CONNECTING -> "连接中..."
                                ConnectionState.CONNECTED -> "断开连接"
                                ConnectionState.RECONNECTING -> "重连中..."
                            }
                        )
                    }
                    Button(
                        onClick = { showScanDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEFEFEF),
                            contentColor = Color.Black
                        )
                    ) {
                        Text("设备列表")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        val features = listOf(
            Feature("🖱", "鼠标", MouseControlActivity::class.java),
            Feature("📺", "遥控器", RemoteControlActivity::class.java),
            Feature("🌀", "触控板"),
            Feature("⋯", "更多")
        )
        FeatureGrid(features)
        // 🔹 功能网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

//            item { FeatureCard("🖱", "鼠标"){
//                // 点击打开 MouseControlActivity
//                context.startActivity(
//                    Intent(context, MouseControlActivity::class.java)
//                )
//            } }
//            item { FeatureCard("📺", "电视时光") }
//            item { FeatureCard("🌀", "触控板") }
//            item { FeatureCard("⋯", "更多") }
        }

    }
    //}
    if (showScanDialog) {
        LaunchedEffect(Unit) {
            vm.discoverPC()
        }
        ScanDialog(
            devices = vm.devices,
            onDismiss = { showScanDialog = false },
            onDeviceClick = { device ->
                vm.connectToServer(device)
                showScanDialog = false
            },
            isScanning = vm.isScanning
        )
    }
}
@Composable
fun SmallFunctionCard(icon: String, title: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.size(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEFEFEF)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 12.sp)
        }
    }
}
// 功能项数据类
data class Feature(
    val icon: String,
    val title: String,
    val targetActivity: Class<*>? = null // 点击跳转的页面，可为 null
)
@Composable
fun FeatureGrid(features: List<Feature>) {
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(features) { feature ->
            FeatureCard(
                icon = feature.icon,
                title = feature.title,
                onClick = {
                    feature.targetActivity?.let {
                        context.startActivity(Intent(context, it))
                    }
                }
            )
        }
    }
}
@Composable
fun FeatureCard(icon: String, title: String,onClick: () -> Unit = {}) {
    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // 图标
            Text(icon, fontSize = 28.sp)

            // 标题
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
@Composable
fun ScanDialog(
    devices: List<Device>,
    onDismiss: () -> Unit,
    onDeviceClick: (Device) -> Unit,
    isScanning: Boolean
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        ) {

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    when(isScanning){
                        true->{"正在扫描"}
                        false->{"扫描完成"}
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(devices) { device ->
                        DeviceItem(device, onDeviceClick)
                    }
                }
            }
        }
    }
}
@Composable
fun DeviceItem(
    device: Device,
    onClick: (Device) -> Unit
) {
    val context = LocalContext.current
    
    // 获取手机WiFi IP用于判断同一网段
    val phoneIp = remember {
        try {
            val wm = context.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            val dhcp = wm.dhcpInfo
            if (dhcp != null) {
                intToInetAddress(dhcp.ipAddress)
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
    
    // 找到与手机同一网段的IP
    val sameNetworkIp = remember(device.ips, phoneIp) {
        findSameNetworkIp(device.ips, phoneIp)
    }
    
    // 显示的IP：优先显示同一网段的IP，否则显示第一个IP
    val displayIp = sameNetworkIp ?: device.ips.firstOrNull() ?: ""
    
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(device) },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💻",
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(
                    displayIp,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                // 如果有多个IP且不是同一网段，显示提示
                if (device.ips.size > 1 && sameNetworkIp == null) {
                    Text(
                        "${device.ips.size}个IP地址",
                        fontSize = 10.sp,
                        color = Color(0xFFFF9800)
                    )
                }
                // 显示操作系统信息
                if (device.os != "unknown") {
                    Text(
                        device.os.uppercase(),
                        fontSize = 10.sp,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
            Text(">", fontSize = 18.sp)
        }
    }
}

private fun intToInetAddress(hostAddress: Int): String {
    val bytes = java.nio.ByteBuffer.allocate(4)
        .order(java.nio.ByteOrder.LITTLE_ENDIAN)
        .putInt(hostAddress)
        .array()
    return java.net.InetAddress.getByAddress(bytes).hostAddress ?: "0.0.0.0"
}

private fun findSameNetworkIp(ips: List<String>, phoneIp: String): String? {
    if (ips.isEmpty() || phoneIp.isEmpty()) return null
    
    val phonePrefix = getNetworkPrefix(phoneIp)
    
    for (ip in ips) {
        if (getNetworkPrefix(ip) == phonePrefix) {
            return ip
        }
    }
    
    return null
}

private fun getNetworkPrefix(ip: String): String {
    val parts = ip.split(".")
    if (parts.size >= 3) {
        return "${parts[0]}.${parts[1]}.${parts[2]}"
    }
    return ip
}