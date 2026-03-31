package com.fenghuo1943.tvcontrol.network

import kotlinx.coroutines.*
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TcpClient @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var socket: Socket? = null
    private var output: OutputStream? = null

    fun connect(ip: String, port: Int = 5002) {
        scope.launch {
            try {
                socket = Socket().apply {
                    connect(InetSocketAddress(ip, port), 2000)
                }
                output = socket?.getOutputStream()
            } catch (_: Exception) {
            }
        }
    }

    fun send(data: ByteArray) {
        scope.launch {
            try {
                output?.write(data)
                output?.flush()
            } catch (_: Exception) {
            }
        }
    }

    fun disconnect() {
        try {
            output?.close()
            socket?.close()
        } catch (_: Exception) {
        }
    }
}