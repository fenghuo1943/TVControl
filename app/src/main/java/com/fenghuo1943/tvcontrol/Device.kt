package com.fenghuo1943.tvcontrol

data class Device(
    val ip: String,
    var name: String = "未知设备",
    var connected: Boolean = false
)
