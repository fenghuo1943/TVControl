package com.fenghuo1943.tvcontrol.input

import android.util.Log
import com.fenghuo1943.tvcontrol.ui.KeyboardEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InputController(
    private val sender: InputSender,
    private val scope: CoroutineScope
) {
    private var deleteJob: Job? = null
    private var lastText = ""
    private val pressedKeys = mutableSetOf<Int>()
    
    // 组合键模式相关
    var comboKeyMode = false
    private val activeModifiers = mutableSetOf<Int>()
    private val modifierKeys = setOf(0x11, 0x5B, 0x12, 0x10) // Ctrl, Win, Alt, Shift

    fun handle(event: KeyboardEvent) {
        when (event) {

            is KeyboardEvent.TextInput -> {
                val diff = event.text.removePrefix(lastText)
                if (diff.isNotEmpty()) {
                    sender.sendText(
                        InputPacket(
                        CommandType.TextInput,
                        diff.toByteArray(Charsets.UTF_8)))
                }
                lastText = event.text
            }

            is KeyboardEvent.KeyDown -> {
                // 如果event中直接指定了modifier，优先使用
                if (event.modifier != 0) {
                    // 直接使用event中的modifier
                    if (pressedKeys.add(event.keyCode)) {
                        sender.keyDown(event.keyCode, event.modifier)
                    }
                } else if (comboKeyMode && event.keyCode in modifierKeys) {
                    // 组合键模式下，修饰键只记录状态，不发送
                    if (activeModifiers.add(event.keyCode)) {
                        pressedKeys.add(event.keyCode)
                    }
                } else {
                    // 非组合键模式或非修饰键，正常处理
                    if (pressedKeys.add(event.keyCode)) {
                        // 如果在组合键模式下且有活跃的修饰键，一起发送
                        if (comboKeyMode && activeModifiers.isNotEmpty()) {
                            val modifier = calculateModifier()
                            sender.keyDown(event.keyCode, modifier)
                            //activeModifiers.clear()
                        } else {
                            sender.keyDown(event.keyCode)
                        }
                    }
                }
                //Log.d("TV","keycode:${event.keyCode}")
                if (event.keyCode == 0x08) {
                    startDeleteLoop()
                }
            }

            is KeyboardEvent.KeyUp -> {
                pressedKeys.remove(event.keyCode)
                
                // 如果event中直接指定了modifier，优先使用
                if (event.modifier != 0) {
                    // 直接使用event中的modifier
                    sender.keyUp(event.keyCode, event.modifier)
                } else if (comboKeyMode && event.keyCode in modifierKeys) {
                    // 组合键模式下，修饰键弹起时清除状态
                    activeModifiers.remove(event.keyCode)
                } else {
                    // 非组合键模式或非修饰键，正常发送弹起事件
                    if (comboKeyMode && activeModifiers.isNotEmpty()) {
                        val modifier = calculateModifier()
                        sender.keyUp(event.keyCode, modifier)
                        activeModifiers.clear()
                    } else {
                        sender.keyUp(event.keyCode)
                    }
                }

                if (event.keyCode == 0x08) {
                    stopDeleteLoop()
                }
            }
        }
    }
    
    private fun calculateModifier(): Int {
        var modifier = 0
        if (0x11 in activeModifiers) modifier = modifier or 0x0002 // Ctrl
        if (0x5B in activeModifiers) modifier = modifier or 0x0008 // Win
        if (0x12 in activeModifiers) modifier = modifier or 0x0001 // Alt
        if (0x10 in activeModifiers) modifier = modifier or 0x0004 // Shift
        return modifier
    }

    private fun startDeleteLoop() {
        deleteJob?.cancel()
        deleteJob = scope.launch {
            delay(300)
            while (true) {
                sender.keyDown(0x08)
                delay(50)
            }
        }
    }

    private fun stopDeleteLoop() {
        deleteJob?.cancel()
    }
}