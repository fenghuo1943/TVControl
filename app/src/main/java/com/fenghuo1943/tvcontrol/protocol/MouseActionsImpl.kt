package com.fenghuo1943.tvassistant.protocol

import com.fenghuo1943.tvassistant.MainViewModel
import com.fenghuo1943.tvassistant.input.CommandType
import com.fenghuo1943.tvassistant.input.InputPacket
import com.fenghuo1943.tvassistant.input.MouseActions
import com.fenghuo1943.tvassistant.input.MouseButton
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MouseActionsImpl(private val vm: MainViewModel): MouseActions {

    override fun move(dx: Float, dy: Float) {
        val buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(dx.toInt())
        buf.putInt(dy.toInt())
        vm.send(InputPacket(CommandType.Move, buf.array()))
    }
    override fun leftClick() {

        vm.send(InputPacket(CommandType.Click, byteArrayOf(MouseButton.Left.value)))
    }
    override fun rightClick() {
        vm.send(InputPacket(CommandType.Click, byteArrayOf(MouseButton.Right.value)))
    }
    override fun down(button: MouseButton) {
        vm.send(InputPacket(CommandType.MouseDown, byteArrayOf(button.value)))
    }
    override fun up(button: MouseButton) {
        vm.send(InputPacket(CommandType.MouseUp, byteArrayOf(button.value)))
    }
    override fun scroll(dy: Float) {
        val buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(dy.toInt())
        vm.send(InputPacket(CommandType.Scroll, buf.array()))
    }
    override fun hScroll(dx: Float) {
        val buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(dx.toInt())
        vm.send(InputPacket(CommandType.HScroll, buf.array()))
    }
    override fun keyDown(vk: Int, modifier: Int) {
        val buf = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(vk)
        buf.putShort(modifier.toShort())
        vm.send(
            InputPacket(CommandType.KeyDown, buf.array())
        )
    }
    override fun keyUp(vk: Int, modifier: Int) {
        val buf = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(vk)
        buf.putShort(modifier.toShort())
        vm.send(
            InputPacket(CommandType.KeyUp, buf.array())
        )
    }
    override fun sendText(text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val packet = InputPacket(CommandType.TextInput, bytes)
        vm.sendTextTcp(packet)   // ⭐ 改这里
    }
}