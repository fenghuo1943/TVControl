package com.fenghuo1943.tvassistant

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.fenghuo1943.tvassistant.network.DiscoveryService
import com.fenghuo1943.tvassistant.ui.MainScreen
import com.fenghuo1943.tvassistant.ui.common.AppScaffold
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.toastEvent = { msg ->
            runOnUiThread{
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }

        }
        val discovery = DiscoveryService(this)
        vm.updateDiscovery(discovery)
        setContent {
            AppScaffold {
                MainScreen(
                    onIpChange = { vm.ip = it },
                )
            }
        }
        vm.networkInit()
        enableEdgeToEdge()
        Log.d("TV", "onCreate")
    }
}
