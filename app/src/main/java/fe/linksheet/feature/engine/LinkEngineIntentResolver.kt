package fe.linksheet.feature.engine

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import app.linksheet.feature.app.core.AppInfoCreator
import app.linksheet.feature.app.core.PackageIntentHandler
import app.linksheet.feature.app.core.PackageLauncherService
import app.linksheet.feature.app.core.labelSorted
import app.linksheet.feature.browser.core.PrivateBrowsingService
import app.linksheet.feature.engine.core.EngineScenarioInput
import app.linksheet.feature.engine.core.ForwardOtherProfileResult
import app.linksheet.feature.engine.core.IntentEngineResult
import app.linksheet.feature.engine.core.ScenarioSelector
import app.linksheet.feature.engine.core.UrlEngineResult
import app.linksheet.feature.engine.core.context.DefaultEngineRunContext
import app.linksheet.feature.engine.core.context.EngineFlag
import app.linksheet.feature.engine.core.context.IgnoreLibRedirectExtra
import app.linksheet.feature.engine.core.context.SkipFollowRedirectsExtra
import app.linksheet.feature.engine.core.context.toExtra
import app.linksheet.feature.engine.core.fetcher.ContextResultId
import app.linksheet.feature.engine.core.fetcher.preview.toUnfurlResult
import app.linksheet.feature.engine.core.fetcher.toFetchResult
import app.linksheet.feature.libredirect.database.entity.LibRedirectDefault
import fe.composekit.mozilla.components.support.base.log.logger.Logger
import app.linksheet.mozilla.components.support.utils.SafeIntent
import fe.composekit.core.AndroidAppPackage
import fe.composekit.core.Scheme
import fe.composekit.core.getAndroidAppPackage
import fe.composekit.lifecycle.network.core.NetworkStateService
import fe.linksheet.extension.std.toAndroidUri
import fe.linksheet.extension.std.toStdUrl
import app.linksheet.api.preference.AppPreferenceRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fe.linksheet.module.preference.app.TextReplaceRule
import fe.linksheet.module.preference.app.AppPreferences
import fe.linksheet.module.database.entity.PreferredApp
import fe.linksheet.module.repository.AppSelectionHistoryRepository
import fe.linksheet.module.repository.PreferredAppRepository
import fe.linksheet.module.resolver.ImprovedBrowserHandler
import fe.linksheet.module.resolver.InAppBrowserHandler
import fe.linksheet.module.resolver.IntentResolveResult
import fe.linksheet.module.resolver.IntentResolver
import fe.linksheet.module.resolver.IntentResolverCommon
import fe.linksheet.module.resolver.ResolveEvent
import fe.linksheet.module.resolver.ResolveModuleStatus
import fe.linksheet.module.resolver.ResolveOptions
import fe.linksheet.module.resolver.ResolverInteraction
import fe.linksheet.module.resolver.module.IntentResolverSettings
import fe.linksheet.module.resolver.personal.PersonalLinkRuleEngine
import fe.linksheet.module.resolver.personal.PersonalPreferredAppSelector
import fe.linksheet.module.resolver.util.AppSorter
import fe.linksheet.module.resolver.util.CustomTabHandler
import fe.linksheet.module.resolver.util.CustomTabInfo2
import fe.linksheet.module.resolver.util.IntentSanitizer
import fe.linksheet.util.intent.cloneIntent
import fe.linksheet.util.intent.parser.IntentParser
import fe.linksheet.util.intent.parser.UriException
import fe.linksheet.util.intent.parser.UriParseException
import fe.std.result.IResult
import fe.std.result.isFailure
import fe.std.result.unaryPlus
import fe.std.uri.StdUrl
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import fe.linksheet.module.repository.HostBehaviorRepository
import fe.linksheet.module.database.entity.HostBehaviorItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class LinkEngineIntentResolver(
    val context: Context,
    val client: HttpClient,
    private val appSelectionHistoryRepository: AppSelectionHistoryRepository,
    private val preferredAppRepository: PreferredAppRepository,
    private val packageIntentHandler: PackageIntentHandler,
    private val packageLauncherService: PackageLauncherService,
    private val appSorter: AppSorter,
    private val appInfoCreator: AppInfoCreator,
    private val browserHandler: ImprovedBrowserHandler,
    private val inAppBrowserHandler: InAppBrowserHandler,
    private val networkStateService: NetworkStateService,
    private val selector: ScenarioSelector,
    private val privateBrowsingService: PrivateBrowsingService,
    private val settings: IntentResolverSettings,
    private val personalLinkRuleEngine: PersonalLinkRuleEngine,
    private val hostBehaviorRepository: HostBehaviorRepository,
    private val appPreferenceRepository: AppPreferenceRepository,
    private val gson: Gson,
) : IntentResolver {
    companion object {
        const val IntentKeyDownloader = "downloader"
        const val IntentKeyResolveRedirects = "resolve_redirects"
    }

    private val logger = Logger("LinkEngineIntentResolver")
    private val browserSettings = settings.browserSettings
    private val previewSettings = settings.previewSettings
    private val downloaderSettings = settings.downloaderSettings
    private val libRedirectSettings = settings.libRedirectSettings
    private val amp2HtmlSettings = settings.amp2HtmlSettings
    private val followRedirectsSettings = settings.followRedirectsSettings

    private val _events = MutableStateFlow(value = ResolveEvent.Idle)
    override val events = _events.asStateFlow()

    private val _interactions = MutableStateFlow<ResolverInteraction>(value = ResolverInteraction.Idle)
    override val interactions = _interactions.asStateFlow()

    private fun emitEvent(event: ResolveEvent) {
        _events.tryEmit(event)
        logger.debug(event.toString())
    }

    private fun emitEventIf(predicate: Boolean, event: ResolveEvent) {
        if (!predicate) return
        emitEvent(event)
    }

    private fun emitInteraction(interaction: ResolverInteraction) {
        _interactions.tryEmit(interaction)
        logger.debug("Emitted interaction $interaction")
    }

    private fun clearInteraction() = emitInteraction(ResolverInteraction.Clear)

    private fun fail(error: String, result: IntentResolveResult): IntentResolveResult {
        logger.error(error)
        return result
    }

    private suspend fun initState(event: ResolveEvent, interaction: ResolverInteraction) {
        _events.emit(event)
        _interactions.emit(interaction)
    }

    private fun parseIntent(intent: SafeIntent): IResult<StdUrl> {
        val uriResult = IntentParser.getUriFromIntent(intent)
        if (uriResult.isFailure()) {
            logger.error("Failed to parse intent ${intent.action}")
            return +uriResult
        }

        val url = uriResult.value.toStdUrl() ?: return +UriParseException()
        return +url
    }

    override suspend fun resolve(
        intent: SafeIntent,
        options: ResolveOptions
    ): IntentResolveResult = coroutineScope scope@{
        initState(ResolveEvent.Initialized, ResolverInteraction.Initialized)

        val searchIntentResult = tryHandleSearchIntent(intent)
        if (searchIntentResult != null) {
            return@scope searchIntentResult
        }

        // 极前置文本改写
        val rulesJson = appPreferenceRepository.get(AppPreferences.textReplaceRulesJson)
        val rulesType = object : TypeToken<List<TextReplaceRule>>() {}.type
        val rules: List<TextReplaceRule> = try {
            gson.fromJson(rulesJson, rulesType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        if (rules.any { it.isEnabled && it.pattern.isNotEmpty() }) {
            // 1. 处理 intent.data
            intent.data?.let { dataUri ->
                val orig = dataUri.toString()
                var processed = orig
                for (rule in rules) {
                    if (rule.isEnabled && rule.pattern.isNotEmpty()) {
                        processed = processed.replace(rule.pattern, rule.replacement)
                    }
                }
                if (processed != orig) {
                    try {
                        intent.unsafe.data = Uri.parse(processed)
                    } catch (e: Exception) {
                        logger.error("Failed to parse replaced URI: $processed", e)
                    }
                }
            }

            // 2. 处理 EXTRA_TEXT
            intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.let { orig ->
                var processed = orig
                for (rule in rules) {
                    if (rule.isEnabled && rule.pattern.isNotEmpty()) {
                        processed = processed.replace(rule.pattern, rule.replacement)
                    }
                }
                if (processed != orig) {
                    intent.unsafe.putExtra(Intent.EXTRA_TEXT, processed)
                }
            }

            // 3. 处理 EXTRA_PROCESS_TEXT
            intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()?.let { orig ->
                var processed = orig
                for (rule in rules) {
                    if (rule.isEnabled && rule.pattern.isNotEmpty()) {
                        processed = processed.replace(rule.pattern, rule.replacement)
                    }
                }
                if (processed != orig) {
                    intent.unsafe.putExtra(Intent.EXTRA_PROCESS_TEXT, processed)
                }
            }

            // 4. 处理 custom "url" extra
            intent.getCharSequenceExtra("url")?.toString()?.let { orig ->
                var processed = orig
                for (rule in rules) {
                    if (rule.isEnabled && rule.pattern.isNotEmpty()) {
                        processed = processed.replace(rule.pattern, rule.replacement)
                    }
                }
                if (processed != orig) {
                    intent.unsafe.putExtra("url", processed)
                }
            }
        }

        val canAccessInternet = networkStateService.isNetworkConnected
        val urlParseResult = parseIntent(intent)
        if (urlParseResult.isFailure()) {
            return@scope IntentResolveResult.IntentParseFailed(urlParseResult.exception as UriException)
        }

        val startUrl = urlParseResult.value

        if (options.forwardProfile) {
            return@scope IntentResolveResult.OtherProfile(startUrl)
        }

        val referringPackage = options.referrer?.getAndroidAppPackage(Scheme.Package)
        val knownBrowser = privateBrowsingService.isKnownBrowser(referringPackage?.packageName)
        val isReferrerBrowser = knownBrowser != null

        emitEvent(ResolveEvent.QueryingBrowsers)
        val browsers = packageIntentHandler.findHttpBrowsable(null)

        val shouldFollowRedirects = IntentResolverCommon.shouldFollowRedirects(
            enabled = followRedirectsSettings.followRedirects(),
            mode = followRedirectsSettings.followRedirectsMode(),
            skipBrowser = followRedirectsSettings.followRedirectsSkipBrowser(),
            isReferrerBrowser = isReferrerBrowser,
            hasManualFlag = intent.getBooleanExtra(IntentKeyResolveRedirects, false),
        )
        intent.extras?.remove(IntentKeyResolveRedirects)
        val hasManualDownloadFlag = intent.getBooleanExtra(IntentKeyDownloader, false)
        intent.extras?.remove(IntentKeyDownloader)

        val ignoreLibRedirect = checkIntentFlag(
            intent,
            LibRedirectDefault.IgnoreIntentKey,
            libRedirectSettings.enableIgnoreLibRedirectButton()
        )

        val context = DefaultEngineRunContext {
            if (ignoreLibRedirect) {
                add(IgnoreLibRedirectExtra)
            }

            if (!shouldFollowRedirects) {
                add(SkipFollowRedirectsExtra)
            }

            referringPackage?.toExtra()?.let(::add)
            knownBrowser?.toExtra()?.let(::add)
        }

        val input = EngineScenarioInput(startUrl, referringPackage)
        val scenario = selector.findScenario(input)
        logger.info("Found scenario $scenario")
        if (scenario == null) {
            // TODO: What do we do in this situation?
            return@scope IntentResolveResult.NoScenarioFound
        }

        val (_, result) = scenario.run(startUrl, context)
        if (result is IntentEngineResult) {
            return@scope IntentResolveResult.IntentResult(result.intent)
        }

        if (result is ForwardOtherProfileResult) {
            return@scope IntentResolveResult.OtherProfile(result.url)
        }

        val resultUrl = (result as UrlEngineResult).url
        val personalRuleResult = personalLinkRuleEngine.apply(resultUrl.toAndroidUri())
        val resultUri = personalRuleResult.uri


        val allowCustomTab = inAppBrowserHandler.shouldAllowCustomTab(
            referrer = options.referrer,
            inAppBrowserMode = browserSettings.inAppBrowserSettings()
        )
        val customTab = CustomTabHandler.getInfo2(intent, allowCustomTab)
        val newIntent = IntentSanitizer.sanitize(
            intent = intent,
            action = Intent.ACTION_VIEW,
            uri = resultUri,
            dropExtras = customTab.dropExtras
        )

        emitEvent(ResolveEvent.LoadingPreferredApps)
        val app = queryPreferredApp(
            repository = preferredAppRepository,
            packageLauncherService = packageLauncherService,
            uri = resultUri
        )

        val lastUsedApps = queryAppSelectionHistory(
            repository = appSelectionHistoryRepository,
            packageLauncherService = packageLauncherService,
            uri = resultUri
        )
        var resolveList = packageIntentHandler.findHandlers(resultUri, referringPackage?.packageName)
        resolveList = maybeFilter(
            resolveList,
            referringPackage,
            settings.bottomSheetSettings.hideReferringApp()
        )

        emitEvent(ResolveEvent.CheckingBrowsers)
        val browserModeConfigHelper = IntentResolverCommon.createBrowserModeConfig(browserSettings, customTab is CustomTabInfo2.Allowed)
        val appList = browserHandler.filterBrowsers(
            config = browserModeConfigHelper,
            autoLaunchSingleBrowser = settings.browserSettings.autoLaunchSingleBrowser(),
            browsers = browsers,
            resolveList = resolveList
        )

        emitEvent(ResolveEvent.SortingApps)
        val (sortedByUsage, filteredByHistory) = appSorter.sort(
            appList = appList,
            lastChosen = app,
            historyMap = lastUsedApps,
            returnLastChosen = !settings.bottomSheetSettings.dontShowFilteredItem()
        )

        var finalFilteredByHistory = filteredByHistory
        if (finalFilteredByHistory == null && app?.alwaysPreferred == true) {
            val matchedResolveInfo = resolveList.firstOrNull { it.activityInfo.packageName == app.pkg }
                ?: browsers.firstOrNull { it.activityInfo.packageName == app.pkg }
            if (matchedResolveInfo != null) {
                finalFilteredByHistory = appInfoCreator.toActivityAppInfo(matchedResolveInfo, null)
            } else {
                val resolveInfo = packageLauncherService.getLauncherOrNull(app.pkg)
                if (resolveInfo != null) {
                    finalFilteredByHistory = appInfoCreator.toActivityAppInfo(resolveInfo, null)
                }
            }
        }

        val host = resultUri.host?.lowercase(java.util.Locale.getDefault())
        val behaviors = if (host != null) {
            findBehaviorsForHost(host)
        } else {
            emptyList()
        }

        var matchedAppInfo: app.linksheet.feature.app.core.ActivityAppInfo? = null
        var isAutoLaunch = false
        val reorderedAppsList = mutableListOf<app.linksheet.feature.app.core.ActivityAppInfo>()
        val hasBehaviors = behaviors.isNotEmpty()

        if (hasBehaviors) {
            val nativeAppInfos = appList.apps.map { appInfoCreator.toActivityAppInfo(it, null) }
            val browserAppInfos = appList.browsers.map { appInfoCreator.toActivityAppInfo(it, null) }
            val allAvailableAppInfos = nativeAppInfos + browserAppInfos

            for (behavior in behaviors) {
                when (behavior.type) {
                    HostBehaviorItem.TYPE_NATIVE_APPS -> {
                        if (nativeAppInfos.isNotEmpty()) {
                            if (nativeAppInfos.size == 1) {
                                matchedAppInfo = nativeAppInfos.first()
                                isAutoLaunch = true
                            } else {
                                reorderedAppsList.addAll(nativeAppInfos)
                            }
                            break
                        }
                    }
                    HostBehaviorItem.TYPE_SPECIFIC_APP -> {
                        var matched = allAvailableAppInfos.firstOrNull {
                            it.packageName == behavior.packageName &&
                            (behavior.componentName == null || it.flatComponentName == behavior.componentName)
                        }
                        if (matched == null && behavior.packageName != null) {
                            val resolveInfo = packageLauncherService.getLauncherOrNull(behavior.packageName)
                            if (resolveInfo != null) {
                                matched = appInfoCreator.toActivityAppInfo(resolveInfo, null)
                            }
                        }
                        if (matched != null) {
                            matchedAppInfo = matched
                            isAutoLaunch = true
                            break
                        }
                    }
                    HostBehaviorItem.TYPE_PREFERRED_BROWSER -> {
                        if (browserAppInfos.isNotEmpty()) {
                            matchedAppInfo = browserAppInfos.first()
                            isAutoLaunch = true
                            break
                        }
                    }
                    HostBehaviorItem.TYPE_BROWSERS -> {
                        if (browserAppInfos.isNotEmpty()) {
                            reorderedAppsList.addAll(browserAppInfos)
                            break
                        }
                    }
                }
            }
            val remaining = allAvailableAppInfos.filter { it !in reorderedAppsList && it != matchedAppInfo }
            reorderedAppsList.addAll(remaining)
        }

        val hasUserAlwaysPreferredApp = app?.alwaysPreferred == true && finalFilteredByHistory != null
        val personalSelection = PersonalPreferredAppSelector.select(
            sorted = sortedByUsage,
            filtered = finalFilteredByHistory,
            hasUserAlwaysPreferredApp = hasUserAlwaysPreferredApp,
            ruleResult = personalRuleResult
        )
        val isRegularPreferredApp = hasUserAlwaysPreferredApp ||
                (personalSelection.selected && personalRuleResult.autoLaunch)

        val finalResolved = if (hasBehaviors) reorderedAppsList else personalSelection.sorted
        val finalFilteredItem = if (hasBehaviors) matchedAppInfo else personalSelection.filtered
        val finalIsRegularPreferredApp = if (hasBehaviors) isAutoLaunch else isRegularPreferredApp

        val shouldRunDownloader = IntentResolverCommon.shouldRunDownloader(
            enabled = downloaderSettings.enableDownloader(),
            mode = downloaderSettings.downloaderMode(),
            isRegularPreferredApp = finalIsRegularPreferredApp,
            hasManualFlag = hasManualDownloadFlag,
        )
        if (!shouldRunDownloader) {
            context.flags.add(EngineFlag.DisableDownload)
        }

        val shouldRunPreviewUrl = IntentResolverCommon.shouldRunPreviewUrl(
            enabled = previewSettings.previewUrl(),
            previewUrlSkipBrowser = previewSettings.previewUrlSkipBrowser(),
            isReferrerBrowser = isReferrerBrowser,
            isRegularPreferredApp = finalIsRegularPreferredApp
        )
        if (!shouldRunPreviewUrl) {
            context.flags.add(EngineFlag.DisablePreview)
        }

        scenario.fetch(resultUrl, context).collect { fetchHandle ->
            when (fetchHandle) {
                null -> clearInteraction()
                else -> {
                    val resolveEvent = when (fetchHandle.id) {
                        ContextResultId.Download -> ResolveEvent.CheckingDownloader
                        ContextResultId.Preview -> ResolveEvent.GeneratingPreview
                        // TODO: Get rid of this, not a fetch result
                        ContextResultId.LibRedirect -> null
                    }
                    if (resolveEvent != null) {
                        emitEvent(resolveEvent)
                        emitInteraction(ResolverInteraction.Cancelable(resolveEvent, fetchHandle.cancel))
                    }
                }
            }
        }

        val sealedContext = context.seal()
        val downloadResult = sealedContext[ContextResultId.Download]
        return@scope IntentResolveResult.Default(
            intent = newIntent,
            uri = resultUri,
            referrer = options.referrer,
            unfurlResult = sealedContext[ContextResultId.Preview]?.toUnfurlResult(),
            referringPackageName = referringPackage?.packageName,
            resolved = finalResolved,
            filteredItem = finalFilteredItem,
            isRegularPreferredApp = finalIsRegularPreferredApp,
            hasSingleMatchingOption = appList.isSingleOption || appList.noBrowsersOnlySingleApp,
            resolveModuleStatus = ResolveModuleStatus(),
            libRedirectResult = sealedContext[ContextResultId.LibRedirect]?.wrapped,
            downloadable = downloadResult?.toFetchResult()
        )
    }

    private fun tryHandleSearchIntent(intent: SafeIntent): IntentResolveResult.WebSearch? {
        if (intent.action != Intent.ACTION_WEB_SEARCH) return null
        val query = IntentParser.parseSearchIntent(intent) ?: return null
        val newIntent = intent.unsafe
            .cloneIntent(Intent.ACTION_WEB_SEARCH, null, true)
            .putExtra(SearchManager.QUERY, query)

        val resolvedList = packageIntentHandler.findHandlers(newIntent)
            .map { appInfoCreator.toActivityAppInfo(it, null) }
            .labelSorted()

        return IntentResolveResult.WebSearch(query, newIntent, resolvedList)
    }

    private fun maybeFilter(
        resolveList: List<ResolveInfo>,
        referringPackage: AndroidAppPackage?,
        hideReferringApp: Boolean
    ): List<ResolveInfo> {
        if (hideReferringApp && referringPackage != null) {
            return resolveList.filter { it.activityInfo.packageName != referringPackage.packageName }
        }

        return resolveList
    }

    private fun checkIntentFlag(intent: SafeIntent, flag: String, setting: Boolean): Boolean {
        if (!intent.getBooleanExtra(flag, false)) return false
        intent.extras?.remove(flag)
        return setting
    }

    private suspend fun queryAppSelectionHistory(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        repository: AppSelectionHistoryRepository,
        packageLauncherService: PackageLauncherService,
        uri: Uri?,
    ): Map<String, Long> = withContext(dispatcher) {
        val lastUsedApps = repository.getLastUsedForHostGroupedByPackage(uri)
            ?: return@withContext emptyMap()

        val (result, delete) = packageLauncherService.hasLauncher(lastUsedApps.keys)
        if (delete.isNotEmpty()) repository.delete(delete)

        lastUsedApps.filter { it.key in result }.toMap()
    }

    private suspend fun queryPreferredApp(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        repository: PreferredAppRepository,
        packageLauncherService: PackageLauncherService,
        uri: Uri?,
    ): PreferredApp? = withContext(dispatcher) {
        val app = repository.getByHost(uri)
        val resolveInfo = packageLauncherService.getLauncherOrNull(app?.pkg)
        if (app != null && resolveInfo == null) repository.delete(app)

        app
    }

    private suspend fun findBehaviorsForHost(host: String): List<HostBehaviorItem> {
        val exact = hostBehaviorRepository.getBehaviorsByHost(host)
        if (exact.isNotEmpty()) return exact

        var parts = host.split('.')
        while (parts.size > 1) {
            parts = parts.drop(1)
            val parentHost = parts.joinToString(".")
            val parentBehaviors = hostBehaviorRepository.getBehaviorsByHost(parentHost)
            if (parentBehaviors.isNotEmpty()) {
                return parentBehaviors
            }
        }
        return emptyList()
    }

    override suspend fun warmup() {
    }
}
