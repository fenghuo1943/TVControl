package com.fenghuo1943.tvcontrol.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenghuo1943.tvcontrol.ConnectionState

@Composable
fun ConnectionStatus(state: ConnectionState) {
    val (text, color) = when (state) {
        ConnectionState.CONNECTED -> "已连接" to Color(0xFF4CAF50)
        ConnectionState.CONNECTING -> "连接中..." to Color.Gray
        ConnectionState.RECONNECTING -> "重连中..." to Color(0xFFFF9800)
        ConnectionState.DISCONNECTED -> "未连接" to Color.Red
        ConnectionState.ERROR -> "连接失败" to Color.Red
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = color, fontSize = 12.sp)
    }
}
