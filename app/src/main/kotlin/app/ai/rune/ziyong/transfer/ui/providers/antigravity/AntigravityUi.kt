package app.ai.rune.ziyong.transfer.ui.providers.antigravity

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
import top.yukonga.miuix.kmp.icon.extended.CloudFill
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Antigravity：Google 账号 OAuth 登录。
 *
 * 额度按「模型组 × 时间窗」拆成多个额度包，因此这里直接平铺渲染全部 quota pack，
 * 不做汇总——汇总会把不同窗口的额度混成一个数，看起来像可用额度比实际多。
 */
object AntigravityUi : ProviderUi {

    override val id = "antigravity"

    override fun description() = "Google 账号 OAuth 登录"

    @Composable
    override fun Icon(modifier: Modifier) {
        Icon(
            imageVector = MiuixIcons.Regular.CloudFill,
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
                Text("在浏览器中登录 Google 账号")
            }
        }
    }

    @Composable
    override fun AccountExtra(status: AccountStatus, actions: ProviderUiActions) {
        val packs = actions.creditPacksOf(id, status.uid)
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
