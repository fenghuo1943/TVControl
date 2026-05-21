package com.fenghuo1943.tvcontrol.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fenghuo1943.tvcontrol.MainViewModel
import com.fenghuo1943.tvcontrol.input.InputController
import com.fenghuo1943.tvcontrol.protocol.MouseActionsImpl
import com.fenghuo1943.tvcontrol.ui.common.ConnectionStatus
import com.fenghuo1943.tvcontrol.ui.common.StatusBarStyle
import kotlinx.coroutines.launch

@Composable
fun RemoteControlScreen(
    modifier: Modifier = Modifier,
    vm: MainViewModel = hiltViewModel()
) {
    val state by vm::connectionState
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
        // 右上角连接状态指示器（放在最上层）
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 12.dp)
                .zIndex(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConnectionStatus(state = state)
        }
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶部空白，占据空间
            Spacer(modifier = Modifier.weight(0.5f))
            
            // 应用选择器和功能按键区域
            AppSelectorAndFunctionKeys(
                handler = handler,
                accentColor = accentColor,
                iconColor = iconColor
            )
            
            // 中央圆形控制器
            RemoteControlPad(
                modifier = Modifier.fillMaxWidth(),
                handler = handler,
                accentColor = accentColor,
                iconColor = iconColor
            )
            
            // 中间空白
            Spacer(modifier = Modifier.weight(0.5f))
            
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
    val haptic = LocalHapticFeedback.current
    val padSize = 280.dp
    val innerCircleSize = 100.dp
    val scope = rememberCoroutineScope()
    
    // 滑动阈值（像素）
    val swipeThreshold = 30f
    
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
            // 外圆 - 细线边框 + 滑动手势和点击检测
            Box(
                modifier = Modifier
                    .size(padSize)
                    .border(1.5.dp, accentColor.copy(alpha = 0.4f), CircleShape)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                // 滑动结束，不执行操作
                            }
                        ) { change, dragAmount ->
                            // 垂直滑动检测
                            if (Math.abs(dragAmount) > swipeThreshold) {
                                if (dragAmount < 0) {
                                    // 向上滑动 - 发送上键
                                    scope.launch {
                                        handler.handle(KeyboardEvent.KeyDown(0x26))
                                        handler.handle(KeyboardEvent.KeyUp(0x26))
                                    }
                                } else {
                                    // 向下滑动 - 发送下键
                                    scope.launch {
                                        handler.handle(KeyboardEvent.KeyDown(0x28))
                                        handler.handle(KeyboardEvent.KeyUp(0x28))
                                    }
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                // 滑动结束，不执行操作
                            }
                        ) { change, dragAmount ->
                            // 水平滑动检测
                            if (Math.abs(dragAmount) > swipeThreshold) {
                                if (dragAmount < 0) {
                                    // 向左滑动 - 发送左键
                                    scope.launch {
                                        handler.handle(KeyboardEvent.KeyDown(0x25))
                                        handler.handle(KeyboardEvent.KeyUp(0x25))
                                    }
                                } else {
                                    // 向右滑动 - 发送右键
                                    scope.launch {
                                        handler.handle(KeyboardEvent.KeyDown(0x27))
                                        handler.handle(KeyboardEvent.KeyUp(0x27))
                                    }
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            // 计算相对于中心的坐标
                            val centerX = padSize.toPx() / 2
                            val centerY = padSize.toPx() / 2
                            val x = offset.x - centerX
                            val y = offset.y - centerY
                            
                            // 内圆半径（OK键区域）
                            val innerRadius = innerCircleSize.toPx() / 2
                            
                            // 判断是否在内圆（OK键）
                            if (x * x + y * y < innerRadius * innerRadius) {
                                // OK键
                                scope.launch {
                                    handler.handle(KeyboardEvent.KeyDown(0x0D))
                                    handler.handle(KeyboardEvent.KeyUp(0x0D))
                                }
                            } else {
                                // 判断方向（根据角度）
                                val angle = Math.atan2(y.toDouble(), x.toDouble())
                                val degrees = Math.toDegrees(angle)
                                
                                when {
                                    degrees < -45 && degrees > -135 -> {
                                        // 上键
                                        scope.launch {
                                            handler.handle(KeyboardEvent.KeyDown(0x26))
                                            handler.handle(KeyboardEvent.KeyUp(0x26))
                                        }
                                    }
                                    degrees > 45 && degrees < 135 -> {
                                        // 下键
                                        scope.launch {
                                            handler.handle(KeyboardEvent.KeyDown(0x28))
                                            handler.handle(KeyboardEvent.KeyUp(0x28))
                                        }
                                    }
                                    (degrees >= -45 && degrees <= 45) -> {
                                        // 右键
                                        scope.launch {
                                            handler.handle(KeyboardEvent.KeyDown(0x27))
                                            handler.handle(KeyboardEvent.KeyUp(0x27))
                                        }
                                    }
                                    else -> {
                                        // 左键 (degrees <= -135 || degrees >= 135)
                                        scope.launch {
                                            handler.handle(KeyboardEvent.KeyDown(0x25))
                                            handler.handle(KeyboardEvent.KeyUp(0x25))
                                        }
                                    }
                                }
                            }
                        }
                    }
            )
            
            // 方向键图标显示（仅用于视觉，点击由外层Box处理）
            // 上键图标
            Box(
                modifier = Modifier.size(padSize),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    "^",
                    fontSize = 48.sp,
                    color = accentColor,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
            
            // 下键图标
            Box(
                modifier = Modifier.size(padSize),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    "v",
                    fontSize = 48.sp,
                    color = accentColor,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }
            
            // 左键图标
            Box(
                modifier = Modifier.size(padSize),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "<",
                    fontSize = 48.sp,
                    color = accentColor,
                    modifier = Modifier.padding(start = 20.dp)
                )
            }
            
            // 右键图标
            Box(
                modifier = Modifier.size(padSize),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    ">",
                    fontSize = 48.sp,
                    color = accentColor,
                    modifier = Modifier.padding(end = 20.dp)
                )
            }
            
            // 内圆 - OK键（仅用于视觉，点击由外层Box处理）
            Box(
                modifier = Modifier
                    .size(innerCircleSize)
                    .background(Color(0xFF4CAF50), CircleShape),
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
    val haptic = LocalHapticFeedback.current
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
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    handler.handle(KeyboardEvent.KeyDown(0x1B))
                    handler.handle(KeyboardEvent.KeyUp(0x1B))
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
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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

@Composable
fun AppSelectorAndFunctionKeys(
    handler: InputController,
    accentColor: Color,
    iconColor: Color
) {
    var selectedApp by remember { mutableStateOf("电视") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 第一部分：应用选择器（电视、游戏、视频）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AppSelectorButton(
                text = "电视",
                isSelected = selectedApp == "电视",
                onClick = { selectedApp = "电视" },
                accentColor = accentColor,
                iconColor = iconColor
            )
            AppSelectorButton(
                text = "游戏",
                isSelected = selectedApp == "游戏",
                onClick = { selectedApp = "游戏" },
                accentColor = accentColor,
                iconColor = iconColor
            )
            AppSelectorButton(
                text = "视频",
                isSelected = selectedApp == "视频",
                onClick = { selectedApp = "视频" },
                accentColor = accentColor,
                iconColor = iconColor
            )
        }
        
        // 第二部分：功能按键网格容器
        FunctionKeysGrid(
            selectedApp = selectedApp,
            handler = handler,
            accentColor = accentColor,
            iconColor = iconColor
        )
    }
}

