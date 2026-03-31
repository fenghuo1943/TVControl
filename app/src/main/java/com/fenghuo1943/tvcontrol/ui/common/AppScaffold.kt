package com.fenghuo1943.tvcontrol.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.fenghuo1943.tvcontrol.ui.components.SnackbarEvent
import com.fenghuo1943.tvcontrol.ui.components.SnackbarManager
import com.fenghuo1943.tvcontrol.ui.components.TopSnackbar

@Composable
fun AppScaffold(
    content: @Composable (Modifier) -> Unit
) {

    val snackbarHostState = remember { SnackbarHostState() }
    var currentEvent by remember { mutableStateOf<SnackbarEvent?>(null) }
    var visible by remember { mutableStateOf(false) }

    // 🔥 全局监听 Snackbar
    LaunchedEffect(Unit) {
        SnackbarManager.events.collect { event ->
            currentEvent = event
            visible = true

            // 自动消失
            kotlinx.coroutines.delay(2000)
            visible = false
            kotlinx.coroutines.delay(300)
//            val result = snackbarHostState.showSnackbar(
//                message = event.message,
//                actionLabel = event.actionLabel,
//                duration = SnackbarDuration.Short
//            )
//            if (result == SnackbarResult.ActionPerformed) {
//                // 👉 可扩展：处理点击事件（比如重试）
//            }
        }
    }
    Box(modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()) {
        // 👇 原页面
        content(Modifier)
        // 👇 顶部浮层 Snackbar
        currentEvent?.let {
            TopSnackbar(
                message = it.message,
                type = it.type,
                visible = visible,
                onDismiss = { visible = false } // 👈 手势关闭
            )
        }
    }
//    Scaffold(
//        snackbarHost = {
//            SnackbarHost(
//                hostState = snackbarHostState,
//                snackbar = { data ->
//                    // 🎨 根据类型设置颜色
//                    val containerColor = when (currentEvent?.type) {
//                        SnackbarType.SUCCESS -> Color(0xFF4CAF50)
//                        SnackbarType.ERROR -> Color(0xFFD32F2F)
//                        else -> Color(0xFF323232)
//                    }
//
//                    Snackbar(
//                        containerColor = containerColor,
//                        contentColor = Color.White,
//                        shape = RoundedCornerShape(12.dp),
//                        action = {
//                            data.visuals.actionLabel?.let { label ->
//                                TextButton(onClick = { data.performAction() }) {
//                                    Text(label, color = Color.White)
//                                }
//                            }
//                        }
//                    ) {
//                        Text(data.visuals.message)
//                    }
//                }
//            )
//        }
//    ) { padding ->
//
//        content(Modifier.padding(padding))
//    }
}