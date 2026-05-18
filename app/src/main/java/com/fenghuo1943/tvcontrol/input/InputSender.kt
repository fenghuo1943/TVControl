package com.fenghuo1943.tvcontrol.input

import com.fenghuo1943.tvcontrol.network.TcpClient
import com.fenghuo1943.tvcontrol.network.UdpClient
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InputSender @Inject constructor(private val tcpClient: TcpClient, private val udpClient: UdpClient) {
    fun send(packet: InputPacket) {
        udpClient.send(packet.toBytes())
    }

    fun sendText(packet: InputPacket) {
        tcpClient.send(packet.toBytes())
    }
    fun keyDown(vk: Int, modifier: Int = 0) {
        val buf = ByteBuffer.allocate(6)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(vk)
            .putShort(modifier.toShort())
            .array()

        send(InputPacket(CommandType.KeyDown, buf))
    }

    fun keyUp(vk: Int, modifier: Int = 0) {
        val buf = ByteBuffer.allocate(6)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(vk)
            .putShort(modifier.toShort())
            .array()

        send(InputPacket(CommandType.KeyUp, buf))
    }
}