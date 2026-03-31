package com.fenghuo1943.tvassistant.input

import java.nio.ByteBuffer
import java.nio.ByteOrder

/*
data class InputPacket(
    val type: CommandType,
    val x: Int = 0,
    val y: Int = 0,
    val button: MouseButton = MouseButton.Left, // 0=左键,1=右键,2=中键
    val keyCode: Int = 0,           // 新增
    val modifier: Int = 0           // 新增（位运算）
) {
    fun toBytes(): ByteArray {
        //Log.d("TV","$x+$y")
        return ByteBuffer.allocate(16)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put(type.value) // 1字节
            .putInt(x)       // 4字节
            .putInt(y)       // 4字节
            .put(button.value) // 1字节
            .putInt(keyCode)       // 4 ⭐
            .putShort(modifier.toShort()) // 2 ⭐
            .array()
    }
}*/
data class InputPacket(
    val type: CommandType,
    val payload: ByteArray = ByteArray(0)
) {
    fun toBytes(): ByteArray {
        val buffer = ByteBuffer.allocate(4 + payload.size)
            .order(ByteOrder.LITTLE_ENDIAN)

        buffer.put(type.value)                 // 1
        buffer.put(0)                          // flags（先预留）
        buffer.putShort(payload.size.toShort())// 2
        buffer.put(payload)                    // N

        return buffer.array()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as InputPacket
        if (type != other.type) return false
        if (!payload.contentEquals(other.payload)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}