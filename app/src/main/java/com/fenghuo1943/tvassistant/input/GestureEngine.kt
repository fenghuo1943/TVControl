package com.fenghuo1943.tvassistant.input

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.positionChange
import com.fenghuo1943.tvassistant.input.GestureConfig.CLICK_THRESHOLD
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.hypot

// ================= 状态 =================

sealed class GestureState {
    object Idle : GestureState()

    data class OneFinger(
        var start: Offset,
        var last: Offset,
        var distance: Float = 0f
    ) : GestureState()

    object Dragging : GestureState()
    object PreDragging : GestureState()
    data class TwoFinger(
        var lastX: Float,
        var lastY: Float,
        var velocityX: Float = 0f,
        var velocityY: Float = 0f,
        var totalDx: Float = 0f,
        var totalDy: Float = 0f,
        var mode: ScrollMode = ScrollMode.NONE,
        var startTime: Long = 0L,
        var lastTime: Long = 0L,
        var moved: Boolean = false
    ) : GestureState()
    data class ThreeFinger(
        var lastX: Float,
        var lastY: Float,
        var startTime: Long = 0L,
        var moved: Boolean = false
    ) : GestureState()

    data class Inertia(
        var velocity: Float,
        //var vy: Float,
        var mode: ScrollMode
    ) : GestureState()
}
object GestureConfig {
    const val CLICK_THRESHOLD = 10f
    const val MOVE_THRESHOLD = 0.5f
    const val LOCK_THRESHOLD = 4f

    const val LONG_PRESS_TIME = 500L
    const val TAP_TIME = 200L

    const val INERTIA_FRICTION = 0.92f
    const val MIN_VELOCITY = 0.3f
}

enum class ScrollMode { NONE, VERTICAL, HORIZONTAL }

// ================= Action 接口 =================

interface MouseActions {
    fun move(dx: Float, dy: Float)
    fun leftClick()
    fun rightClick()
    fun down(button: MouseButton)
    fun up(button: MouseButton)
    fun scroll(dy: Float)
    fun hScroll(dx: Float)
    fun keyDown(vk: Int, modifier: Int = 0)
    fun keyUp(vk: Int, modifier: Int = 0)
    fun sendText(text: String)
}

// ================= 核心引擎 =================

