package fe.linksheet.module.resolver.personal

import android.net.Uri

class PersonalLinkRuleEngine(
    private val rules: List<PersonalLinkRule> = defaultRules
) {
    fun apply(uri: Uri): PersonalLinkRuleResult {
        var currentUri = uri
        var preferredPackageName: String? = null
        var autoLaunch = false

        rules.forEach { rule ->
            val result = rule.apply(currentUri) ?: return@forEach
            currentUri = result.uri
            preferredPackageName = result.preferredPackageName ?: preferredPackageName
            autoLaunch = autoLaunch || result.autoLaunch
        }

        return PersonalLinkRuleResult(
            uri = currentUri,
            preferredPackageName = preferredPackageName,
            autoLaunch = autoLaunch
        )
    }

    companion object {
        const val GithubPackageName = "com.github.android"
        const val YoutubePackageName = "com.google.android.youtube"

        private val defaultRules = listOf(
            TrackingParameterRule(
                names = setOf(
                    "utm_source",
                    "utm_medium",
                    "utm_campaign",
                    "utm_term",
                    "utm_content",
                    "utm_id",
                    "gclid",
                    "fbclid",
                    "msclkid",
                    "mc_cid",
                    "mc_eid",
                    "igshid"
                )
            ),
            HostPreferredAppRule(
                hosts = setOf("github.com", "gist.github.com"),
                packageName = GithubPackageName,
                autoLaunch = true
            ),
            HostPreferredAppRule(
                hosts = setOf("youtube.com", "m.youtube.com", "music.youtube.com", "youtu.be"),
                packageName = YoutubePackageName,
                autoLaunch = true
            )
        )
    }
}

data class PersonalLinkRuleResult(
    val uri: Uri,
    val preferredPackageName: String? = null,
    val autoLaunch: Boolean = false
) {
    fun merge(next: PersonalLinkRuleResult): PersonalLinkRuleResult {
        return PersonalLinkRuleResult(
            uri = next.uri,
            preferredPackageName = next.preferredPackageName ?: preferredPackageName,
            autoLaunch = autoLaunch || next.autoLaunch
        )
    }
}

fun interface PersonalLinkRule {
    fun apply(uri: Uri): PersonalLinkRuleResult?
}

private class HostPreferredAppRule(
    hosts: Set<String>,
    private val packageName: String,
    private val autoLaunch: Boolean
) : PersonalLinkRule {
    private val normalizedHosts = hosts.mapTo(mutableSetOf()) { it.normalizedHost() }

    override fun apply(uri: Uri): PersonalLinkRuleResult? {
        val host = uri.host?.normalizedHost() ?: return null
        if (host !in normalizedHosts) return null

        return PersonalLinkRuleResult(
            uri = uri,
            preferredPackageName = packageName,
            autoLaunch = autoLaunch
        )
    }
}

private class TrackingParameterRule(
    names: Set<String>
) : PersonalLinkRule {
    private val blockedNames = names.mapTo(mutableSetOf()) { it.lowercase() }

    override fun apply(uri: Uri): PersonalLinkRuleResult? {
        if (!uri.isHttpUrl()) return null

        val queryNames = uri.queryParameterNames
        if (queryNames.none { it.lowercase() in blockedNames }) return null

        val builder = uri.buildUpon().clearQuery()
        queryNames.forEach { name ->
            if (name.lowercase() in blockedNames) return@forEach
            uri.getQueryParameters(name).forEach { value ->
                builder.appendQueryParameter(name, value)
            }
        }

        return PersonalLinkRuleResult(uri = builder.build())
    }
}

private fun Uri.isHttpUrl(): Boolean {
    val scheme = scheme?.lowercase() ?: return false
    return scheme == "http" || scheme == "https"
}

private fun String.normalizedHost(): String {
    return lowercase().removePrefix("www.")
}
