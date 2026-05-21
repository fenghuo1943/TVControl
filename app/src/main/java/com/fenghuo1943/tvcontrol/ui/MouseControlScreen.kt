package com.fenghuo1943.tvcontrol.ui

import android.util.Log
import android.view.ViewTreeObserver
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import com.fenghuo1943.tvcontrol.MainViewModel
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fenghuo1943.tvcontrol.input.GestureEngine
import com.fenghuo1943.tvcontrol.input.InputController
import com.fenghuo1943.tvcontrol.input.MouseActions
import com.fenghuo1943.tvcontrol.protocol.MouseActionsImpl
import com.fenghuo1943.tvcontrol.input.MouseButton
import com.fenghuo1943.tvcontrol.ui.common.ConnectionStatus
import com.fenghuo1943.tvcontrol.ui.common.StatusBarStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MouseControlScreen(
    modifier: Modifier = Modifier,
    vm: MainViewModel = hiltViewModel()

) {
    val state by vm::connectionState

    val actions = remember(vm) { MouseActionsImpl(vm) }
    var showCustomKeyboard by remember { mutableStateOf(false) }
    var showComputerKeyboard by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val handler = remember { InputController(vm.inputSender, scope) }
    var requestKeyboard by remember { mutableStateOf(false) }
    var comboKeyMode by remember { mutableStateOf(false) }
    
    // 监听 comboKeyMode 变化，立即同步到 InputController
    LaunchedEffect(comboKeyMode) {
        Log.d("TV", "Syncing comboKeyMode to InputController: $comboKeyMode")
        handler.comboKeyMode = comboKeyMode
    }
    
    // 键盘高度配置
    val customKeyboardHeight = 300.dp // 自定义键盘固定高度
    var systemKeyboardHeight by remember { mutableStateOf<Dp?>(null) } // 系统键盘高度（动态检测，用于日志）
    var isKeyboardVisible by remember { mutableStateOf(false) } // 跟踪是否有键盘显示
    
    // 获取系统键盘高度
    val view = LocalView.current
    val density = LocalDensity.current

    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            view.getWindowVisibleDisplayFrame(rect)
            val screenHeight = view.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            
            Log.d("TV", "Screen height: $screenHeight, Visible bottom: ${rect.bottom}, Keypad height: $keypadHeight")

            if (keypadHeight > screenHeight * 0.15) {
                // 如果高度超过屏幕的15%，认为是键盘
                val detectedHeight = with(density) { keypadHeight.toDp() }
                Log.d("TV", "Detected system keyboard height: $detectedHeight")
                systemKeyboardHeight = detectedHeight
            } else {
                Log.d("TV", "System keyboard not detected (height too small)")
                // 键盘高度为0，说明键盘被收起了
                if (keypadHeight < screenHeight * 0.05 && requestKeyboard) {
                    Log.d("TV", "System keyboard dismissed externally, updating state")
                    requestKeyboard = false
                    systemKeyboardHeight = null
                }
            }
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }
    
    // 监听键盘显示状态变化（在 KeyboardControlBar 之前定义）

    StatusBarStyle(isLight = true)
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color(0xFFF0F0F0))
    ) {
        // 右上角连接状态指示器（放在最上层）
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 16.dp)
                .zIndex(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConnectionStatus(state = state)
        }
        
        // 主要内容区域（固定布局）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            MouseTouchArea(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                actions = actions
            )
            Spacer(modifier = Modifier.height(8.dp))
            MouseButtons(actions = actions)
            Spacer(modifier = Modifier.height(8.dp))

            val keyboardController = LocalSoftwareKeyboardController.current
            
            // 监听键盘显示状态变化
            LaunchedEffect(showCustomKeyboard, showComputerKeyboard, requestKeyboard) {
                val wasVisible = isKeyboardVisible
                isKeyboardVisible = showCustomKeyboard || showComputerKeyboard || requestKeyboard
                
                Log.d("TV", "Keyboard visibility changed: showCustom=$showCustomKeyboard, showComputer=$showComputerKeyboard, requestSystem=$requestKeyboard, isVisible=$isKeyboardVisible")
                
                // 如果所有键盘都关闭了，重置系统键盘高度
                if (!isKeyboardVisible && wasVisible) {
                    systemKeyboardHeight = null
                    Log.d("TV", "All keyboards closed, reset system keyboard height")
                }
            }
            
            KeyboardControlBar(
                onCustomKeyboard = {
                    if (showCustomKeyboard) {
                        // 如果自定义键盘已显示，则关闭它
                        showCustomKeyboard = false
                        Log.d("TV", "Hide custom keyboard")
                    } else {
                        // 关闭其他键盘，显示自定义键盘
                        keyboardController?.hide()
                        requestKeyboard = false
                        showComputerKeyboard = false
                        showCustomKeyboard = true
                        Log.d("TV", "Show custom keyboard with height: $customKeyboardHeight")
                    }
                },
                onSystemKeyboard = {
                    if (requestKeyboard) {
                        // 如果系统键盘已显示，则关闭它
                        requestKeyboard = false
                        Log.d("TV", "Hide system keyboard")
                    } else {
                        // 关闭其他键盘，显示系统键盘
                        showCustomKeyboard = false
                        showComputerKeyboard = false
                        scope.launch {
                            delay(1)
                            requestKeyboard = true
                        }
                    }
                },
                onComputerKeyboard = {
                    if (showComputerKeyboard) {
                        // 如果电脑键盘已显示，则关闭它
                        showComputerKeyboard = false
                        Log.d("TV", "Hide computer keyboard")
                    } else {
                        // 关闭其他键盘，显示电脑键盘
                        keyboardController?.hide()
                        requestKeyboard = false
                        showCustomKeyboard = false
                        showComputerKeyboard = true
                        Log.d("TV", "Show computer keyboard with height: $customKeyboardHeight")
                    }
                }
            )
            
            // 📺 遥控器区域
            if (showCustomKeyboard) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(customKeyboardHeight)
                    ) {
                        RemoteControlPanel(actions = actions, keyboardHeight = customKeyboardHeight)
                    }
                }
            }
            
            // 💻 电脑键盘区域
            if (showComputerKeyboard) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(customKeyboardHeight)
                    ) {
                        ComputerKeyboard(
                            handler = handler,
                            actions = actions, 
                            keyboardHeight = customKeyboardHeight, 
                            comboKeyMode = comboKeyMode, 
                            onComboKeyModeChange = { newValue ->
                                Log.d("TV", "Combo key mode changed from $comboKeyMode to $newValue")
                                comboKeyMode = newValue
                            },
                            onClose = {
                                showComputerKeyboard = false
                                Log.d("TV", "Computer keyboard closed via close button")
                            }
                        )
                    }
                }
            }
            
            // ⌨️ 系统键盘预留空间
            if (requestKeyboard && systemKeyboardHeight != null) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(systemKeyboardHeight ?: customKeyboardHeight)
                    ) {
                        // 空 Box，仅用于占据空间，系统键盘会覆盖在此区域上方
                    }
                }
            }
            
            // ⌨️ 系统键盘输入（隐藏TextField）
            SystemKeyboardInput(
                text = textInput,
                requestFocus = requestKeyboard,
                onTextChange = { run { textInput = it } },
                onEvent = { 
                    handler.handle(it) 
                },
                onFocusHandled = { }
            )
        }


    }
}
@Composable
fun MouseTouchArea(
    modifier: Modifier = Modifier,
    actions: MouseActions,
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val engine = remember {
        GestureEngine(actions, scope, haptic)
    }
    Box(
        modifier = modifier
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .background(Color.White)
            .pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent()
                        engine.onEvent(event.changes)
                    }
                }
            }
    )
}
@Composable
fun MouseButtons(modifier: Modifier = Modifier,actions: MouseActions) {
    var pressedButtons by remember { mutableStateOf(setOf<MouseButton>()) }
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MouseButtonItem(
            text="左键",
            pressed = MouseButton.Left in pressedButtons,
            onDown = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                pressedButtons = pressedButtons + MouseButton.Left
                actions.down((MouseButton.Left))
            },
            onUp = {
                pressedButtons = pressedButtons - MouseButton.Left
                actions.up(MouseButton.Left)
            })
        MouseButtonItem(text = "中键",
            pressed = MouseButton.Middle in pressedButtons,
            onDown = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                pressedButtons = pressedButtons - MouseButton.Middle
                actions.down(MouseButton.Middle)
            },
            onUp = {
                pressedButtons = pressedButtons - MouseButton.Middle
                actions.up(MouseButton.Middle)
            }
        )
        MouseButtonItem(
            text = "右键",
            pressed = MouseButton.Right in pressedButtons,
            onDown = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                pressedButtons = pressedButtons - MouseButton.Right
                actions.down(MouseButton.Right)
            },
            onUp = {
                pressedButtons = pressedButtons - MouseButton.Right
                actions.up(MouseButton.Right)
            }
        )
    }
}
@Composable
fun MouseButtonItem(
    text: String,
    pressed: Boolean = false,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp, 50.dp)
            .background(
                if (pressed) Color.DarkGray else Color.Gray,
                RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume() // 👈 消费 DOWN
                    onDown()
                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.find { it.id == pointerId } ?: continue
                        if (!change.pressed) {
                            // 👈 手指抬起（无论在哪）
                            change.consume()
                            onUp()
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White)
    }
}
@Composable
fun KeyboardControlBar(
    onCustomKeyboard: () -> Unit,
    onSystemKeyboard: () -> Unit,
    onComputerKeyboard: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // ⌨️ 系统键盘按钮
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSystemKeyboard()
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3) // 蓝色背景
            )
        ) {
            Text("输入法")
        }
        Spacer(modifier = Modifier.width(8.dp))
        // 💻 电脑键盘按钮
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onComputerKeyboard()
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3) // 蓝色背景
            )
        ) {
            Text("电脑键盘")
        }
        Spacer(modifier = Modifier.width(8.dp))
        // 🎮 遥控器按钮
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCustomKeyboard()
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3) // 蓝色背景
            )
        ) {
            Text("遥控器")
        }
    }
}
@Composable
fun SystemKeyboardInput(
    text: String,
    onTextChange: (String) -> Unit,
    requestFocus: Boolean,
    onEvent: (KeyboardEvent) -> Unit, // ⭐ 统一出口
    onFocusHandled: () -> Unit // 新增回调
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    OutlinedTextField(
        value = text,
        onValueChange = {
            onTextChange(it)
            onEvent(KeyboardEvent.TextInput(it))
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .alpha(0f)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                //
                Log.d("TV","event${event.key.nativeKeyCode}")
                val vk = mapKey(event.key)
                Log.d("TV","vk${vk}")
                if (vk != null) {
                    when (event.type) {
                        KeyEventType.KeyDown ->
                            onEvent(KeyboardEvent.KeyDown(vk))
                        KeyEventType.KeyUp ->
                            onEvent(KeyboardEvent.KeyUp(vk))
                    }
                    true
                } else {
                    false
                }
            }
    )
    
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            delay(100)
            focusRequester.requestFocus()
            keyboardController?.show()
            onFocusHandled()
            Log.d("TV", "System keyboard shown")
        }
    }
    
    // 监听 requestFocus 变化，当变为 false 时隐藏键盘
    LaunchedEffect(requestFocus) {
        if (!requestFocus) {
            keyboardController?.hide()
            Log.d("TV", "System keyboard hidden")
        }
    }
}
fun mapKey(key: Key): Int? {
    return when (key) {
        Key.Backspace -> 0x08
        Key.Enter -> 0x0D
        Key.Spacebar -> 0x20

        Key.DirectionLeft -> 0x25
        Key.DirectionUp -> 0x26
        Key.DirectionRight -> 0x27
        Key.DirectionDown -> 0x28

        else -> null
    }
}
@Composable
fun CustomKeyboard(actions: MouseActions, keyboardHeight: Dp = 250.dp) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(keyboardHeight)
            .background(Color.White, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(16.dp)
    ) {

        Row(horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()) {

            Button(onClick = { actions.keyDown(0x11); actions.keyUp(0x11) }) {
                Text("Ctrl")
            }

            Button(onClick = { actions.keyDown(0x12); actions.keyUp(0x12) }) {
                Text("Alt")
            }

            Button(onClick = { actions.keyDown(0x10); actions.keyUp(0x10) }) {
                Text("Shift")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()) {

            Button(onClick = { actions.keyDown(0x57); actions.keyUp(0x57) }) {
                Text("W")
            }
            Button(onClick = { actions.keyDown(0x41); actions.keyUp(0x41) }) {
                Text("A")
            }
            Button(onClick = { actions.keyDown(0x53); actions.keyUp(0x53) }) {
                Text("S")
            }
            Button(onClick = { actions.keyDown(0x44); actions.keyUp(0x44) }) {
                Text("D")
            }
        }
    }
}

