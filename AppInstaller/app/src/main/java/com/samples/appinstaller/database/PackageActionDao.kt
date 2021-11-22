package com.samples.appinstaller.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PackageActionDao {
    @Query("SELECT * FROM package_actions ORDER BY created_at DESC")
    fun getAllActions(): List<PackageAction>

    @Query("SELECT * FROM package_actions ORDER BY created_at DESC")
    fun getMostRecentActions(): List<PackageAction>

    @Insert
    fun insert(action: PackageAction)

    @Update
    fun update(action: PackageAction)
}