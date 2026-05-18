package com.fenghuo1943.tvcontrol

data class Device(
    val ips: List<String>, // 支持多个IP地址
    var name: String = "未知设备",
    var os: String = "unknown",
    var connected: Boolean = false
) {
    // 兼容旧代码，返回第一个IP
    val ip: String
        get() = ips.firstOrNull() ?: ""
}
