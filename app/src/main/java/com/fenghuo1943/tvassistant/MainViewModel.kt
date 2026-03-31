package com.fenghuo1943.tvassistant

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenghuo1943.tvassistant.input.InputSender
import com.fenghuo1943.tvassistant.network.DiscoveryService
import com.fenghuo1943.tvassistant.network.InternalState
import com.fenghuo1943.tvassistant.network.TcpClient
import com.fenghuo1943.tvassistant.network.UdpClient
import com.fenghuo1943.tvassistant.input.InputPacket
import com.fenghuo1943.tvassistant.ui.components.SnackbarManager
import com.fenghuo1943.tvassistant.ui.components.SnackbarType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger


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
                    }
                     InternalState.ERROR->{
                         currentDevice?.connected = false
                         connectionState = ConnectionState.ERROR
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
                            discovery.saveLastDevice(device)
                            ip = device.ip

                            SnackbarManager.show(
                                "已连接 ${device.ip}",
                                SnackbarType.SUCCESS
                            )
                        }

                        connectionState =ConnectionState.CONNECTED
                    }

                    InternalState.DISCONNECTED -> {

                        currentDevice?.connected = false
                        if (!hasConnectedOnce) {
                            return@collect
                        }

                        if (isManualDisconnect) {
                            SnackbarManager.show(
                                "断开连接",
                                SnackbarType.SUCCESS
                            )
                            connectionState =ConnectionState.DISCONNECTED
                        } else {
                            if (reconnectAttempts < maxReconnectAttempts) {
                                reconnectAttempts++
                                SnackbarManager.show(
                                    "连接断开，正在重连",
                                    SnackbarType.ERROR
                                )
                                connectionState =ConnectionState.RECONNECTING
                            } else {
                                SnackbarManager.show(
                                    "连接失败",
                                    SnackbarType.ERROR
                                )
                                connectionState =ConnectionState.DISCONNECTED
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
        udpClient.connect(device.ip)
        tcpClient.connect(device.ip)
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