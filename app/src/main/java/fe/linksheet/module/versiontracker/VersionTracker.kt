package fe.linksheet.module.versiontracker

import androidx.lifecycle.LifecycleOwner
import app.linksheet.api.SystemInfoService
import app.linksheet.api.preference.AppPreferenceRepository
import com.google.gson.Gson
import fe.android.lifecycle.LifecycleAwareService
import fe.android.lifecycle.koin.extension.service
import fe.gson.GlobalGsonModule
import fe.gson.GsonQualifier
import fe.linksheet.BuildConfig
import fe.linksheet.module.preference.app.AppPreferences
import fe.linksheet.module.systeminfo.SystemInfoServiceModule
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module

val VersionTrackerModule = module {
    includes(GlobalGsonModule, SystemInfoServiceModule
//        , PreferenceRepositoryModule
    )

    service<VersionTracker, AppPreferenceRepository> { _, preferences ->
        VersionTracker(
            preferenceRepository = preferences,
            systemInfoService = scope.get<SystemInfoService>(),
            gson = scope.get(qualifier(GsonQualifier.Compact))
        )
    }
}

internal class VersionTracker(
    val preferenceRepository: AppPreferenceRepository,
    private val systemInfoService: SystemInfoService,
    val gson: Gson,
) : LifecycleAwareService {
    private val lastVersionsService by lazy {
        LastVersionService(gson, systemInfoService.buildInfo)
    }

    override suspend fun onAppInitialized(owner: LifecycleOwner) {
        val lastVersions = preferenceRepository.get(AppPreferences.lastVersions)
        val lastVersionJson = lastVersionsService.handleVersions(lastVersions, true)

        preferenceRepository.edit {
            lastVersionJson?.let {
                put(AppPreferences.lastVersions, it)
            }

            put(AppPreferences.lastVersion, BuildConfig.VERSION_CODE)
        }
    }

    override suspend fun onStop() {
    }
}
