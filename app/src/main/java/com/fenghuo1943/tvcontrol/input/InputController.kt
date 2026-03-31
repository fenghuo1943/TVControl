package com.fenghuo1943.tvassistant.input

import android.util.Log
import com.fenghuo1943.tvassistant.ui.KeyboardEvent
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
                if (pressedKeys.add(event.keyCode)) {
                    sender.keyDown(event.keyCode)
                }
                //Log.d("TV","keycode:${event.keyCode}")
                if (event.keyCode == 0x08) {
                    startDeleteLoop()
                }
            }

            is KeyboardEvent.KeyUp -> {
                pressedKeys.remove(event.keyCode)
                sender.keyUp(event.keyCode)

                if (event.keyCode == 0x08) {
                    stopDeleteLoop()
                }
            }
        }
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