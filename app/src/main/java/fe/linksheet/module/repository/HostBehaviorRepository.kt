package fe.linksheet.module.repository

import fe.linksheet.module.database.dao.HostBehaviorDao
import fe.linksheet.module.database.entity.HostBehaviorItem
import kotlinx.coroutines.flow.Flow

class HostBehaviorRepository(
    private val dao: HostBehaviorDao
) {
    fun getAllHosts(): Flow<List<String>> = dao.getAllHosts()

    fun getByHost(host: String): Flow<List<HostBehaviorItem>> = dao.getByHost(host)

    suspend fun getBehaviorsByHost(host: String): List<HostBehaviorItem> = dao.getBehaviorsByHost(host)

    suspend fun updateBehavior(host: String, items: List<HostBehaviorItem>) {
        dao.updateBehavior(host, items)
    }

    suspend fun deleteByHost(host: String) {
        dao.deleteByHost(host)
    }
}
