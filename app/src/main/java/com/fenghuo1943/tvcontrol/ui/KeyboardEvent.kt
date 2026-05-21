package com.fenghuo1943.tvcontrol.ui

sealed class KeyboardEvent {
    data class KeyDown(val keyCode: Int, val modifier: Int = 0) : KeyboardEvent()
    data class KeyUp(val keyCode: Int, val modifier: Int = 0) : KeyboardEvent()
    data class TextInput(val text: String) : KeyboardEvent()
}