@Composable
fun ComputerKeyboard(
    handler: InputController,
    actions: MouseActions, 
    keyboardHeight: Dp = 344.dp,
    comboKeyMode: Boolean = false,
    onComboKeyModeChange: (Boolean) -> Unit = {},
    onClose: () -> Unit = {}
) {
    val smallKeySize = 32.4f.dp
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    
    // 跟踪修饰键的按下状态
    var modifierKeysState by remember { mutableStateOf(setOf<Int>()) }
    
    // 当组合键模式关闭时，清除所有修饰键状态
    LaunchedEffect(comboKeyMode) {
        Log.d("TV", "ComputerKeyboard LaunchedEffect: comboKeyMode = $comboKeyMode, modifierKeysState = $modifierKeysState")
        if (!comboKeyMode && modifierKeysState.isNotEmpty()) {
            // 发送所有修饰键的弹起事件
            modifierKeysState.forEach { keyCode ->
                handler.handle(KeyboardEvent.KeyUp(keyCode))
            }
            modifierKeysState = emptySet()
        }
    }
    
    // 辅助函数：发送普通键盘事件
    val sendKey = { keyCode: Int ->
        handler.handle(KeyboardEvent.KeyDown(keyCode))
        handler.handle(KeyboardEvent.KeyUp(keyCode))
        // 在组合键模式下，发送非修饰键后清除所有修饰键的UI状态
        if (comboKeyMode && modifierKeysState.isNotEmpty()) {
            modifierKeysState.forEach { keyCode ->
                handler.handle(KeyboardEvent.KeyUp(keyCode))
            }
            modifierKeysState = emptySet()
        }
    }
    
    // 辅助函数：切换修饰键状态
    val toggleModifierKey = { keyCode: Int ->
        if (comboKeyMode) {
            if (keyCode in modifierKeysState) {
                // 如果已经按下，则弹起
                modifierKeysState = modifierKeysState - keyCode
                handler.handle(KeyboardEvent.KeyUp(keyCode))
            } else {
                // 如果未按下，则按下
                modifierKeysState = modifierKeysState + keyCode
                handler.handle(KeyboardEvent.KeyDown(keyCode))
            }
        } else {
            // 非组合键模式下，正常按下-弹起
            handler.handle(KeyboardEvent.KeyDown(keyCode))
            handler.handle(KeyboardEvent.KeyUp(keyCode))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(keyboardHeight)
            .background(Color(0xFFE8E8E8), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(8.dp)
    ) {
        // 页面切换标签
        Row(
            modifier = Modifier.fillMaxWidth().height(36.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 组合键模式选择框
            Checkbox(
                checked = comboKeyMode,
                onCheckedChange = { newValue ->
                    onComboKeyModeChange(newValue)
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("组合键", fontSize = 10.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.weight(1f))
            
            TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(0) } }, modifier = Modifier.height(36.dp)) {
                Text("字母数字", fontSize = 12.sp, color = if (pagerState.currentPage == 0) Color(0xFF2196F3) else Color.Gray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(1) } }, modifier = Modifier.height(36.dp)) {
                Text("符号功能", fontSize = 12.sp, color = if (pagerState.currentPage == 1) Color(0xFF2196F3) else Color.Gray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            // ❌ 关闭电脑键盘按钮
            TextButton(
                onClick = onClose, 
                modifier = Modifier.height(36.dp)
            ) {
                Text("关闭", fontSize = 12.sp, color = Color.Red)
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            pageSpacing = 8.dp,
            beyondViewportPageCount = 1
        ) { page ->
            Column(modifier = Modifier.fillMaxSize()) {
                if (page == 0) {
                    // 第一页：数字与字母
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().height(smallKeySize * 1.0f), verticalAlignment = Alignment.CenterVertically) {
                        
                        (1..9).forEach { i -> ComputerKey(text = "$i", size = smallKeySize, onClick = { sendKey(0x30 + i) }) }
                        ComputerKey(text = "0", size = smallKeySize, onClick = { sendKey(0x30) })
                        ComputerKey(text = "Back", size = smallKeySize * 1.5f, onClick = { sendKey(0x08) })
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().height(smallKeySize * 1.0f), verticalAlignment = Alignment.CenterVertically) {
                        
                        listOf('Q','W','E','R','T','Y','U','I','O','P').forEach { c -> ComputerKey(text = c.toString(), size = smallKeySize, onClick = { sendKey(c.code) }) }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().height(smallKeySize * 1.0f), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(smallKeySize * 0.5f))
                        listOf('A','S','D','F','G','H','J','K','L').forEach { c -> ComputerKey(text = c.toString(), size = smallKeySize, onClick = { sendKey(c.code) }) }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().height(smallKeySize * 1.0f), verticalAlignment = Alignment.CenterVertically) {
                        ComputerKey(text = "Shift", size = smallKeySize * 1.6f, onClick = { toggleModifierKey(0x10) }, isPressed = 0x10 in modifierKeysState)
                        listOf('Z','X','C','V','B','N','M').forEach { c -> ComputerKey(text = c.toString(), size = smallKeySize, onClick = { sendKey(c.code) }) }
                        ComputerKey(text = "Enter", size = smallKeySize * 1.6f, onClick = { sendKey(0x0D) })
                        
                    }
                } else {
                    // 第二页：功能键与符号
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().height(smallKeySize * 1.0f), verticalAlignment = Alignment.CenterVertically) {
                        ComputerKey(text = "Esc", size = smallKeySize, onClick = { sendKey(0x1B) })
                        (1..8).forEach { i -> ComputerKey(text = "F$i", size = smallKeySize, onClick = { sendKey(0x6F + i) }) }
                        ComputerKey(text = "Del", size = smallKeySize, onClick = { sendKey(0x2E) })
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().height(smallKeySize * 1.0f), verticalAlignment = Alignment.CenterVertically) {
                        (9..12).forEach { i -> ComputerKey(text = "F$i", size = smallKeySize, onClick = { sendKey(0x6F + i) }) }
                        ComputerKey(text = "`~", size = smallKeySize, onClick = { sendKey(0xC0) })
                        ComputerKey(text = "-_", size = smallKeySize, onClick = { sendKey(0xBD) })
                        ComputerKey(text = "=+", size = smallKeySize, onClick = { sendKey(0xBB) })
                        ComputerKey(text = "[{", size = smallKeySize, onClick = { sendKey(0xDB) })
                        ComputerKey(text = "}]", size = smallKeySize, onClick = { sendKey(0xDD) })
                        ComputerKey(text = "\\|", size = smallKeySize, onClick = { sendKey(0xDC) })
                        
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().height(smallKeySize * 1.0f), verticalAlignment = Alignment.CenterVertically) {
                        listOf('!','@','#','$','%','^','&','*','(',')').forEach { c -> 
                            val vkCode = when(c) {
                                '!' -> 0x31 // Shift+1
                                '@' -> 0x32 // Shift+2
                                '#' -> 0x33 // Shift+3
                                '$' -> 0x34 // Shift+4
                                '%' -> 0x35 // Shift+5
                                '^' -> 0x36 // Shift+6
                                '&' -> 0x37 // Shift+7
                                '*' -> 0x38 // Shift+8
                                '(' -> 0x39 // Shift+9
                                ')' -> 0x30 // Shift+0
                                else -> c.code
                            }
                            ComputerKey(text = c.toString(), size = smallKeySize, onClick = { 
                                // 这些符号需要Shift键配合（Shift = 0x0004）
                                if (comboKeyMode) {
                                    // 在组合键模式下，先清除所有其他修饰键
                                    modifierKeysState.forEach { keyCode ->
                                        handler.handle(KeyboardEvent.KeyUp(keyCode))
                                    }
                                    modifierKeysState = emptySet()
                                }
                                
                                // 使用modifier参数发送Shift+键
                                handler.handle(KeyboardEvent.KeyDown(vkCode, 0x0004))
                                handler.handle(KeyboardEvent.KeyUp(vkCode, 0x0004))
                            }) 
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().height(smallKeySize * 1.0f), verticalAlignment = Alignment.CenterVertically) {
                        ComputerKey(text = ";:", size = smallKeySize, onClick = { sendKey(0xBA) })
                        ComputerKey(text = "'\"", size = smallKeySize, onClick = { sendKey(0xDE) })
                        ComputerKey(text = ",<", size = smallKeySize, onClick = { sendKey(0xBC) })
                        ComputerKey(text = ".>", size = smallKeySize, onClick = { sendKey(0xBE) })
                        ComputerKey(text = "/?", size = smallKeySize, onClick = { sendKey(0xBF) })
                    }
                    
                }
                Spacer(modifier = Modifier.height(6.dp))
                // 公共底部控制行
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().height(smallKeySize * 1.0f), verticalAlignment = Alignment.CenterVertically) {
                    
                    ComputerKey(text = "Esc", size = smallKeySize * 1.44f, onClick = { sendKey(0x1B) })
                    ComputerKey(text = "Tab", size = smallKeySize * 1.44f, onClick = { sendKey(0x09) })
                    ComputerKey(text = "Caps", size = smallKeySize * 1.44f, onClick = { sendKey(0x14) })
                    ComputerKey(text = "Shift", size = smallKeySize * 1.44f, onClick = { toggleModifierKey(0x10) }, isPressed = 0x10 in modifierKeysState)
                    ComputerKey(text = "Enter", size = smallKeySize * 1.44f, onClick = { sendKey(0x0D) })
                    ComputerKey(text = "Del", size = smallKeySize, onClick = { sendKey(0x2E) })
                    ComputerKey(text = "↑", size = smallKeySize, onClick = { sendKey(0x26) })
                    ComputerKey(text = "Back", size = smallKeySize * 1.0f, onClick = { sendKey(0x08) })
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().height(smallKeySize * 1.0f), verticalAlignment = Alignment.CenterVertically) {
                    ComputerKey(text = "Ctrl", size = smallKeySize, onClick = { toggleModifierKey(0x11) }, isPressed = 0x11 in modifierKeysState)
                    ComputerKey(text = "Win", size = smallKeySize, onClick = { toggleModifierKey(0x5B) }, isPressed = 0x5B in modifierKeysState)
                    ComputerKey(text = "Alt", size = smallKeySize, onClick = { toggleModifierKey(0x12) }, isPressed = 0x12 in modifierKeysState)
                    ComputerKey(text = "Space", size = smallKeySize * 3.2f, onClick = { sendKey(0x20) })
                    ComputerKey(text = "Alt", size = smallKeySize, onClick = { toggleModifierKey(0x12) }, isPressed = 0x12 in modifierKeysState)
                    ComputerKey(text = "←", size = smallKeySize, onClick = { sendKey(0x25) })
                    ComputerKey(text = "↓", size = smallKeySize, onClick = { sendKey(0x28) })
                    ComputerKey(text = "→", size = smallKeySize, onClick = { sendKey(0x27) })
                }
            }
        }
    }
}

@Composable
fun ComputerKey(
    text: String,
    size: Dp,
    onClick: () -> Unit,
    isPressed: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .width(size)
            .fillMaxHeight()
            .background(
                if (isPressed) Color(0xFF2196F3) else Color.White,
                RoundedCornerShape(4.dp)
            )
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
            color = if (isPressed) Color.White else Color.Black,
            maxLines = 1
        )
    }
}

