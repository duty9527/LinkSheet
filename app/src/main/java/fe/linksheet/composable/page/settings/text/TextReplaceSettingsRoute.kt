package fe.linksheet.composable.page.settings.text

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.linksheet.compose.page.SaneScaffoldSettingsPage
import fe.linksheet.module.preference.app.TextReplaceRule
import fe.linksheet.module.viewmodel.TextReplaceViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextReplaceSettingsRoute(
    onBackPressed: () -> Unit,
    viewModel: TextReplaceViewModel = koinViewModel()
) {
    val list = remember { mutableStateListOf<TextReplaceRule>() }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        list.clear()
        list.addAll(viewModel.getRules())
    }

    fun save() {
        viewModel.saveRules(list.toList())
    }

    if (showAddDialog) {
        var pattern by remember { mutableStateOf("") }
        var replacement by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = "添加文本替换规则") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pattern,
                        onValueChange = { pattern = it },
                        label = { Text("查找文本 (如 baidu.com)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = replacement,
                        onValueChange = { replacement = it },
                        label = { Text("替换为 (如 google.com)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pattern.isNotEmpty()) {
                            list.add(TextReplaceRule(pattern = pattern, replacement = replacement))
                            save()
                            showAddDialog = false
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
        headline = "前置文本替换",
        onBackPressed = onBackPressed,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(paddingValues = WindowInsets.navigationBars.asPaddingValues()),
                text = { Text(text = "添加规则") },
                icon = { Icon(imageVector = Icons.Outlined.Add, contentDescription = null) },
                onClick = { showAddDialog = true }
            )
        }
    ) {
        if (list.isEmpty()) {
            item(key = "empty_placeholder") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无规则，可在拦截解析链接前进行自定义文本替换",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            group(size = list.size) {
                list.forEachIndexed { index, item ->
                    item(key = item.id) { padding, shape ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(padding),
                            shape = shape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Terminal,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1.0f)
                                        .padding(start = 12.dp)
                                ) {
                                    Text(text = "查找: \"${item.pattern}\"", style = MaterialTheme.typography.titleMedium)
                                    Text(text = "替换为: \"${item.replacement}\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Switch(
                                    checked = item.isEnabled,
                                    onCheckedChange = { checked ->
                                        list[index] = item.copy(isEnabled = checked)
                                        save()
                                    },
                                    modifier = Modifier.padding(end = 8.dp)
                                )

                                IconButton(
                                    onClick = {
                                        list.removeAt(index)
                                        save()
                                    }
                                ) {
                                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "删除")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
