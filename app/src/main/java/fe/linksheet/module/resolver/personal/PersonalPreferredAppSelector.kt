package fe.linksheet.module.resolver.personal

import app.linksheet.feature.app.core.ActivityAppInfo

object PersonalPreferredAppSelector {
    fun select(
        sorted: List<ActivityAppInfo>,
        filtered: ActivityAppInfo?,
        hasUserAlwaysPreferredApp: Boolean,
        ruleResult: PersonalLinkRuleResult,
    ): PersonalPreferredAppSelection {
        val preferredPackageName = ruleResult.preferredPackageName
        if (hasUserAlwaysPreferredApp || preferredPackageName == null) {
            return PersonalPreferredAppSelection(sorted, filtered, selected = false)
        }

        if (filtered?.packageName == preferredPackageName) {
            return PersonalPreferredAppSelection(sorted, filtered, selected = true)
        }

        val index = sorted.indexOfFirst { it.packageName == preferredPackageName }
        if (index < 0) {
            return PersonalPreferredAppSelection(sorted, filtered, selected = false)
        }

        val selectedApp = sorted[index]
        val remainingApps = sorted.toMutableList()
        remainingApps.removeAt(index)

        return PersonalPreferredAppSelection(
            sorted = remainingApps,
            filtered = selectedApp,
            selected = true
        )
    }
}

data class PersonalPreferredAppSelection(
    val sorted: List<ActivityAppInfo>,
    val filtered: ActivityAppInfo?,
    val selected: Boolean
)