@Composable
fun AppSelectorButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    iconColor: Color
) {
    val haptic = LocalHapticFeedback.current
    val backgroundColor = if (isSelected) accentColor else Color.White
    val textColor = if (isSelected) Color.White else iconColor
    val borderColor = if (isSelected) accentColor else Color.Gray.copy(alpha = 0.3f)
    
    Box(
        modifier = Modifier
            .width(80.dp)
            .height(40.dp)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .background(backgroundColor, RoundedCornerShape(20.dp))
            .clickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
fun FunctionKeysGrid(
    selectedApp: String,
    handler: InputController,
    accentColor: Color,
    iconColor: Color
) {
    // 根据不同应用显示不同的功能按键
    val functionKeys = when (selectedApp) {
        "电视" -> listOf(
            "频道+", "频道-", "音量+", "音量-",
            "静音", "信源", "菜单", "返回",
            "主页", "设置", "录制", "播放/暂停"
        )
        "游戏" -> listOf(
            "开始", "全屏", "暂停", "停止",
            "重启", "设置"
        )
        "视频" -> listOf(
            "播放", "暂停", "停止", "快进",
            "快退", "上一集", "下一集", "字幕",
            "音轨", "全屏", "截图", "收藏"
        )
        else -> emptyList()
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(functionKeys) { key ->
            FunctionKeyButton(
                text = key,
                onClick = { 
                    // 根据不同按键发送相应的指令
                    when (key) {
                        "开始" -> {
                            // 回车键
                            handler.handle(KeyboardEvent.KeyDown(0x0D))
                            handler.handle(KeyboardEvent.KeyUp(0x0D))
                        }
                        "全屏" -> {
                            // F11键
                            handler.handle(KeyboardEvent.KeyDown(0x7A))
                            handler.handle(KeyboardEvent.KeyUp(0x7A))
                        }
                        "暂停" -> {
                            // F4键
                            handler.handle(KeyboardEvent.KeyDown(0x73))
                            handler.handle(KeyboardEvent.KeyUp(0x73))
                        }
                        "停止" -> {
                            // F5键
                            handler.handle(KeyboardEvent.KeyDown(0x74))
                            handler.handle(KeyboardEvent.KeyUp(0x74))
                        }
                        "重启" -> {
                            // F6键
                            handler.handle(KeyboardEvent.KeyDown(0x75))
                            handler.handle(KeyboardEvent.KeyUp(0x75))
                        }
                        "设置" -> {
                            // Ctrl + , 组合键，使用modifier参数（Ctrl = 0x0002）
                            handler.handle(KeyboardEvent.KeyDown(0xBC, 0x0002)) // 逗号键，带Ctrl修饰符
                            handler.handle(KeyboardEvent.KeyUp(0xBC, 0x0002))   // 逗号键弹起
                        }
                        else -> {
                            // 默认处理
                            handler.handle(KeyboardEvent.KeyDown(0x20))
                            handler.handle(KeyboardEvent.KeyUp(0x20))
                        }
                    }
                },
                accentColor = accentColor,
                iconColor = iconColor
            )
        }
    }
}

@Composable
fun FunctionKeyButton(
    text: String,
    onClick: () -> Unit,
    accentColor: Color,
    iconColor: Color
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .border(0.5.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .background(Color.White, RoundedCornerShape(6.dp))
            .clickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = iconColor,
            maxLines = 1
        )
    }
}