@Composable
fun RemoteControlPanel(
    actions: MouseActions,
    keyboardHeight: Dp = 300.dp
) {
    var selectedApp by remember { mutableStateOf("电视") }
    
    // 浅色主题颜色（与遥控器界面一致）
    val accentColor = Color(0xFF4CAF50) // 绿色箭头
    val iconColor = Color(0xFF333333) // 深灰色图标
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(keyboardHeight)
            .background(Color.White, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(16.dp)
    ) {
        // 第一部分：应用选择器（电视、游戏、视频）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RemoteAppSelectorButton(
                text = "电视",
                isSelected = selectedApp == "电视",
                onClick = { selectedApp = "电视" },
                accentColor = accentColor,
                iconColor = iconColor
            )
            RemoteAppSelectorButton(
                text = "游戏",
                isSelected = selectedApp == "游戏",
                onClick = { selectedApp = "游戏" },
                accentColor = accentColor,
                iconColor = iconColor
            )
            RemoteAppSelectorButton(
                text = "视频",
                isSelected = selectedApp == "视频",
                onClick = { selectedApp = "视频" },
                accentColor = accentColor,
                iconColor = iconColor
            )
        }
        
        // 第二部分：功能按键网格容器
        RemoteFunctionKeysGrid(
            selectedApp = selectedApp,
            actions = actions,
            accentColor = accentColor,
            iconColor = iconColor
        )
    }
}

