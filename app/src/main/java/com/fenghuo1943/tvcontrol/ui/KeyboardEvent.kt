package com.fenghuo1943.tvcontrol.ui

sealed class KeyboardEvent {
    data class KeyDown(val keyCode: Int) : KeyboardEvent()
    data class KeyUp(val keyCode: Int) : KeyboardEvent()
    data class TextInput(val text: String) : KeyboardEvent()
}
