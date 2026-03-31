package com.fenghuo1943.tvassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.fenghuo1943.tvassistant.ui.MouseControlScreen
import com.fenghuo1943.tvassistant.ui.common.AppScaffold
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