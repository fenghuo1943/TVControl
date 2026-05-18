package com.fenghuo1943.tvcontrol.network

import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.util.Log
import com.fenghuo1943.tvcontrol.Device
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

class DiscoveryService(val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tv_assistant", Context.MODE_PRIVATE)
    // 自动连接上次记忆的设备
    fun getLastDevice(): Device? {
        val json = prefs.getString("last_device", null) ?: return null
        val obj = JSONObject(json)
        val ips = if (obj.has("ips")) {
            val jsonArray = obj.getJSONArray("ips")
            List(jsonArray.length()) { jsonArray.getString(it) }
        } else {
            listOf(obj.getString("ip"))
        }
        return Device(
            ips = ips,
            name = obj.getString("name"),
            os = obj.optString("os", "unknown")
        )
    }

    fun saveLastDevice(device: Device) {
        val json = JSONObject().apply {
            put("ips", org.json.JSONArray(device.ips))
            put("name", device.name)
            put("os", device.os)
        }
        prefs.edit().putString("last_device", json.toString()).apply()
    }

    fun discover(onFound: (Device) -> Unit, onFinish: () -> Unit) {
        thread {
            var socket: DatagramSocket? = null
            try {
                val info = getNetworkInfo(context) ?: return@thread
                socket = DatagramSocket()
                socket.broadcast = true
                //val sendData = "DISCOVER_PC".toByteArray()
                val sendData = JSONObject().put("type", "discover").toString().toByteArray()
                // ⭐ 局域网广播
                val packet = DatagramPacket(
                    sendData,
                    sendData.size,
                    InetAddress.getByName(info.broadcast),
                    9999
                )
                //socket.send(packet)
                // ⭐ 全网广播（兜底）
                val globalPacket = DatagramPacket(
                    sendData,
                    sendData.size,
                    InetAddress.getByName("255.255.255.255"),
                    9999
                )
                socket.send(globalPacket)
                socket.soTimeout = 3000
                while (true) {
                    val buffer = ByteArray(1024)
                    val resp = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(resp)
                        val msg = String(resp.data, 0, resp.length)
                        val obj = JSONObject(msg)
                        if (obj.optString("type") == "discover_response") {
                            val ipsArray = obj.getJSONArray("ips")
                            val ips = List(ipsArray.length()) { ipsArray.getString(it) }
                            val name = obj.optString("name", "未知设备")
                            val os = obj.optString("os", "unknown")
                            
                            // 找到与手机同一网段的IP
                            val sameNetworkIp = findSameNetworkIp(ips, info.ip)
                            
                            val device = Device(
                                ips = ips,
                                name = name,
                                os = os
                            )
                            Log.d("TV", "Found: $device, Same network IP: $sameNetworkIp")
                            onFound(device)
                        }
//                        if (msg.startsWith("PC_HERE")) {
//                            val parts = msg.split(":")
//                            val ip = parts.getOrNull(1) ?: continue
//                            val name = parts.getOrNull(2) ?: "PC"
//                            onFound(Device(ip, name))
//                        }
                    } catch (_: SocketTimeoutException) {
                        break
                    }catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                socket?.close()
                onFinish()
            }
        }
    }
    // ================= 工具 =================
    data class NetworkInfo(
        val ip: String,
        val mask: String,
        val broadcast: String
    )
    
    /**
     * 查找与手机WiFi在同一网段的IP地址
     */
    private fun findSameNetworkIp(ips: List<String>, phoneIp: String): String? {
        if (ips.isEmpty()) return null
        
        val phonePrefix = getNetworkPrefix(phoneIp)
        
        // 查找第一个与手机在同一网段的IP
        for (ip in ips) {
            if (getNetworkPrefix(ip) == phonePrefix) {
                return ip
            }
        }
        
        return null
    }
    
    /**
     * 获取IP的网络前缀（例如：192.168.1.100 -> 192.168.1）
     */
    private fun getNetworkPrefix(ip: String): String {
        val parts = ip.split(".")
        if (parts.size >= 3) {
            return "${parts[0]}.${parts[1]}.${parts[2]}"
        }
        return ip
    }
    private fun getNetworkInfo(context: Context): NetworkInfo? {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wm.dhcpInfo ?: return null
        val ip = intToInetAddress(dhcp.ipAddress)
        val mask = intToInetAddress(dhcp.netmask)
        val broadcastInt = dhcp.ipAddress or dhcp.netmask.inv()
        val broadcast = intToInetAddress(broadcastInt)
        return NetworkInfo(ip, mask, broadcast)
    }

    private fun intToInetAddress(hostAddress: Int): String {
        val bytes = ByteBuffer.allocate(4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(hostAddress)
            .array()

        return InetAddress.getByAddress(bytes).hostAddress ?: "0.0.0.0"
    }
}