@Composable
fun RemoteAppSelectorButton(
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
fun RemoteFunctionKeysGrid(
    selectedApp: String,
    actions: MouseActions,
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
            RemoteFunctionKeyButton(
                text = key,
                onClick = { 
                    // 根据不同按键发送相应的指令
                    when (key) {
                        "开始" -> {
                            // 回车键
                            actions.keyDown(0x0D)
                            actions.keyUp(0x0D)
                        }
                        "全屏" -> {
                            // F11键
                            actions.keyDown(0x7A)
                            actions.keyUp(0x7A)
                        }
                        "暂停" -> {
                            // F4键
                            actions.keyDown(0x73)
                            actions.keyUp(0x73)
                        }
                        "停止" -> {
                            // F5键
                            actions.keyDown(0x74)
                            actions.keyUp(0x74)
                        }
                        "重启" -> {
                            // F6键
                            actions.keyDown(0x75)
                            actions.keyUp(0x75)
                        }
                        "设置" -> {
                            // Ctrl + , 组合键
                            actions.keyDown(0x11) // Ctrl
                            actions.keyDown(0xBC) // 逗号键
                            actions.keyUp(0xBC)
                            actions.keyUp(0x11)
                        }
                        else -> {
                            // 默认处理 - 空格键
                            actions.keyDown(0x20)
                            actions.keyUp(0x20)
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
fun RemoteFunctionKeyButton(
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
