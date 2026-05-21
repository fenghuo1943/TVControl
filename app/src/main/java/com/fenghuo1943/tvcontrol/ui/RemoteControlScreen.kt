package com.fenghuo1943.tvcontrol.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fenghuo1943.tvcontrol.MainViewModel
import com.fenghuo1943.tvcontrol.input.InputController
import com.fenghuo1943.tvcontrol.protocol.MouseActionsImpl
import com.fenghuo1943.tvcontrol.ui.common.StatusBarStyle
import kotlinx.coroutines.launch

@Composable
fun RemoteControlScreen(
    modifier: Modifier = Modifier,
    vm: MainViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val handler = remember(vm) { InputController(vm.inputSender, scope) }
    val actions = remember(vm) { MouseActionsImpl(vm) }
    
    // 浅色主题颜色（与鼠标界面一致）
    val backgroundColor = Color(0xFFF0F0F0)
    val accentColor = Color(0xFF4CAF50) // 绿色箭头
    val iconColor = Color(0xFF333333) // 深灰色图标
    
    StatusBarStyle(isLight = true)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶部空白，占据空间
            Spacer(modifier = Modifier.weight(1f))
            
            // 中央圆形控制器
            RemoteControlPad(
                modifier = Modifier.fillMaxWidth(),
                handler = handler,
                accentColor = accentColor,
                iconColor = iconColor
            )
            
            // 中间空白
            Spacer(modifier = Modifier.weight(1f))
            
            // 底部导航栏
            BottomNavigationBar(
                handler = handler,
                backgroundColor = Color(0xFFE0E0E0),
                iconColor = iconColor
            )
        }
    }
}

@Composable
fun RemoteControlPad(
    modifier: Modifier = Modifier,
    handler: InputController,
    accentColor: Color,
    iconColor: Color
) {
    val padSize = 280.dp
    val innerCircleSize = 100.dp
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(padSize),
            contentAlignment = Alignment.Center
        ) {
            // 外圆 - 细线边框
            Box(
                modifier = Modifier
                    .size(padSize)
                    .border(1.5.dp, accentColor.copy(alpha = 0.4f), CircleShape)
            )
            
            // 上键
            Box(
                modifier = Modifier
                    .size(padSize)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { 
                        handler.handle(KeyboardEvent.KeyDown(0x26))
                        handler.handle(KeyboardEvent.KeyUp(0x26))
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    "^",
                    fontSize = 48.sp,
                    color = accentColor,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
            
            // 下键
            Box(
                modifier = Modifier
                    .size(padSize)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { 
                        handler.handle(KeyboardEvent.KeyDown(0x28))
                        handler.handle(KeyboardEvent.KeyUp(0x28))
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    "v",
                    fontSize = 48.sp,
                    color = accentColor,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }
            
            // 左键
            Box(
                modifier = Modifier
                    .size(padSize)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { 
                        handler.handle(KeyboardEvent.KeyDown(0x25))
                        handler.handle(KeyboardEvent.KeyUp(0x25))
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "<",
                    fontSize = 48.sp,
                    color = accentColor,
                    modifier = Modifier.padding(start = 20.dp)
                )
            }
            
            // 右键
            Box(
                modifier = Modifier
                    .size(padSize)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { 
                        handler.handle(KeyboardEvent.KeyDown(0x27))
                        handler.handle(KeyboardEvent.KeyUp(0x27))
                    },
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    ">",
                    fontSize = 48.sp,
                    color = accentColor,
                    modifier = Modifier.padding(end = 20.dp)
                )
            }
            
            // 内圆 - OK键
            Box(
                modifier = Modifier
                    .size(innerCircleSize)
                    .background(Color(0xFF4CAF50), CircleShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { 
                        handler.handle(KeyboardEvent.KeyDown(0x0D))
                        handler.handle(KeyboardEvent.KeyUp(0x0D))
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "OK",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    handler: InputController,
    backgroundColor: Color,
    iconColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(backgroundColor)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 菜单键 - 三条横线
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable { 
                    handler.handle(KeyboardEvent.KeyDown(0x5D))
                    handler.handle(KeyboardEvent.KeyUp(0x5D))
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(iconColor.copy(alpha = 0.7f))
                )
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(iconColor.copy(alpha = 0.7f))
                )
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(iconColor.copy(alpha = 0.7f))
                )
            }
        }
        
        // 主页键 - 房子图标
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable { 
                    handler.handle(KeyboardEvent.KeyDown(0x5B))
                    handler.handle(KeyboardEvent.KeyUp(0x5B))
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "⌂",
                fontSize = 28.sp,
                color = iconColor.copy(alpha = 0.7f)
            )
        }
        
        // 返回键 - 左箭头
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable { 
                    handler.handle(KeyboardEvent.KeyDown(0x1B))
                    handler.handle(KeyboardEvent.KeyUp(0x1B))
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "←",
                fontSize = 28.sp,
                color = iconColor.copy(alpha = 0.7f)
            )
        }
    }
}

// 使用文本字符表示图标，无需额外依赖