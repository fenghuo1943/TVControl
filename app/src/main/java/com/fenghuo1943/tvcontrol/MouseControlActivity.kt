package com.fenghuo1943.tvcontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.fenghuo1943.tvcontrol.ui.MouseControlScreen
import com.fenghuo1943.tvcontrol.ui.common.AppScaffold
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MouseControlActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppScaffold {
                MouseControlScreen(
                )
            }
        }
    }
}