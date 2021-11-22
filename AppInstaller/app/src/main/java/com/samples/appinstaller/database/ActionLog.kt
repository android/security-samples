package com.samples.appinstaller.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.samples.appinstaller.store.PackageName

@Entity(tableName = "action_logs")
data class ActionLog(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "session_id") val sessionId: Int,
    @ColumnInfo(name = "package_name") val packageName: PackageName,
    @ColumnInfo(name = "type") val type: ActionType,
    @ColumnInfo(name = "status") val status: ActionStatus,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)