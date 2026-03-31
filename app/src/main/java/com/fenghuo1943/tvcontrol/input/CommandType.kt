package com.fenghuo1943.tvcontrol.input

enum class CommandType(val value: Byte) {
    Move(0),
    Click(1),
    Scroll(2),
    HScroll(3),
    KeyDown(4),
    KeyUp(5),
    MouseDown(6),   // 新增
    MouseUp(7),      // 新增
    TextInput(20),   // 输入字符串（核心）
    ComboKey(21),    // 组合键（Ctrl+C）
}
enum class MouseButton(val value: Byte) {
    Left(0),
    Right(1),
    Middle(2)
}
