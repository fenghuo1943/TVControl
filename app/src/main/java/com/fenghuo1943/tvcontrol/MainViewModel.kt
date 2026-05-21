package com.fenghuo1943.tvcontrol

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenghuo1943.tvcontrol.input.InputSender
import com.fenghuo1943.tvcontrol.network.DiscoveryService
import com.fenghuo1943.tvcontrol.network.InternalState
import com.fenghuo1943.tvcontrol.network.TcpClient
import com.fenghuo1943.tvcontrol.network.UdpClient
import com.fenghuo1943.tvcontrol.input.InputPacket
import com.fenghuo1943.tvcontrol.ui.components.SnackbarManager
import com.fenghuo1943.tvcontrol.ui.components.SnackbarType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch


@HiltViewModel
class MainViewModel @Inject constructor(
    private val appState: AppState,
    val inputSender: InputSender,
    private val udpClient: UdpClient,
    private val tcpClient: TcpClient
) : ViewModel() {

    private val _ip = mutableStateOf(appState.ip)
    var ip: String
        get() = _ip.value
        set(value) {
            _ip.value = value        // ✅ 改变 Compose state
            appState.ip = value      // ✅ 同步到单例
        }
    val devices = mutableStateListOf<Device>()

    var currentDevice by mutableStateOf<Device?>(null)
    // 连接状态
    private val _connectionState = mutableStateOf(appState.connectionState)
    var connectionState: ConnectionState
        get() = _connectionState.value
        set(value) {
            _connectionState.value = value
            appState.connectionState = value
        }

    lateinit var discovery: DiscoveryService
    var isScanning by mutableStateOf(false)
    private var isManualDisconnect = false
    private var hasConnectedOnce = false
    private var connectedIp: String = "" // 记录实际连接成功的IP
    private var connectingIps: List<String> = emptyList() // 当前正在尝试的IP列表
    private var connectingIndex: Int = 0 // 当前正在尝试的IP索引

    var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    var toastEvent: ((String) -> Unit)? = null
    init {
        observeClient()
    }

    // =========================
    // 🔁 监听底层状态（核心）
    // =========================
    private fun observeClient() {
        viewModelScope.launch {
            udpClient.state.collect { internal ->
                 when (internal) {
                    InternalState.IDLE -> {
                        currentDevice?.connected = false
                        connectionState = ConnectionState.DISCONNECTED
                        connectedIp = "" // 清空记录的IP
                        connectingIps = emptyList()
                        connectingIndex = 0
                    }
                     InternalState.ERROR->{
                         currentDevice?.connected = false
                         connectionState = ConnectionState.ERROR
                         connectedIp = "" // 清空记录的IP
                         connectingIps = emptyList()
                         connectingIndex = 0
                     }
                    InternalState.CONNECTING -> {
                        connectionState = if (connectionState == ConnectionState.RECONNECTING)
                            ConnectionState.RECONNECTING
                        else
                            ConnectionState.CONNECTING
                    }

                    InternalState.CONNECTED -> {
                        isManualDisconnect = false
                        reconnectAttempts = 0
                        hasConnectedOnce = true

                        currentDevice?.let { device ->
                            device.connected = true
                            // 使用实际连接的IP
                            val actualIp = if (connectingIndex >= 0 && connectingIndex < connectingIps.size) {
                                connectingIps[connectingIndex]
                            } else {
                                device.ip
                            }
                            connectedIp = actualIp // 记录实际连接的IP
                            Log.d("TV", "Using IP for display: $actualIp (index=$connectingIndex, list=$connectingIps)")
                            discovery.saveLastDevice(device)
                            ip = actualIp
                            Log.d("TV", "Connected to $ip")
                            /* SnackbarManager.show(
                                "已连接 $actualIp",
                                SnackbarType.SUCCESS
                            ) */
                        }
                        
                        // 清空连接状态
                        connectingIps = emptyList()
                        connectingIndex = 0

                        connectionState =ConnectionState.CONNECTED
                    }

                    InternalState.DISCONNECTED -> {

                        currentDevice?.connected = false
                        if (!hasConnectedOnce) {
                            return@collect
                        }

                        if (isManualDisconnect) {
                            /* SnackbarManager.show(
                                "断开连接",
                                SnackbarType.SUCCESS
                            ) */
                            connectionState =ConnectionState.DISCONNECTED
                            connectedIp = "" // 清空记录的IP
                            connectingIps = emptyList()
                            connectingIndex = 0
                        } else {
                            if (reconnectAttempts < maxReconnectAttempts) {
                                reconnectAttempts++
                                /* SnackbarManager.show(
                                    "连接断开，正在重连",
                                    SnackbarType.ERROR
                                ) */
                                connectionState =ConnectionState.RECONNECTING
                            } else {
                                SnackbarManager.show(
                                    "连接失败",
                                    SnackbarType.ERROR
                                )
                                connectionState =ConnectionState.DISCONNECTED
                                connectedIp = "" // 清空记录的IP
                                connectingIps = emptyList()
                                connectingIndex = 0
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================
    // 🔧 初始化
    // =========================
    fun updateDiscovery(d: DiscoveryService) {
        discovery = d
    }

    fun networkInit() {
        discovery.getLastDevice()?.let { device ->
            ip = device.ip
            connectToServer(device)
        }
    }

    // =========================
    // 🔌 连接
    // =========================
    fun connectToServer(device: Device) {
        isManualDisconnect = false
        currentDevice = device
        connectionState = ConnectionState.CONNECTING
        connectedIp = "" // 清空之前记录的IP
        
        // 获取手机WiFi IP
        val phoneIp = getPhoneWifiIp()
        
        // 找到与手机同一网段的IP
        val sameNetworkIp = findSameNetworkIp(device.ips, phoneIp)
        
        val ipsToTry = if (sameNetworkIp != null) {
            // 优先连接同一网段的IP
            Log.d("TV", "Found same network IP: $sameNetworkIp")
            listOf(sameNetworkIp) + device.ips.filter { it != sameNetworkIp }
        } else {
            // 没有同一网段的IP，从第一个开始尝试
            Log.d("TV", "No same network IP found, trying all IPs in order")
            device.ips
        }
        
        // 保存当前尝试的IP列表
        connectingIps = ipsToTry
        connectingIndex = 0
        
        // 开始尝试连接
        attemptConnectWithFallback(device, ipsToTry, 0)
    }
    
    /**
     * 递归尝试连接多个IP
     */
    private fun attemptConnectWithFallback(device: Device, ips: List<String>, index: Int) {
        if (index >= ips.size) {
            // 所有IP都失败了
            Log.d("TV", "All IPs failed")
            connectionState = ConnectionState.ERROR
            connectingIps = emptyList()
            connectingIndex = 0
            return
        }
        
        val currentIp = ips[index]
        connectingIndex = index // 更新当前索引
        Log.d("TV", "Attempting to connect to: $currentIp (${index + 1}/${ips.size})")
        
        // 断开之前的连接
        udpClient.disconnect()
        tcpClient.disconnect()
        
        // 设置超时
        viewModelScope.launch {
            kotlinx.coroutines.delay(5000)
            // 如果5秒后还在连接中，说明超时了
            if (connectingIndex == index && connectionState == ConnectionState.CONNECTING) {
                Log.d("TV", "Connection to $currentIp timeout")
                udpClient.disconnect()
                tcpClient.disconnect()
                // 尝试下一个IP
                attemptConnectWithFallback(device, ips, index + 1)
            }
        }
        
        // 开始连接
        udpClient.connect(currentIp)
        tcpClient.connect(currentIp)
    }
    
    /**
     * 获取手机WiFi IP地址
     */
    private fun getPhoneWifiIp(): String {
        return try {
            val wm = discovery.context.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            val dhcp = wm.dhcpInfo
            if (dhcp != null) {
                intToInetAddress(dhcp.ipAddress)
            } else {
                ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
    
    /**
     * 查找与手机在同一网段的IP
     */
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
    
    /**
     * 获取IP的网络前缀
     */
    private fun getNetworkPrefix(ip: String): String {
        val parts = ip.split(".")
        if (parts.size >= 3) {
            return "${parts[0]}.${parts[1]}.${parts[2]}"
        }
        return ip
    }
    
    private fun intToInetAddress(hostAddress: Int): String {
        val bytes = java.nio.ByteBuffer.allocate(4)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .putInt(hostAddress)
            .array()
        return java.net.InetAddress.getByAddress(bytes).hostAddress ?: "0.0.0.0"
    }

    // =========================
    // ❌ 断开
    // =========================
    fun disconnect() {
        isManualDisconnect = true
        udpClient.disconnect()
        tcpClient.disconnect()

        connectionState = ConnectionState.DISCONNECTED
        currentDevice?.connected = false
        currentDevice = null
        connectedIp = "" // 清空记录的IP
        connectingIps = emptyList()
        connectingIndex = 0
    }

    // =========================
    // 📤 发送
    // =========================
    fun send(packet: InputPacket) {
        inputSender.send(packet)
    }
    fun sendTextTcp(packet: InputPacket) {
        inputSender.send(packet)
    }




    // =========================
    // 🔍 扫描
    // =========================
    fun discoverPC() {
        devices.clear()
        isScanning=true
        discovery.discover(
            onFound = { device ->
                if (devices.none { it.ip == device.ip }) {
                    devices.add(device)
                }
            },
            onFinish = {
                isScanning=false
                //SnackbarManager.show("扫描完成", SnackbarType.SUCCESS)
            }
        )
    }
}
enum class ConnectionState {
    DISCONNECTED,   // 未连接
    CONNECTING,     // 连接中
    CONNECTED,      // 已连接
    RECONNECTING,    // 重连中
    ERROR        //连接失败
}
@Singleton
class AppState @Inject constructor() {
    var ip: String = ""

    var connectionState: ConnectionState = ConnectionState.DISCONNECTED


    // 单例 UdpClient
//    val udpClient: UdpClient = UdpClient()
//    val tcpClient: TcpClient = TcpClient()
    //val inputSender: InputSender = InputSender(tcpClient, udpClient)
}