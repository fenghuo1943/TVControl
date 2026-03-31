package com.fenghuo1943.tvassistant.protocol

import android.view.KeyEvent



fun androidKeyToVK(keyCode: Int): Int {
    return when (keyCode) {
        // ===== 字母 A-Z =====
        in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z ->
            0x41 + (keyCode - KeyEvent.KEYCODE_A)
        // ===== 数字 0-9 =====
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 ->
            0x30 + (keyCode - KeyEvent.KEYCODE_0)

        // ===== 功能键 F1-F12 =====
        in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12 ->
            0x70 + (keyCode - KeyEvent.KEYCODE_F1)

        // ===== 基本控制键 =====
        KeyEvent.KEYCODE_ENTER -> 0x0D
        KeyEvent.KEYCODE_DEL -> 0x08
        KeyEvent.KEYCODE_TAB -> 0x09
        KeyEvent.KEYCODE_SPACE -> 0x20
        KeyEvent.KEYCODE_ESCAPE -> 0x1B

        // ===== 修饰键 =====
        KeyEvent.KEYCODE_SHIFT_LEFT,
        KeyEvent.KEYCODE_SHIFT_RIGHT -> 0x10

        KeyEvent.KEYCODE_CTRL_LEFT,
        KeyEvent.KEYCODE_CTRL_RIGHT -> 0x11

        KeyEvent.KEYCODE_ALT_LEFT,
        KeyEvent.KEYCODE_ALT_RIGHT -> 0x12

        KeyEvent.KEYCODE_META_LEFT,
        KeyEvent.KEYCODE_META_RIGHT -> 0x5B // Win键

        // ===== 方向键 =====
        KeyEvent.KEYCODE_DPAD_LEFT -> 0x25
        KeyEvent.KEYCODE_DPAD_UP -> 0x26
        KeyEvent.KEYCODE_DPAD_RIGHT -> 0x27
        KeyEvent.KEYCODE_DPAD_DOWN -> 0x28

        // ===== 导航键 =====
        KeyEvent.KEYCODE_MOVE_HOME -> 0x24
        KeyEvent.KEYCODE_MOVE_END -> 0x23
        KeyEvent.KEYCODE_PAGE_UP -> 0x21
        KeyEvent.KEYCODE_PAGE_DOWN -> 0x22
        KeyEvent.KEYCODE_INSERT -> 0x2D
        KeyEvent.KEYCODE_FORWARD_DEL -> 0x2E

        // ===== 小键盘 =====
        KeyEvent.KEYCODE_NUMPAD_0 -> 0x60
        KeyEvent.KEYCODE_NUMPAD_1 -> 0x61
        KeyEvent.KEYCODE_NUMPAD_2 -> 0x62
        KeyEvent.KEYCODE_NUMPAD_3 -> 0x63
        KeyEvent.KEYCODE_NUMPAD_4 -> 0x64
        KeyEvent.KEYCODE_NUMPAD_5 -> 0x65
        KeyEvent.KEYCODE_NUMPAD_6 -> 0x66
        KeyEvent.KEYCODE_NUMPAD_7 -> 0x67
        KeyEvent.KEYCODE_NUMPAD_8 -> 0x68
        KeyEvent.KEYCODE_NUMPAD_9 -> 0x69

        KeyEvent.KEYCODE_NUMPAD_ADD -> 0x6B
        KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> 0x6D
        KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> 0x6A
        KeyEvent.KEYCODE_NUMPAD_DIVIDE -> 0x6F
        KeyEvent.KEYCODE_NUMPAD_DOT -> 0x6E
        KeyEvent.KEYCODE_NUMPAD_ENTER -> 0x0D

        // ===== 符号键（重点）=====
        KeyEvent.KEYCODE_MINUS -> 0xBD
        KeyEvent.KEYCODE_EQUALS -> 0xBB
        KeyEvent.KEYCODE_LEFT_BRACKET -> 0xDB
        KeyEvent.KEYCODE_RIGHT_BRACKET -> 0xDD
        KeyEvent.KEYCODE_BACKSLASH -> 0xDC
        KeyEvent.KEYCODE_SEMICOLON -> 0xBA
        KeyEvent.KEYCODE_APOSTROPHE -> 0xDE
        KeyEvent.KEYCODE_GRAVE -> 0xC0
        KeyEvent.KEYCODE_COMMA -> 0xBC
        KeyEvent.KEYCODE_PERIOD -> 0xBE
        KeyEvent.KEYCODE_SLASH -> 0xBF

        // ===== 锁定键 =====
        KeyEvent.KEYCODE_CAPS_LOCK -> 0x14
        KeyEvent.KEYCODE_NUM_LOCK -> 0x90
        KeyEvent.KEYCODE_SCROLL_LOCK -> 0x91

        else -> 0
    }
}

object VK {
    const val ENTER = 0x0D
    const val ESC = 0x1B
    const val BACKSPACE = 0x08
    const val TAB = 0x09
    const val SPACE = 0x20

    const val LEFT = 0x25
    const val UP = 0x26
    const val RIGHT = 0x27
    const val DOWN = 0x28

    const val SHIFT = 0x10
    const val CTRL = 0x11
    const val ALT = 0x12
}
enum class KeyModifier(val value: Int) {
    NONE(0),
    SHIFT(1),
    CTRL(2),
    ALT(4),
    META(8); // Windows键

    companion object {
        fun combine(vararg mods: KeyModifier): Int {
            return mods.fold(0) { acc, m -> acc or m.value }
        }
    }
}