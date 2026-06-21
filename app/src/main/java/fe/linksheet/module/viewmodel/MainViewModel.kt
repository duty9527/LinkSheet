package fe.linksheet.module.viewmodel


import android.app.Activity
import android.app.Application
import android.content.ClipboardManager
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.viewModelScope
import app.linksheet.api.preference.AppPreferenceRepository
import app.linksheet.compose.debug.DebugMenuSlotProvider
import app.linksheet.feature.app.core.PackageIntentHandler
import app.linksheet.feature.devicecompat.miui.MiuiCompat
import app.linksheet.feature.devicecompat.miui.MiuiCompatProvider
import app.linksheet.feature.shizuku.service.ShizukuService
import app.linksheet.feature.shizuku.usecase.ShizukuStatusUseCase
import fe.composekit.extension.getSystemServiceOrThrow
import fe.linksheet.extension.android.tryStartActivity
import fe.linksheet.module.preference.app.AppPreferences
import fe.linksheet.module.preference.experiment.ExperimentRepository
import fe.linksheet.module.preference.experiment.Experiments
import fe.linksheet.module.preference.state.AppStatePreferences
import fe.linksheet.module.preference.state.DefaultAppStateRepository
import fe.linksheet.module.viewmodel.base.BaseViewModel
import fe.linksheet.usecase.ClipboardUseCase
import fe.std.coroutines.RefreshableStateFlow
import fe.std.result.isSuccess
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch


class MainViewModel(
    val context: Application,
    val appStateRepository: DefaultAppStateRepository,
    val preferenceRepository: AppPreferenceRepository,
    val experimentRepository: ExperimentRepository,
    private val miuiCompatProvider: MiuiCompatProvider,
    private val miuiCompat: MiuiCompat,
    val debugMenu: DebugMenuSlotProvider,
    private val intentHandler: PackageIntentHandler,
    private val shizukuService: ShizukuService,
) : BaseViewModel(preferenceRepository) {
    val clipboardUseCase: ClipboardUseCase = ClipboardUseCase(
        repository = preferenceRepository,
        clipboardManager = context.getSystemServiceOrThrow<ClipboardManager>(),
        coroutineScope = viewModelScope
    )
    val shizukuStatusUseCase = ShizukuStatusUseCase(
        shizukuService = shizukuService
    )

    init {
        Log.d("MainViewModel", "init")
        clipboardUseCase.init()
        addCloseable(clipboardUseCase)
    }

    val shizukuEnabled = experimentRepository.asViewModelState(Experiments.newShizuku)
    val newDefaultsDismissed =
        appStateRepository.asViewModelState(AppStatePreferences.newDefaults_2025_12_15_InfoDismissed)

    private val _showMiuiAlert = RefreshableStateFlow(false) {
        if (miuiCompatProvider.isRequired.value) miuiCompat.showAlert(context) else false
    }

    val showMiuiAlert = _showMiuiAlert

    suspend fun updateMiuiAutoStartAppOp(activity: Activity?): Boolean {
        if (activity == null) return false
        val result = miuiCompat.startPermissionRequest(activity)
        _showMiuiAlert.refresh()

        return result
    }

    private val _defaultBrowser = { intentHandler.isSelfDefaultBrowser() }.asFlow()
    val defaultBrowser = _defaultBrowser

    fun launchIntent(activity: Activity?, intent: SettingsIntent): Boolean {
        if (activity == null) return false
        return activity.tryStartActivity(Intent(intent.action)).isSuccess()
    }

    enum class SettingsIntent(val action: String) {
        DefaultApps(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
        DomainUrls("android.settings.MANAGE_DOMAIN_URLS"),
        CrossProfileAccess("android.settings.MANAGE_CROSS_PROFILE_ACCESS")
    }
}
