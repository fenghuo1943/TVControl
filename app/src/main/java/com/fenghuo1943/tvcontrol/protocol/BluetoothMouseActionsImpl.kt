package com.fenghuo1943.tvcontrol.protocol

import com.fenghuo1943.tvcontrol.bluetooth.BleHidService
import com.fenghuo1943.tvcontrol.input.MouseActions
import com.fenghuo1943.tvcontrol.input.MouseButton
import kotlinx.coroutines.launch

class BluetoothMouseActionsImpl(private val bleService: BleHidService) : MouseActions {
    
    override fun move(dx: Float, dy: Float) {
        bleService.sendMouseMove(dx.toInt(), dy.toInt())
    }
    
    override fun leftClick() {
        // 按下
        bleService.sendMouseButton(left = true)
        // 短暂延迟后释放
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            kotlinx.coroutines.delay(50)
            bleService.sendMouseButton()
        }
    }
    
    override fun rightClick() {
        bleService.sendMouseButton(right = true)
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            kotlinx.coroutines.delay(50)
            bleService.sendMouseButton()
        }
    }
    
    override fun down(button: MouseButton) {
        when (button) {
            MouseButton.Left -> bleService.sendMouseButton(left = true)
            MouseButton.Right -> bleService.sendMouseButton(right = true)
            MouseButton.Middle -> bleService.sendMouseButton(middle = true)
        }
    }
    
    override fun up(button: MouseButton) {
        // 释放所有按键
        bleService.sendMouseButton()
    }
    
    override fun scroll(dy: Float) {
        bleService.sendMouseMove(0, 0, dy.toInt())
    }
    
    override fun hScroll(dx: Float) {
        // HID 标准鼠标报告不支持水平滚动，暂时忽略
        // 或者可以通过扩展报告描述符来实现
    }
    
    override fun keyDown(vk: Int, modifier: Int) {
        // 蓝牙 HID 鼠标模式不支持键盘
    }
    
    override fun keyUp(vk: Int, modifier: Int) {
        // 蓝牙 HID 鼠标模式不支持键盘
    }
    
    override fun sendText(text: String) {
        // 蓝牙 HID 鼠标模式不支持文本输入
    }
}
