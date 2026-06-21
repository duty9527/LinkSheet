package fe.linksheet.composable.page.settings.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Domain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.linksheet.compose.page.SaneScaffoldSettingsPage
import fe.android.compose.icon.iconPainter
import fe.android.compose.text.DefaultContent.Companion.text
import fe.composekit.component.list.column.shape.ClickableShapeListItem
import fe.composekit.route.Route
import fe.linksheet.module.viewmodel.AppConfigViewModel
import fe.linksheet.navigation.AppConfigRoute
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigOverviewRoute(
    onBackPressed: () -> Unit,
    navigate: (Route) -> Unit,
    viewModel: AppConfigViewModel = koinViewModel()
) {
    val hosts by viewModel.allHosts.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        var inputHost by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = "添加域名规则") },
            text = {
                OutlinedTextField(
                    value = inputHost,
                    onValueChange = { inputHost = it.trim().lowercase(java.util.Locale.getDefault()) },
                    label = { Text("输入域名 (如 github.com)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputHost.isNotEmpty()) {
                            showAddDialog = false
                            navigate(AppConfigRoute(inputHost))
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    SaneScaffoldSettingsPage(
        headline = "域名分流配置",
        onBackPressed = onBackPressed,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(paddingValues = WindowInsets.navigationBars.asPaddingValues()),
                text = { Text(text = "添加域名") },
                icon = { Icon(imageVector = Icons.Outlined.Add, contentDescription = null) },
                onClick = { showAddDialog = true }
            )
        }
    ) {
        if (hosts.isEmpty()) {
            item(key = "empty_placeholder") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无配置，请点击右下角按钮添加域名规则",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            group(size = hosts.size) {
                hosts.forEachIndexed { index, host ->
                    item(key = host) { padding, shape ->
                        ClickableShapeListItem(
                            shape = shape,
                            padding = padding,
                            headlineContent = text(host),
                            supportingContent = text("管理该域名的打开优先级行为"),
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Outlined.Domain,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { viewModel.deleteHost(host) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "删除"
                                    )
                                }
                            },
                            onClick = { navigate(AppConfigRoute(host)) }
                        )
                    }
                }
            }
        }
    }
}
