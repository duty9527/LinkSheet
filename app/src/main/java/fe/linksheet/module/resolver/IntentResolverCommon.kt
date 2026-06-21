package fe.linksheet.module.resolver

import app.linksheet.feature.downloader.core.DownloaderMode
import fe.linksheet.module.resolver.browser.BrowserMode
import fe.linksheet.module.resolver.module.BrowserSettings

object IntentResolverCommon {
    fun shouldRunDownloader(
        enabled: Boolean,
        mode: DownloaderMode,
        isRegularPreferredApp: Boolean,
        hasManualFlag: Boolean
    ): Boolean {
        if (!enabled) return false
        if (isRegularPreferredApp) return false
        return when (mode) {
            is DownloaderMode.Auto -> true
            is DownloaderMode.Manual -> hasManualFlag
        }
    }

    fun shouldRunPreviewUrl(
        enabled: Boolean,
        previewUrlSkipBrowser: Boolean,
        isReferrerBrowser: Boolean,
        isRegularPreferredApp: Boolean
    ): Boolean {
        if (!enabled) return false
        if (previewUrlSkipBrowser && isReferrerBrowser) return false

        return !isRegularPreferredApp
    }

    fun shouldFollowRedirects(
        enabled: Boolean,
        mode: FollowRedirectsMode,
        skipBrowser: Boolean,
        isReferrerBrowser: Boolean,
        hasManualFlag: Boolean
    ): Boolean {
        if (!enabled) return false
        return when (mode) {
            is FollowRedirectsMode.Auto -> !(skipBrowser && isReferrerBrowser)
            is FollowRedirectsMode.Manual -> hasManualFlag
        }
    }

    suspend fun createBrowserModeConfig(
        browserSettings: BrowserSettings,
        customTab: Boolean
    ): BrowserModeConfigHelper {
        val useInAppSettings = !browserSettings.unifiedPreferredBrowser() && customTab
        val mode = when {
            useInAppSettings -> browserSettings.inAppBrowserMode()
            else -> browserSettings.browserMode()
        }

        return when (mode) {
            BrowserMode.AlwaysAsk -> BrowserModeConfigHelper.AlwaysAsk
            BrowserMode.None -> BrowserModeConfigHelper.None
            BrowserMode.SelectedBrowser -> {
                val selectedBrowser = when {
                    useInAppSettings -> browserSettings.selectedInAppBrowser()
                    else -> browserSettings.selectedBrowser()
                }
                BrowserModeConfigHelper.SelectedBrowser(selectedBrowser)
            }

            BrowserMode.Whitelisted -> {
                val whitelistedPackages = when {
                    useInAppSettings -> browserSettings.whitelistedInAppBrowserPackages()
                    else -> browserSettings.whitelistedNormalBrowserPackages()
                }
                BrowserModeConfigHelper.Whitelisted(whitelistedPackages)
            }
        }
    }
}
