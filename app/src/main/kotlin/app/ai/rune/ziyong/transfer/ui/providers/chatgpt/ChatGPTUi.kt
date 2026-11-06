package app.ai.rune.ziyong.transfer.ui.providers.chatgpt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ai.rune.ziyong.transfer.ui.QuotaPackBar
import app.ai.rune.ziyong.transfer.ui.SectionCard
import app.ai.rune.ziyong.transfer.ui.TextDivider
import app.ai.rune.ziyong.transfer.ui.providers.ProviderUi
import app.ai.rune.ziyong.transfer.ui.providers.ProviderUiActions
import dev.aigw.core.pool.AccountStatus
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** ChatGPT 订阅账号：OAuth 登录，额度来自 Codex 后端。 */
object ChatGPTUi : ProviderUi {

    override val id = "chatgpt"

    override fun description() = "ChatGPT 订阅额度（Codex 后端）"

    @Composable
    override fun Icon(modifier: Modifier) {
        Icon(
            imageVector = MiuixIcons.Regular.Notes,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = modifier,
        )
    }

    @Composable
    override fun LoginEntry(accounts: List<AccountStatus>, actions: ProviderUiActions) {
        SectionCard {
            Button(
                onClick = { actions.onWebLogin(id, "") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text("在浏览器中登录 ChatGPT 账号")
            }
        }
    }

    @Composable
    override fun AccountExtra(status: AccountStatus, actions: ProviderUiActions) {
        val packs = actions.creditPacksOf(id, status.uid)
        // 首次进详情页没有数据时拉一次（打上游；拉取结果回到 state.creditPacks）
        LaunchedEffect(id, status.uid) {
            if (packs.isEmpty()) {
                actions.onLoadCreditPacks(id, status.uid)
            }
        }
        if (packs.isEmpty()) return

        TextDivider()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (pack in packs) {
                QuotaPackBar(pack)
            }
        }
    }
}
