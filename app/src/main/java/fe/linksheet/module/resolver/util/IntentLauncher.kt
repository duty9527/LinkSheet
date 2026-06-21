package fe.linksheet.module.resolver.util

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import app.linksheet.feature.app.core.ActivityAppInfo
import app.linksheet.feature.browser.core.Browser
import app.linksheet.feature.profile.core.CrossProfile
import app.linksheet.lib.flavors.LinkSheetReferrer

interface IntentLauncher {
    fun launch(info: ActivityAppInfo, intent: Intent, referrer: Uri?, browser: Browser?): LaunchIntent
}

class DefaultIntentLauncher(
    private val context: android.content.Context,
    val getComponentEnabledSetting: (ComponentName) -> Int,
) : IntentLauncher {

    override fun launch(info: ActivityAppInfo, intent: Intent, referrer: Uri?, browser: Browser?): LaunchIntent {
        if (isComponentDisabled(info)) {
            return LaunchMainIntent(createMainIntent(intent, info.packageName))
        }

        browser?.requestPrivateBrowsing(intent)

        val resolveIntent = Intent(intent.action, intent.data).apply {
            `package` = info.packageName
        }
        val resolvedActivities = try {
            context.packageManager.queryIntentActivities(resolveIntent, 0)
        } catch (e: Exception) {
            emptyList()
        }

        if (resolvedActivities.isNotEmpty()) {
            intent.setPackage(info.packageName)
        } else {
            intent.component = info.componentName
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        intent.putExtra(
            LinkSheetReferrer.EXTRA_REFERRER,
            referrer
        )
        intent.putExtra(Intent.EXTRA_REFERRER, referrer)

        return LaunchViewIntent(intent)
    }

    private fun isComponentDisabled(info: ActivityAppInfo): Boolean {
        val status = getComponentEnabledSetting(info.componentName)
        return status == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    private fun createMainIntent(intent: Intent, packageName: String): Intent {
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        mainIntent.selector = intent
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setPackage(packageName)

        return mainIntent
    }
}

sealed interface Launchable {

}
sealed class LaunchIntent(val intent: Intent) : Launchable {

}

class LaunchOtherProfileIntent(val profile: CrossProfile, val url: String) : Launchable
class LaunchRawIntent(intent: Intent) : LaunchIntent(intent)
class LaunchMainIntent(intent: Intent) : LaunchIntent(intent)
class LaunchViewIntent(intent: Intent) : LaunchIntent(intent)
