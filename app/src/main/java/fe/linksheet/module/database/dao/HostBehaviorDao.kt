package fe.linksheet.module.database.dao

import androidx.room3.*
import fe.linksheet.module.database.entity.HostBehaviorItem
import kotlinx.coroutines.flow.Flow

@Dao
interface HostBehaviorDao {
    @Query("SELECT * FROM host_behavior_item ORDER BY position ASC")
    fun getAll(): Flow<List<HostBehaviorItem>>

    @Query("SELECT * FROM host_behavior_item WHERE host = :host ORDER BY position ASC")
    fun getByHost(host: String): Flow<List<HostBehaviorItem>>

    @Query("SELECT * FROM host_behavior_item WHERE host = :host ORDER BY position ASC")
    suspend fun getBehaviorsByHost(host: String): List<HostBehaviorItem>

    @Query("SELECT DISTINCT host FROM host_behavior_item")
    fun getAllHosts(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(items: List<HostBehaviorItem>)

    @Query("DELETE FROM host_behavior_item WHERE host = :host")
    suspend fun deleteByHost(host: String)

    @Transaction
    suspend fun updateBehavior(host: String, items: List<HostBehaviorItem>) {
        deleteByHost(host)
        insert(items)
    }
}
