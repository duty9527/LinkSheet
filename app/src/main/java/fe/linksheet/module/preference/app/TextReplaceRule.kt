package fe.linksheet.module.preference.app

import java.util.UUID

data class TextReplaceRule(
    val id: String = UUID.randomUUID().toString(),
    val pattern: String,
    val replacement: String,
    val isEnabled: Boolean = true
)
