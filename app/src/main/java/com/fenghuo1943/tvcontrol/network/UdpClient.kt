package com.fenghuo1943.tvcontrol.network

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class UdpClient @Inject constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var socket: DatagramSocket? = null
    private var serverIp: InetAddress? = null
    private val sendChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private var receiverJob: Job? = null
    private var heartbeatJob: Job? = null
    private var senderJob: Job? = null
    private var lastPongTime = 0L
    private var reconnectDelay = 2000L
    private val _state = MutableStateFlow(InternalState.IDLE)
    val state: StateFlow<InternalState> = _state

    // =========================
    // 🚀 连接
    // =========================
    fun connect(ip: String) {
        scope.launch {
            disconnect()

            try {
                _state.value = InternalState.CONNECTING

                serverIp = InetAddress.getByName(ip)

                socket = DatagramSocket().apply {
                    soTimeout = 2000
                }

                startReceiver()
                startSender()
                startHeartbeat()

                sendRaw("HELLO")

                waitForAck()

            } catch (e: Exception) {
                _state.value = InternalState.DISCONNECTED
            }
        }
    }

    // =========================
    // 📤 发送
    // =========================
    fun send(data: ByteArray) {
        //Log.d("TV",data.toString())
        sendChannel.trySend(data)
    }


    private fun startSender() {
        senderJob = scope.launch {
            for (data in sendChannel) {
                try {
                    val packet = DatagramPacket(data, data.size, serverIp, 5001)
                    socket?.send(packet)
                } catch (_: Exception) {}
            }
        }
    }

    private fun sendRaw(msg: String) {
        send(msg.toByteArray())
    }

    // =========================
    // 📡 接收
    // =========================
    private fun startReceiver() {
        receiverJob = scope.launch {
            val buffer = ByteArray(1024)

            while (isActive) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)

                    val msg = String(packet.data, 0, packet.length)

                    when (msg) {
                        "OK" -> {
                            _state.value = InternalState.CONNECTED
                            reconnectDelay = 2000L
                            lastPongTime = System.currentTimeMillis()
                        }

                        "PONG" -> {
                            lastPongTime = System.currentTimeMillis()
                        }
                    }

                } catch (_: SocketTimeoutException) {
                } catch (_: Exception) {
                }
            }
        }
    }

    // =========================
    // ❤️ 心跳
    // =========================
    private fun startHeartbeat() {
        heartbeatJob = scope.launch {
            while (isActive) {

                if (_state.value == InternalState.CONNECTED) {
                    sendRaw("PING")

                    val now = System.currentTimeMillis()

                    if (now - lastPongTime > 5000) {
                        _state.value = InternalState.DISCONNECTED
                        reconnect()
                    }
                }

                delay(2000)
            }
        }
    }

    // =========================
    // 🔁 自动重连
    // =========================
    private fun reconnect() {
        scope.launch {
            delay(reconnectDelay)

            reconnectDelay = (reconnectDelay * 1.5)
                .toLong()
                .coerceAtMost(10000L)

            serverIp?.hostAddress?.let {
                connect(it)
            }
        }
    }

    // =========================
    // ⏳ 等待 ACK
    // =========================
    private suspend fun waitForAck() {
        withTimeoutOrNull(3000) {
            while (_state.value != InternalState.CONNECTED) {
                delay(100)
            }
        } ?: run {
            _state.value = InternalState.ERROR
        }
    }

    // =========================
    // ❌ 断开
    // =========================
    fun disconnect() {
        receiverJob?.cancel()
        heartbeatJob?.cancel()
        senderJob?.cancel()

        receiverJob = null
        heartbeatJob = null
        senderJob = null

        socket?.close()
        socket = null

        _state.value = InternalState.DISCONNECTED
    }
}
enum class InternalState {
    IDLE,          // ⭐ 新增
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}