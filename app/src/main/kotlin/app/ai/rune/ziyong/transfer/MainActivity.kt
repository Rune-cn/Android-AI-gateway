package app.ai.rune.ziyong.transfer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.ai.rune.ziyong.transfer.service.GatewayTileService
import app.ai.rune.ziyong.transfer.ui.AppRoot
import app.ai.rune.ziyong.transfer.ui.AppViewModel
import app.ai.rune.ziyong.transfer.ui.theme.RTransferTheme

/**
 * 唯一的 Activity：承载 Compose 界面。
 *
 * 只做三件事：注册磁贴广播、构造 ViewModel、把主题与根容器挂起来。
 * 具体页面与导航都在 `ui/` 里，Activity 不参与业务决策——
 * 否则改一个页面顺带要改 Activity，测试也无从下手。
 */
class MainActivity : ComponentActivity() {

    private var viewModel: AppViewModel? = null

    private val gatewayToggledReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // 磁贴启停后通知界面刷新（磁贴无法直接触达 ViewModel）
            viewModel?.refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        registerGatewayToggledReceiver()
        setContent {
            val vm: AppViewModel = viewModel(factory = appViewModelFactory())
            viewModel = vm
            val state by vm.state.collectAsStateWithLifecycle()

            RTransferTheme(
                useDynamicColor = state.dynamicColor,
                themeMode = state.themeMode,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surface),
                ) {
                    AppRoot(vm, state)
                }
            }
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(gatewayToggledReceiver) }
        viewModel = null
        super.onDestroy()
    }

    private fun registerGatewayToggledReceiver() {
        val filter = IntentFilter(GatewayTileService.ACTION_GATEWAY_TOGGLED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(gatewayToggledReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(gatewayToggledReceiver, filter)
        }
    }

    private fun appViewModelFactory() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AppViewModel(application) as T
    }
}
