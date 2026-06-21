package fe.linksheet.debug.composable

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.linksheet.compose.DebugMenuButton
import app.linksheet.feature.shizuku.shizukuDebugItem
import fe.composekit.preference.collectAsStateWithLifecycle
import fe.linksheet.activity.onboarding.OnboardingActivity
import fe.linksheet.debug.activity.ComponentStateActivity
import fe.linksheet.debug.activity.ComposableRendererActivity
import fe.linksheet.debug.activity.ExportLogDialogTestActivity
import fe.linksheet.debug.activity.LinkTestingActivity
import fe.linksheet.debug.activity.LocaleDebugActivity
import fe.linksheet.debug.activity.ManifestParserActivity
import fe.linksheet.debug.activity.MetaDataHandlerActivity
import fe.linksheet.debug.activity.SnapTesterActivity
import fe.linksheet.debug.module.viewmodel.DebugViewModel
import fe.linksheet.extension.compose.dashedBorder
import fe.linksheet.navigation.Routes
import kotlin.reflect.KClass

@Composable
fun DebugMenuSlot(viewModel: DebugViewModel, navigate: (String) -> Unit) {
    val activity = LocalActivity.current

    Column(
        modifier = Modifier
            .dashedBorder(1.dp, Color.Gray, 12.dp)
            .padding(all = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            shizukuDebugItem()

            if (activity != null) {
                item(key = "start-service") {
                    DebugMenuButton(
                        text = "启动服务",
                        onClick = {
//                            val intent = Intent(activity, SocketService::class.java)
//                            if (AndroidVersion.isAtLeastApi26O()) {
//                                activity.startForegroundService(intent)
//                            }
                        }
                    )
                }
            }
            if (activity != null) {
                item(key = "metadata-handler") {
                    FilledTonalActivityLauncher(
                        activity = activity,
                        text = "元数据处理器",
                        intent = createIntent(activity, MetaDataHandlerActivity::class)
                    )
                }
                item(key = "manifest-parser") {
                    FilledTonalActivityLauncher(
                        activity = activity,
                        text = "清单解析器",
                        intent = createIntent(activity, ManifestParserActivity::class)
                    )
                }
                item(key = "component-state") {
                    FilledTonalActivityLauncher(
                        activity = activity,
                        text = "组件状态",
                        intent = createIntent(activity, ComponentStateActivity::class)
                    )
                }

                item(key = "locale") {
                    FilledTonalActivityLauncher(
                        activity = activity,
                        text = "语言环境调试",
                        intent = createIntent(activity, LocaleDebugActivity::class)
                    )
                }
            }

            item(key = "draw-borders") {
                val drawBorders by viewModel.drawBorders.collectAsStateWithLifecycle()

                DebugMenuButton(
                    text = "绘制边框 ($drawBorders)",
                    onClick = { viewModel.drawBorders(!drawBorders) }
                )
            }

            item(key = "crash") {
                FilledTonalButton(
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    onClick = {
                        throw Exception("Crash")
                    }
                ) {
                    Text(text = "崩溃测试")
                }
            }

            if (viewModel.debugMiuiCompatProvider != null) {
                item(key = "miui") {
                    var isRequired by remember {
                        mutableStateOf(viewModel.debugMiuiCompatProvider.isRequired.value)
                    }

                    DebugMenuButton(
                        text = "切换 MIUI 兼容 ($isRequired)",
                        onClick = {
                            isRequired = viewModel.toggleMiuiCompatRequired()
                        }
                    )
                }
            }

            item(key = "rules") {
                FilledTonalButton(
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    onClick = { navigate(Routes.RuleOverview) }
                ) {
                    Text(text = "规则")
                }
            }

            if (activity != null) {
                item(key = "onboarding") {
                    FilledTonalActivityLauncher(
                        activity = activity,
                        text = "启动新手引导",
                        intent = createIntent(activity, OnboardingActivity::class)
                    )
                }

                item(key = "export_log_dialog") {
                    FilledTonalActivityLauncher(
                        activity = activity,
                        text = "导出日志对话框测试",
                        intent = createIntent(activity, ExportLogDialogTestActivity::class)
                    )
                }

                item(key = "link_menu") {
                    FilledTonalActivityLauncher(
                        activity = activity,
                        text = "链接测试",
                        intent = createIntent(activity, LinkTestingActivity::class)
                    )
                }

                item(key = "snap_tester") {
                    FilledTonalActivityLauncher(
                        activity = activity,
                        text = "Snap 测试器",
                        intent = createIntent(activity, SnapTesterActivity::class)
                    )
                }

                item(key = "url_preview") {
                    FilledTonalActivityLauncher(
                        activity = activity,
                        text = "URL 预览",
                        intent = createIntent(activity, ComposableRendererActivity::class)
                    )
                }
            }
        }
    }
}

private fun createIntent(activity: Activity, activityClass: KClass<*>): Intent {
    return Intent(activity, activityClass.java)
}

@Composable
private fun FilledTonalActivityLauncher(activity: Activity, text: String, intent: Intent) {
    DebugMenuButton(
        text = text,
        onClick = { activity.startActivity(intent) }
    )
}