class GestureEngine(
    private val actions: MouseActions,
    private val scope: CoroutineScope,
    private val haptic: HapticFeedback
) {

    var state: GestureState = GestureState.Idle
    private var inertiaJob: Job? = null
    private var longPressJob:Job?=null
    private var gestureGeneration = 0

    fun onEvent(pointers: List<PointerInputChange>) {
        if (pointers.isNotEmpty()) {
            inertiaJob?.cancel()
        }
        val activeCount = pointers.count { it.pressed }
        if (activeCount == 0) {
            resetAll()
            return
        }
        updateState(activeCount, pointers)
        dispatchState(pointers)
    }
    private fun updateState(count: Int, pointers: List<PointerInputChange>) {
        when (state) {
            is GestureState.Idle -> {
                when (count) {
                    1 -> startOneFinger(pointers)
                    2 -> switchToTwoFinger(pointers)
                    3 -> switchToThreeFinger(pointers)
                }
            }
            is GestureState.OneFinger -> {
                if (count >= 2) switchToTwoFinger(pointers)
            }
            is GestureState.TwoFinger -> {
                if (count >= 3) switchToThreeFinger(pointers)
            }
            else -> {}
        }
    }
    private fun dispatchState(pointers: List<PointerInputChange>) {
        when (state) {
            is GestureState.OneFinger -> handleOneFinger(pointers)
            is GestureState.PreDragging -> handlePreDragging(pointers)
            is GestureState.Dragging -> handleDragging(pointers)
            is GestureState.TwoFinger -> handleTwoFinger(pointers)
            is GestureState.ThreeFinger -> handleThreeFinger(pointers)
            is GestureState.Inertia -> handleInertia(pointers)
            else -> {}
        }
    }
    private fun resetAll() {
        when (state) {
            is GestureState.OneFinger -> {
                val s=state as GestureState.OneFinger
                if (s.distance < CLICK_THRESHOLD) {
                    actions.leftClick()
                }
            }
            is GestureState.PreDragging -> {
                actions.up(MouseButton.Left)
            }
            is GestureState.Dragging -> {
                actions.up(MouseButton.Left)
            }
            is GestureState.TwoFinger -> {
                finishTwoFinger()
            }
            is GestureState.ThreeFinger -> {
                finishThreeFinger()
            }
            else -> {}
        }
        stopLongPressJob()
        //inertiaJob?.cancel()
        newGesture()
        state = GestureState.Idle
    }
    private fun newGesture() {
        gestureGeneration++
    }
    // ================= Idle =================
    private fun handleIdle(pointers: List<PointerInputChange>) {
        when (pointers.size) {
            1 -> {
                val p = pointers[0]
                state = GestureState.OneFinger(p.position, p.position)
                startLongPressJob()
            }
        }
    }
    // ================= 单指 =================
    private fun handleOneFinger(pointers: List<PointerInputChange>) {
        if (pointers.isEmpty()) {
            return
        }
        val p = pointers[0]
        val s = state as GestureState.OneFinger
        val delta = p.position - s.last
        s.distance = (p.position - s.start).getDistance()
        if (delta != Offset.Zero) {
            actions.move(accelerateMove(delta.x),
                accelerateMove(delta.y))
        }
        // ================= 一旦移动超过阈值 → 取消长按 =================
        if (s.distance > CLICK_THRESHOLD) {
            stopLongPressJob()
        }
        s.last = p.position
    }
    private fun startOneFinger(pointers: List<PointerInputChange>){
        val p = pointers[0]
        newGesture()
        state = GestureState.OneFinger(p.position, p.position)
        startLongPressJob()
        return
    }



    // ================= 拖拽 =================
    private fun startLongPressJob() {
        longPressJob?.cancel()
        val gen = gestureGeneration
        longPressJob = scope.launch {
            delay(GestureConfig.LONG_PRESS_TIME)
            if (gen != gestureGeneration) return@launch
            if (state !is GestureState.OneFinger) return@launch
            if (state is GestureState.OneFinger) {
                val s = state as GestureState.OneFinger
                val dist = (s.last - s.start).getDistance()
                if (dist < CLICK_THRESHOLD) {
                    state = GestureState.PreDragging
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    actions.down(MouseButton.Left)
                }
            }
        }
    }
    private fun stopLongPressJob() {
        longPressJob?.cancel()
        longPressJob = null
    }
    private fun handlePreDragging(pointers: List<PointerInputChange>) {
        if (pointers.isEmpty()) return
        val p = pointers[0]
        val delta = p.positionChange()
        // ⭐ 只有移动超过阈值，才真正进入 Dragging
        val threshold = GestureConfig.MOVE_THRESHOLD * 1.5f
        if (abs(delta.x) > threshold ||
            abs(delta.y) > threshold
        ) {
            state = GestureState.Dragging
        }
    }
    private fun handleDragging(pointers: List<PointerInputChange>) {
        if (pointers.isEmpty()) {
            return
        }
        val p = pointers[0]
        val delta = p.positionChange()
        actions.move(accelerateMove(delta.x), accelerateMove(delta.y))
    }
    fun accelerateMove(delta: Float): Float {
        return accelerate(
            delta,
            linearGain = 0.8f,
            logGain = 3.0f,
            maxSpeed = 18f
        )
    }
    fun accelerateScroll(delta: Float): Float {
        return accelerate(
            delta,
            linearGain = 0.5f,
            logGain = 2.0f,
            maxSpeed = 30f
        )
    }
    fun accelerate(
        delta: Float,
        linearGain: Float,
        logGain: Float,
        maxSpeed: Float
    ): Float {
        val sign = if (delta > 0) 1 else -1
        val abs = abs(delta)
        if(abs<1f)return sign*abs*0.5f
        val v = abs * linearGain + kotlin.math.ln(abs + 1) * logGain
        //val v = maxSpeed * (1 - kotlin.math.exp(-raw / maxSpeed))
        //Log.d("TV",v.toString())
        return sign * v
    }
    // ================= 双指 =================
    private fun switchToTwoFinger(pointers: List<PointerInputChange>) {
        val avgX = (pointers[0].position.x + pointers[1].position.x) / 2
        val avgY = (pointers[0].position.y + pointers[1].position.y) / 2
        stopLongPressJob()
        inertiaJob?.cancel()
        state = GestureState.TwoFinger(
            lastX = avgX,
            lastY = avgY,
            startTime = System.currentTimeMillis()
        )
    }
    private fun handleTwoFinger(pointers: List<PointerInputChange>) {
        val activeCount = pointers.count { it.pressed }
        if (activeCount < 2) {
            return
        }
        val s = state as GestureState.TwoFinger
        val avgX = (pointers[0].position.x + pointers[1].position.x) / 2
        val avgY = (pointers[0].position.y + pointers[1].position.y) / 2
        val dx = avgX - s.lastX
        val dy = avgY - s.lastY
        if (abs(dx) > GestureConfig.MOVE_THRESHOLD || abs(dy) > GestureConfig.MOVE_THRESHOLD) {
            s.moved = true
        }
        s.totalDx += dx
        s.totalDy += dy
        // ⭐ 方向锁
        if (s.mode == ScrollMode.NONE) {
            if (hypot(dx, dy) > GestureConfig.LOCK_THRESHOLD) {
                s.mode =
                    if (abs(dx) > abs(dy)) ScrollMode.HORIZONTAL
                    else ScrollMode.VERTICAL
            }
        }
        val now = System.currentTimeMillis()
        val dt = (now - s.lastTime).coerceAtLeast(1)
        val alpha = 0.2f
        when (s.mode) {
            ScrollMode.VERTICAL -> {
                actions.scroll(accelerateScroll(dy))
                s.velocityY = s.velocityY * (1 - alpha) + (dy / dt) * alpha
            }
            ScrollMode.HORIZONTAL -> {
                actions.hScroll(accelerateScroll(-dx))
                s.velocityX = s.velocityX * (1 - alpha) + (dx / dt) * alpha
            }

            else -> {}
        }
        s.lastX = avgX
        s.lastY = avgY
        s.lastTime = now
    }

    // ================= 双指结束 =================
    private fun finishTwoFinger() {
        val s = state as GestureState.TwoFinger
        val duration = System.currentTimeMillis() - s.startTime
        val totalDist = hypot(s.totalDx, s.totalDy)
        if (totalDist < CLICK_THRESHOLD && duration < GestureConfig.TAP_TIME) {
            actions.rightClick()
        } else {
            val v=if (abs(s.velocityX) > abs(s.velocityY)) s.velocityX else s.velocityY
            startInertia(v, s.mode)
        }
    }
    // ================= 三指 =================
    private fun handleThreeFinger(pointers: List<PointerInputChange>) {
        val activeCount = pointers.count { it.pressed }
        if (activeCount < 3) {
            return
        }
        val s = state as GestureState.ThreeFinger
        val avgX = pointers.map { it.position.x }.average().toFloat()
        val avgY = pointers.map { it.position.y }.average().toFloat()
        val dx = avgX - s.lastX
        val dy = avgY - s.lastY
        // 判断是否移动
        if (abs(dx) > GestureConfig.MOVE_THRESHOLD || abs(dy) > GestureConfig.MOVE_THRESHOLD) {
            s.moved = true
        }
        // ⭐ 可扩展：三指滑动行为
        // 例如：切窗口 / 切桌面
        if (s.moved) {
            // 示例（你可以改成发快捷键）
            // actions.switchApp(dx, dy)
        }
        s.lastX = avgX
        s.lastY = avgY
    }
    private fun switchToThreeFinger(pointers: List<PointerInputChange>) {
        val avgX = pointers.map { it.position.x }.average().toFloat()
        val avgY = pointers.map { it.position.y }.average().toFloat()
        // ⭐ 停掉所有低级手势
        stopLongPressJob()
        inertiaJob?.cancel()
        state = GestureState.ThreeFinger(
            lastX = avgX,
            lastY = avgY,
            startTime = System.currentTimeMillis()
        )
    }
    private fun finishThreeFinger() {
        val s = state as GestureState.ThreeFinger
        val duration = System.currentTimeMillis() - s.startTime
        // ⭐ 三指点击 → 中键
        if (!s.moved && duration < GestureConfig.TAP_TIME) {
            actions.down(MouseButton.Middle)
            actions.up(MouseButton.Middle)
        }
    }
    // ================= 惯性 =================
    private fun startInertia(v: Float, mode: ScrollMode) {
        inertiaJob?.cancel()
        val gen = gestureGeneration
        val frameTime = 16L
        state = GestureState.Inertia(v, mode)
        inertiaJob = scope.launch {
            val boost = 1.5f
            var velocity = v*boost
            val minVelocity = GestureConfig.MIN_VELOCITY
            while (abs(velocity) > minVelocity) {
                val speed = abs(velocity)

                val friction = 0.85f + 0.15f * (1 - kotlin.math.exp(-speed / 2.2f))
                //if (gen != gestureGeneration) return@launch
                val dt = frameTime / 1000f
                when (mode) {
                    ScrollMode.VERTICAL -> actions.scroll(velocity * dt * 100)
                    ScrollMode.HORIZONTAL -> actions.hScroll(-velocity * dt * 100)
                    else -> {}
                }
                velocity *= friction
                if(speed<1f) velocity*=0.6f
                delay(frameTime)
            }
        }
    }

    private fun handleInertia(pointers: List<PointerInputChange>) {
        // 用户重新触摸 → 打断惯性
        if (pointers.isNotEmpty()) {
            inertiaJob?.cancel()
            state = GestureState.Idle
        }
    }
}