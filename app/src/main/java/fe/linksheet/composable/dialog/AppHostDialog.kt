package fe.linksheet.composable.dialog

import android.os.Parcelable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.WebAssetOff
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import app.linksheet.compose.theme.DialogTitleStyle
import app.linksheet.feature.app.core.IAppInfo
import fe.android.compose.content.rememberOptionalContent
import fe.android.compose.text.ComposableTextContent.Companion.content
import fe.android.compose.text.DefaultContent.Companion.text
import fe.android.compose.text.StringResourceContent.Companion.textContent
import fe.android.compose.text.TextContentWrapper
import fe.composekit.component.dialog.DialogDefaults
import fe.composekit.component.dialog.SaneAlertDialog
import fe.composekit.component.dialog.SaneAlertDialogTextButton
import fe.composekit.component.list.item.ContentPosition
import fe.composekit.component.list.item.type.CheckboxListItem
import fe.linksheet.R
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.net.IDN
import java.util.Locale
import app.linksheet.compose.R as CommonR

fun Map<String, Boolean>.createResult(selectedStates: Map<String, Boolean>): List<HostState> {
    return (keys + selectedStates.keys).distinct().mapNotNull { host ->
        val initialState = this[host] ?: false
        val currentState = selectedStates[host] ?: false
        HostState(host, initialState, currentState)
    }
}

@Parcelize
data class HostState(
    val host: String,
    val previousState: Boolean,
    val currentState: Boolean,
) : Parcelable


@Parcelize
data class AppHostDialogResult(
    val info: @RawValue IAppInfo,
    val hosts: List<HostState>,
) : Parcelable

@Composable
fun AppHostDialog(
    hosts: List<String>,
    hostState: SnapshotStateMap<String, Boolean>,
    onDismiss: () -> Unit,
    close: () -> Unit,
) {
    val hasHosts = hosts.isNotEmpty()

    val state = rememberLazyListState()
    SaneAlertDialog(
        state = state,
        title = content {
            Text(
                text = stringResource(R.string.app_host_dialog__title_hosts),
                style = DialogTitleStyle
            )
        },
        onDismiss = onDismiss,
        confirmButton = {
            SaneAlertDialogTextButton(
                content = textContent(
                    id = if (hasHosts) R.string.generic__button_text_save
                    else R.string.generic__button_text_close
                ),
                onClick = close
            )
        },
        dismissButton = rememberOptionalContent(hasHosts) {
            SaneAlertDialogTextButton(
                content = textContent(CommonR.string.generic__button_text_cancel),
                onClick = onDismiss
            )
        }
    ) {
        DialogContent(state = state, hosts = hosts, hostState = hostState)
    }
}

@Composable
private fun BoxScope.DialogContent(
    state: LazyListState,
    hosts: List<String>,
    hostState: SnapshotStateMap<String, Boolean>,
) {
    val displayHosts = (hosts + hostState.keys).distinct().sorted()
    Column(modifier = Modifier.matchParentSize()) {
        CustomHostInput(
            hostState = hostState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(DialogDefaults.ContentPadding)
        )

    if (displayHosts.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DialogDefaults.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.WebAssetOff,
                contentDescription = null,
            )
            TextContentWrapper(
                modifier = Modifier.padding(bottom = DialogDefaults.ContentPadding),
                textContent = textContent(R.string.app_host_dialog__text_no_hosts)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = state,
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(items = displayHosts, key = { it }) { host ->
                val isCustom = host !in hosts
                CheckboxListItem(
                    host = host,
                    isChecked = hostState[host]!!,
                    onCheckedChange = {
                        hostState[host] = it
                    },
                    otherContent = rememberOptionalContent(isCustom) {
                        IconButton(onClick = { hostState.remove(host) }) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = stringResource(R.string.generic__button_text_delete)
                            )
                        }
                    }
                )
            }
        }
    }
    }
}

