package fe.linksheet.composable.page.settings.app

import android.content.pm.getInstalledPackagesCompat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.linksheet.compose.appbar.SaneLargeTopAppBar
import app.linksheet.compose.extension.toImageBitmap
import fe.android.compose.extension.atElevation
import fe.android.compose.feedback.FeedbackType
import fe.android.compose.feedback.LocalHapticFeedbackInteraction
import fe.android.compose.icon.BitmapIconPainter
import fe.android.compose.icon.iconPainter
import fe.android.compose.text.ComposableTextContent.Companion.content
import fe.android.compose.text.DefaultContent.Companion.text
import fe.android.compose.text.TextContent
import fe.composekit.component.card.AlertCardContentLayout
import fe.composekit.component.card.AlertCardDefaults
import fe.composekit.component.icon.AppIconImage
import fe.composekit.component.icon.FilledIcon
import fe.composekit.component.list.column.SaneLazyColumnLayout
import fe.composekit.component.page.SaneSettingsScaffold
import fe.composekit.component.shape.CustomShapeDefaults
import fe.composekit.layout.column.SaneLazyListScope
import fe.linksheet.R
import fe.linksheet.extension.android.isUserApp
import fe.linksheet.module.database.entity.HostBehaviorItem
import fe.linksheet.module.viewmodel.AppConfigViewModel
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppConfigPage(
    host: String,
    onBackPressed: () -> Unit,
    viewModel: AppConfigViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val interaction = LocalHapticFeedbackInteraction.current

    val behaviorItems by viewModel.getByHost(host).collectAsStateWithLifecycle(initialValue = null)
    val list = remember { mutableStateListOf<Item>() }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(behaviorItems) {
        val items = behaviorItems
        if (items != null && !isInitialized) {
            if (items.isNotEmpty()) {
                list.clear()
                items.forEach { dbItem ->
                    val uiItem = when (dbItem.type) {
                        HostBehaviorItem.TYPE_NATIVE_APPS -> NativeAppsItem
                        HostBehaviorItem.TYPE_PREFERRED_BROWSER -> PreferredBrowserItem
                        HostBehaviorItem.TYPE_BROWSERS -> BrowsersItem
                        HostBehaviorItem.TYPE_SPECIFIC_APP -> {
                            val appInfo = try {
                                pm.getApplicationInfo(dbItem.packageName!!, 0)
                            } catch (e: Exception) {
                                null
                            }
                            if (appInfo != null) {
                                AppItem(
                                    label = appInfo.loadLabel(pm),
                                    packageName = dbItem.packageName!!,
                                    icon = appInfo.loadIcon(pm).toImageBitmap()
                                )
                            } else {
                                AppItem(
                                    label = dbItem.packageName!!,
                                    packageName = dbItem.packageName!!,
                                    icon = null
                                )
                            }
                        }
                        else -> null
                    }
                    if (uiItem != null) {
                        list.add(uiItem)
                    }
                }
                isInitialized = true
            } else {
                // 初始化默认链：1. 原生分流 2. 首选浏览器
                list.clear()
                list.add(NativeAppsItem)
                list.add(PreferredBrowserItem)
                isInitialized = true

                val dbItems = list.mapIndexed { idx, item ->
                    HostBehaviorItem(
                        host = host,
                        type = item.type,
                        packageName = item.packageName,
                        componentName = item.componentName,
                        position = idx
                    )
                }
                viewModel.saveBehavior(host, dbItems)
            }
        }
    }

    fun saveCurrentList() {
        val dbItems = list.mapIndexed { idx, item ->
            HostBehaviorItem(
                host = host,
                type = item.type,
                packageName = item.packageName,
                componentName = item.componentName,
                position = idx
            )
        }
        viewModel.saveBehavior(host, dbItems)
    }

    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val installedApps = remember {
            pm.getInstalledPackagesCompat()
                .filter { it.applicationInfo?.isUserApp == true }
                .map { it.applicationInfo!! }
                .sortedBy { it.loadLabel(pm).toString().lowercase() }
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = "添加分流节点") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("搜索动作或应用") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1.0f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 1. 系统预设项 (如果当前规则中没有才显示)
                        val hasNative = list.any { it.type == HostBehaviorItem.TYPE_NATIVE_APPS }
                        val hasPrefBrowser = list.any { it.type == HostBehaviorItem.TYPE_PREFERRED_BROWSER }
                        val hasBrowsers = list.any { it.type == HostBehaviorItem.TYPE_BROWSERS }

                        if (!hasNative && "原生应用分流".contains(searchQuery, ignoreCase = true)) {
                            item {
                                AddDialogItem(
                                    title = "原生应用分流",
                                    subtitle = "如有匹配的原生应用，优先使用其打开",
                                    icon = Icons.Outlined.Apps,
                                    onClick = {
                                        list.add(NativeAppsItem)
                                        saveCurrentList()
                                        showAddDialog = false
                                    }
                                )
                            }
                        }

                        if (!hasPrefBrowser && "全局首选浏览器".contains(searchQuery, ignoreCase = true)) {
                            item {
                                AddDialogItem(
                                    title = "全局首选浏览器",
                                    subtitle = "直接在全局首选浏览器中打开链接",
                                    icon = Icons.Outlined.Public,
                                    onClick = {
                                        list.add(PreferredBrowserItem)
                                        saveCurrentList()
                                        showAddDialog = false
                                    }
                                )
                            }
                        }

                        if (!hasBrowsers && "浏览器选择器".contains(searchQuery, ignoreCase = true)) {
                            item {
                                AddDialogItem(
                                    title = "浏览器选择器",
                                    subtitle = "弹出浏览器列表供您自由选择",
                                    icon = Icons.Outlined.OpenInBrowser,
                                    onClick = {
                                        list.add(BrowsersItem)
                                        saveCurrentList()
                                        showAddDialog = false
                                    }
                                )
                            }
                        }

                        // 2. 应用程序项
                        val filteredApps = installedApps.filter {
                            val label = it.loadLabel(pm).toString()
                            label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
                        }

                        items(filteredApps) { appInfo ->
                            val packageName = appInfo.packageName
                            val isAlreadyAdded = list.any { it.packageName == packageName }

                            AddDialogAppItem(
                                label = appInfo.loadLabel(pm).toString(),
                                packageName = packageName,
                                icon = appInfo.loadIcon(pm).toImageBitmap(),
                                isAdded = isAlreadyAdded,
                                onClick = {
                                    if (!isAlreadyAdded) {
                                        list.add(
                                            AppItem(
                                                label = appInfo.loadLabel(pm),
                                                packageName = packageName,
                                                icon = appInfo.loadIcon(pm).toImageBitmap()
                                            )
                                        )
                                        saveCurrentList()
                                        showAddDialog = false
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    var selectedIdx by remember { mutableIntStateOf(-1) }
    var pinnedCount by remember { mutableIntStateOf(0) }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        selectedIdx = -1
        list.add(to.index, list.removeAt(from.index))
        saveCurrentList()
        interaction.perform(FeedbackType.SegmentFrequentTick)
    }

    SaneScaffoldSettingsPage3(
        state = lazyListState,
        headline = "分流配置: $host",
        onBackPressed = onBackPressed,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(paddingValues = WindowInsets.navigationBars.asPaddingValues()),
                text = { Text(text = "添加分流动作") },
                icon = { Icon(imageVector = Icons.Outlined.Add, contentDescription = null) },
                onClick = { showAddDialog = true }
            )
        }
    ) {
        itemsIndexed(items = list, key = { _, item -> item.hashCode() }) { index, item ->
            var isPinned by remember { mutableStateOf(index < pinnedCount) }

            ReorderableItem(
                modifier = Modifier.fillMaxWidth(),
                state = reorderableLazyListState,
                key = item.hashCode(),
                enabled = !isPinned
            ) {
                ItemCard3(
                    item = item,
                    isSelected = selectedIdx == index,
                    onLongClick = {
                        selectedIdx = if (selectedIdx == index) -1 else index
                    },
                    onRemoveRequested = {
                        selectedIdx = -1
                        if (isPinned) {
                            pinnedCount--
                        }
                        list.removeAt(index)
                        saveCurrentList()
                    },
                    isPinned = isPinned,
                    onPinRequested = {
                        selectedIdx = -1
                        if (isPinned) {
                            list.add(--pinnedCount, list.removeAt(index))
                        } else {
                            list.add(pinnedCount++, list.removeAt(index))
                        }
                        isPinned = !isPinned
                        saveCurrentList()
                    }
                )
            }
        }
    }
}

@Composable
private fun AddDialogItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIcon(
                icon = icon.iconPainter,
                iconSize = 20.dp,
                containerSize = 34.dp,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AddDialogAppItem(
    label: String,
    packageName: String,
    icon: ImageBitmap,
    isAdded: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = if (isAdded) ({}) else onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAdded) MaterialTheme.colorScheme.surfaceDim else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconImage(icon = BitmapIconPainter.bitmap(icon), label = label)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isAdded) {
                Text(
                    text = "已添加",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ReorderableCollectionItemScope.ItemCard3(
    modifier: Modifier = AlertCardDefaults.MinHeight,
    innerPadding: PaddingValues = AlertCardDefaults.InnerPadding,
    horizontalArrangement: Arrangement.Horizontal = AlertCardDefaults.HorizontalArrangement,
    item: Item,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onRemoveRequested: () -> Unit,
    isPinned: Boolean,
    onPinRequested: () -> Unit,
) {
    val interaction = LocalHapticFeedbackInteraction.current
    val interactionSource = remember { MutableInteractionSource() }

    val cardContainerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHighest

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CustomShapeDefaults.SingleShape)
            .combinedClickable(onClick = {}, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(modifier)
                .padding(innerPadding),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item is AppItem) {
                if (item.icon != null) {
                    AppIconImage(icon = BitmapIconPainter.bitmap(item.icon), label = item.label.toString())
                } else {
                    FilledIcon(
                        icon = Icons.Outlined.Apps.iconPainter,
                        iconSize = 20.dp,
                        containerSize = 34.dp,
                        contentDescription = null
                    )
                }
            } else if (item is IconItem) {
                val containerColor = cardContainerColor.atElevation(
                    MaterialTheme.colorScheme.surfaceTint, 6.dp
                )
                FilledIcon(
                    icon = item.icon.iconPainter,
                    iconSize = 20.dp,
                    containerSize = 34.dp,
                    contentDescription = null,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = containerColor,
                        contentColor = contentColorFor(backgroundColor = containerColor)
                    )
                )
            }

            AlertCardContentLayout(
                modifier = Modifier.weight(1.0f).padding(start = 12.dp),
                title = {
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.titleMedium,
                        content = item.title.content
                    )
                },
                subtitle = {
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                        content = item.description.content
                    )
                }
            )

            if (isSelected) {
                Row(modifier = Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                        IconButton(
                            onClick = onRemoveRequested,
                        ) {
                            Icon(imageVector = Icons.Rounded.DeleteOutline, contentDescription = null)
                        }

                        IconButton(onClick = onPinRequested) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = ImageVector.vectorResource(id = if (isPinned) R.drawable.keep_filled_24px else R.drawable.keep_24px),
                                contentDescription = null
                            )
                        }
                    }
                }
            } else if (isPinned) {
                IconButton(onClick = onPinRequested) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = ImageVector.vectorResource(id = R.drawable.keep_filled_24px),
                        contentDescription = null
                    )
                }
            } else {
                IconButton(
                    modifier = Modifier.draggableHandle(
                        onDragStarted = { interaction.perform(FeedbackType.DragStart) },
                        onDragStopped = { interaction.perform(FeedbackType.GestureEnd) },
                        interactionSource = interactionSource,
                    ),
                    onClick = {},
                ) {
                    Icon(imageVector = Icons.Rounded.DragHandle, contentDescription = null)
                }
            }
        }
    }
}

