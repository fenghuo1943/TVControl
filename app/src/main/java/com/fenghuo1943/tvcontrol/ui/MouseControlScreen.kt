package com.fenghuo1943.tvcontrol.ui

import android.util.Log
import android.view.ViewTreeObserver
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.input.key.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import com.fenghuo1943.tvcontrol.MainViewModel
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fenghuo1943.tvcontrol.input.GestureEngine
import com.fenghuo1943.tvcontrol.input.InputController
import com.fenghuo1943.tvcontrol.input.MouseActions
import com.fenghuo1943.tvcontrol.protocol.MouseActionsImpl
import com.fenghuo1943.tvcontrol.input.MouseButton
import com.fenghuo1943.tvcontrol.ui.common.StatusBarStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MouseControlScreen(
    modifier: Modifier = Modifier,
    vm: MainViewModel = hiltViewModel()

) {

    val actions= remember(vm) { MouseActionsImpl(vm) }
    var showCustomKeyboard by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val handler = remember { InputController(vm.inputSender, scope) }
    // 获取系统键盘高度
    val view = LocalView.current
    val density = LocalDensity.current
    var keyboardHeight by remember { mutableStateOf(250.dp) } // 默认值

    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            view.getWindowVisibleDisplayFrame(rect)
            val screenHeight = view.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) {
                // 如果高度超过屏幕的15%，认为是键盘
                keyboardHeight = with(density) { keypadHeight.toDp() }
            }
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    StatusBarStyle(isLight = true)
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color(0xFFF0F0F0))
    ) {
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
        
        var requestKeyboard by remember { mutableStateOf(false) }
        val keyboardController = LocalSoftwareKeyboardController.current
        KeyboardControlBar(
            onCustomKeyboard = { 
                // 关闭系统键盘，显示自定义键盘
                keyboardController?.hide()
                requestKeyboard = false
                showCustomKeyboard = true
            },
            onSystemKeyboard = { 
                // 关闭自定义键盘，显示系统键盘
                showCustomKeyboard = false
                scope.launch {
                    delay(1)
                    requestKeyboard = true
                }
            }
        )
            // 🎮 自定义键盘
            if (showCustomKeyboard) {
                Spacer(modifier = Modifier.height(8.dp))
                CustomKeyboard(actions = actions, keyboardHeight = keyboardHeight)
            }
        }
        
        // ⌨️ 系统键盘输入（隐藏TextField，放在Box底部，不影响上方布局）
        SystemKeyboardInput(
            text = textInput,
            requestFocus = requestKeyboard,
            onTextChange = { run { textInput = it } },
            onEvent = { handler.handle(it)},
            onFocusHandled = { requestKeyboard = false}
        )
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MouseButtonItem(
            text="左键",
            pressed = MouseButton.Left in pressedButtons,
            onDown = {pressedButtons = pressedButtons + MouseButton.Left
            actions.down((MouseButton.Left))
        },
            onUp = {
                pressedButtons = pressedButtons - MouseButton.Left
                actions.up(MouseButton.Left)
            })
        MouseButtonItem(text = "中键",
            pressed = MouseButton.Middle in pressedButtons,
            onDown = {
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
    onSystemKeyboard: () -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 🎮 自定义键盘按钮
        Button(
            onClick = onCustomKeyboard,
            modifier = Modifier.weight(1f)
        ) {
            Text("快捷键键盘")
        }
        Spacer(modifier = Modifier.width(8.dp))
        // ⌨️ 系统键盘按钮
        Button(
            onClick = onSystemKeyboard,
            modifier = Modifier.weight(1f)
        ) {
            Text("系统键盘")
        }
    }
}

sealed class KeyboardEvent {
    data class KeyDown(val keyCode: Int) : KeyboardEvent()
    data class KeyUp(val keyCode: Int) : KeyboardEvent()

    data class TextInput(val text: String) : KeyboardEvent()
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
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            delay(100)
            focusRequester.requestFocus()
            keyboardController?.show()
            onFocusHandled()
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