@Composable
private fun CustomHostInput(
    hostState: SnapshotStateMap<String, Boolean>,
    modifier: Modifier = Modifier,
) {
    var input by rememberSaveable { mutableStateOf("") }
    var hasError by rememberSaveable { mutableStateOf(false) }

    fun addHost() {
        val host = normalizeCustomHost(input)
        if (host == null) {
            hasError = input.isNotBlank()
            return
        }

        hostState[host] = true
        input = ""
        hasError = false
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = input,
            onValueChange = {
                input = it
                hasError = false
            },
            singleLine = true,
            label = { Text(text = stringResource(R.string.app_host_dialog__label_custom_host)) },
            supportingText = rememberOptionalContent(hasError) {
                Text(text = stringResource(R.string.app_host_dialog__text_invalid_host))
            },
            isError = hasError,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { addHost() })
        )
        IconButton(onClick = ::addHost) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.add)
            )
        }
    }
}

internal fun normalizeCustomHost(input: String): String? {
    val trimmed = input.trim()
        .removePrefix("http://")
        .removePrefix("https://")
        .substringBefore("/")
        .substringBefore("?")
        .substringBefore("#")
        .substringBefore(":")
        .trim()
        .trim('.')

    if (trimmed.isBlank()) return null

    val asciiHost = try {
        IDN.toASCII(trimmed, IDN.USE_STD3_ASCII_RULES)
    } catch (_: IllegalArgumentException) {
        return null
    }.lowercase(Locale.ROOT)

    if (asciiHost.length > 253) return null
    if (!asciiHost.contains(".")) return null
    if (asciiHost.any { it != '.' && it != '-' && !it.isLetterOrDigit() }) return null
    if (asciiHost.split(".").any { it.isBlank() || it.length > 63 || it.startsWith("-") || it.endsWith("-") }) return null

    return asciiHost
}

@Composable
fun LazyItemScope.CheckboxListItem(
    host: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    otherContent: @Composable (() -> Unit)? = null,
) {
    CheckboxListItem(
        checked = isChecked,
        onCheckedChange = onCheckedChange,
        position = ContentPosition.Leading,
        headlineContent = text(host),
        otherContent = otherContent,
        innerPadding = DialogDefaults.ListItemInnerPadding.copy(
            vertical = 4.dp
        ),
        textOptions = DialogDefaults.ListItemTextOptions,
        colors = DialogDefaults.ListItemColors
    )
}

data class HostStatePreview(
    val hosts: List<String>,
    val states: SnapshotStateMap<String, Boolean>,
)

private class HostStatePreviewProvider() : PreviewParameterProvider<HostStatePreview> {
    override val values: Sequence<HostStatePreview> = sequenceOf(
        HostStatePreview(
            listOf("google.com", "youtube.com", "facebook.com", "linksheet.app", "github.com", "discord.com"),
            mutableStateMapOf(
                "google.com" to false,
                "youtube.com" to true,
                "facebook.com" to false,
                "linksheet.app" to false,
                "github.com" to true,
                "discord.com" to false
            )
        ),
        HostStatePreview(
            listOf("google.com"),
            mutableStateMapOf(
                "google.com" to false,
            )
        ),
        HostStatePreview(
            listOf(),
            mutableStateMapOf(
            )
        ),
    )
}

@Composable
@Preview(showBackground = true)
private fun DialogContentPreview(
    @PreviewParameter(HostStatePreviewProvider::class) hostState: HostStatePreview,
) {
    Box {
        DialogContent(
            state = rememberLazyListState(),
            hosts = hostState.hosts,
            hostState = hostState.states,
        )
    }
}

@Composable
@Preview
private fun AppHostDialogPreview(
    @PreviewParameter(HostStatePreviewProvider::class) hostState: HostStatePreview,
) {
    AppHostDialog(
        hosts = hostState.hosts,
        hostState = hostState.states,
        onDismiss = {

        },
        close = {

        },
    )
}
