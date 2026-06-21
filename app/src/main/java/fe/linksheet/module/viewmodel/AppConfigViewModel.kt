package fe.linksheet.module.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import app.linksheet.api.preference.AppPreferenceRepository
import fe.linksheet.module.database.entity.HostBehaviorItem
import fe.linksheet.module.repository.HostBehaviorRepository
import fe.linksheet.module.viewmodel.base.BaseViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AppConfigViewModel(
    val context: Application,
    preferenceRepository: AppPreferenceRepository,
    private val hostBehaviorRepository: HostBehaviorRepository
) : BaseViewModel(preferenceRepository) {

    val allHosts: Flow<List<String>> = hostBehaviorRepository.getAllHosts()

    fun getByHost(host: String): Flow<List<HostBehaviorItem>> {
        return hostBehaviorRepository.getByHost(host)
    }

    fun saveBehavior(host: String, items: List<HostBehaviorItem>) {
        viewModelScope.launch {
            hostBehaviorRepository.updateBehavior(host, items)
        }
    }

    fun deleteHost(host: String) {
        viewModelScope.launch {
            hostBehaviorRepository.deleteByHost(host)
        }
    }
}
