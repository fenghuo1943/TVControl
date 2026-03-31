package com.fenghuo1943.tvassistant.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun TopSnackbar(
    message: String,
    type: SnackbarType,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    val bgColor = when (type) {
        SnackbarType.SUCCESS -> Color(0xCC4CAF50) // 半透明绿
        SnackbarType.ERROR -> Color(0xCCD32F2F)
        else -> Color(0xCC333333)
    }
    var offsetY by remember { mutableFloatStateOf(0f) }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .offset { IntOffset(0, offsetY.toInt()) }
                .alpha(1f + offsetY / 300f)
                .scale(1f + offsetY / 1000f)
                .pointerInput(Unit){
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            offsetY += dragAmount.y

                            // 只允许向上拖
                            if (offsetY > 0) offsetY = 0f
                        },
                        onDragEnd = {
                            if (offsetY < -100f) {
                                onDismiss() // 👈 触发关闭
                            }
                            offsetY = 0f
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = message,
                    color = Color.White
                )
            }
        }
    }
}