package com.fenghuo1943.tvassistant.network

import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.util.Log
import com.fenghuo1943.tvassistant.Device
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

class DiscoveryService(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tv_assistant", Context.MODE_PRIVATE)
    // 自动连接上次记忆的设备
    fun getLastDevice(): Device? {
        val json = prefs.getString("last_device", null) ?: return null
        val obj = JSONObject(json)
        return Device(
            ip = obj.getString("ip"),
            name = obj.getString("name")
        )
    }

    fun saveLastDevice(device: Device) {
        val json = JSONObject().apply {
            put("ip", device.ip)
            put("name", device.name)
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
                            val device = Device(
                                ip = obj.getString("ip"),
                                name = obj.getString("name")
                            )
                            Log.d("TV", "Found: $device")
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