private object NativeAppsItem : IconItem(
    title = text("原生应用分流"),
    description = text("如有匹配的原生应用，优先使用其打开"),
    icon = Icons.Outlined.Apps,
    type = HostBehaviorItem.TYPE_NATIVE_APPS
)

private object BrowsersItem : IconItem(
    title = text("浏览器选择器"),
    description = text("弹出浏览器列表供您选择"),
    icon = Icons.Outlined.OpenInBrowser,
    type = HostBehaviorItem.TYPE_BROWSERS
)

private object PreferredBrowserItem : IconItem(
    title = text("全局首选浏览器"),
    description = text("直接在全局首选浏览器中打开"),
    icon = Icons.Outlined.Public,
    type = HostBehaviorItem.TYPE_PREFERRED_BROWSER
)

private class AppItem(val label: CharSequence, packageName: String, val icon: ImageBitmap?) : Item(
    title = content {
        Text(text = label.toString(), overflow = TextOverflow.Ellipsis, maxLines = 1)
    },
    description = content {
        Text(text = packageName, overflow = TextOverflow.Ellipsis, maxLines = 1)
    },
    type = HostBehaviorItem.TYPE_SPECIFIC_APP,
    packageName = packageName
)

private open class IconItem(
    title: TextContent, description: TextContent,
    val icon: ImageVector,
    type: Int
) : Item(title, description, type)

private open class Item(
    val title: TextContent,
    val description: TextContent,
    val type: Int,
    val packageName: String? = null,
    val componentName: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaneScaffoldSettingsPage3(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    headline: String,
    onBackPressed: () -> Unit,
    enableBackButton: Boolean = true,
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    content: SaneLazyListScope.() -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true }
    )

    SaneSettingsScaffold(
        modifier = modifier.then(Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)),
        topBar = {
            SaneLargeTopAppBar(
                headline = headline,
                enableBackButton = enableBackButton,
                onBackPressed = onBackPressed,
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        content = { padding ->
            SaneLazyColumnLayout(
                state = state,
                padding = padding,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                content = content
            )
        }
    )
}
