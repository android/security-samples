package com.samples.appinstaller.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.samples.appinstaller.store.PackageName

@Entity(tableName = "package_actions")
data class PackageAction(
    @PrimaryKey val sessionId: Int,
    @ColumnInfo(name = "package_name") val packageName: PackageName,
    @ColumnInfo(name = "type") val type: ActionType,
    @ColumnInfo(name = "status") val status: ActionStatus,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

enum class ActionType{
    INSTALL, UNINSTALL
}

enum class ActionStatus{
    INITIALIZED, COMMITTED, PENDING_USER_ACTION, SUCCESS, FAILURE
}