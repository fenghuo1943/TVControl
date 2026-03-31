package com.fenghuo1943.tvcontrol.ui.components

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

object SnackbarManager {
    private val channel = Channel<SnackbarEvent>(Channel.UNLIMITED)
    private val _events = MutableSharedFlow<SnackbarEvent>()
    val events = channel.receiveAsFlow()
    //val events = _events.asSharedFlow()

    fun show(
        message: String,
        type: SnackbarType = SnackbarType.INFO,
        actionLabel: String? = null
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            channel.send(SnackbarEvent(message, type, actionLabel))
            //_events.emit(SnackbarEvent(message, type, actionLabel))
        }
    }
}
enum class SnackbarType {
    INFO,
    SUCCESS,
    ERROR
}
data class SnackbarEvent(
    val message: String,
    val type: SnackbarType = SnackbarType.INFO,
    val actionLabel: String? = null
)