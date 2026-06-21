package fe.linksheet.module.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "host_behavior_item")
data class HostBehaviorItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val host: String,
    val type: Int, // 0: Specific App, 1: Preferred Browser, 2: Browsers, 3: Native Apps
    val packageName: String? = null,
    val componentName: String? = null,
    val position: Int
) {
    companion object {
        const val TYPE_SPECIFIC_APP = 0
        const val TYPE_PREFERRED_BROWSER = 1
        const val TYPE_BROWSERS = 2
        const val TYPE_NATIVE_APPS = 3
    }
}
