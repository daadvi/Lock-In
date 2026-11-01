package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LockInDao {
    @Query("SELECT * FROM restricted_apps ORDER BY appName ASC")
    fun getAllAppsFlow(): Flow<List<RestrictedAppEntity>>

    @Query("SELECT * FROM restricted_apps ORDER BY appName ASC")
    suspend fun getAllAppsList(): List<RestrictedAppEntity>

    @Query("SELECT * FROM restricted_apps WHERE isLimited = 1")
    fun getLimitedAppsFlow(): Flow<List<RestrictedAppEntity>>

    @Query("SELECT * FROM restricted_apps WHERE isLimited = 1")
    suspend fun getLimitedAppsList(): List<RestrictedAppEntity>

    @Query("SELECT * FROM restricted_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): RestrictedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateApp(app: RestrictedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<RestrictedAppEntity>)

    @Update
    suspend fun updateApp(app: RestrictedAppEntity)

    @Query("DELETE FROM restricted_apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)

    @Query("DELETE FROM restricted_apps")
    suspend fun deleteAllApps()

    @Query("UPDATE restricted_apps SET individualRemainingSeconds = individualLimitMinutes * 60, shortVideoRemainingSeconds = shortVideoLimitMinutes * 60, longVideoRemainingSeconds = longVideoLimitMinutes * 60, isBlocked = 0")
    suspend fun resetAllAppTimers()

    @Query("UPDATE restricted_apps SET individualRemainingSeconds = individualLimitMinutes * 60, shortVideoRemainingSeconds = shortVideoLimitMinutes * 60, longVideoRemainingSeconds = longVideoLimitMinutes * 60, isBlocked = 0 WHERE packageName = :packageName")
    suspend fun resetAppTimer(packageName: String)

    // Websites
    @Query("SELECT * FROM restricted_websites ORDER BY domain ASC")
    fun getAllWebsitesFlow(): Flow<List<RestrictedWebsiteEntity>>

    @Query("SELECT * FROM restricted_websites")
    suspend fun getAllWebsitesList(): List<RestrictedWebsiteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWebsite(website: RestrictedWebsiteEntity)

    @Update
    suspend fun updateWebsite(website: RestrictedWebsiteEntity)

    @Delete
    suspend fun deleteWebsite(website: RestrictedWebsiteEntity)

    @Query("DELETE FROM restricted_websites")
    suspend fun deleteAllWebsites()

    @Query("UPDATE restricted_websites SET remainingSeconds = limitMinutes * 60, isBlocked = 0")
    suspend fun resetAllWebsiteTimers()

    // App Groups (Conjuntos de apps)
    @Query("SELECT * FROM app_groups ORDER BY createdAt ASC")
    fun getAllAppGroupsFlow(): Flow<List<AppGroupEntity>>

    @Query("SELECT * FROM app_groups ORDER BY createdAt ASC")
    suspend fun getAllAppGroupsList(): List<AppGroupEntity>

    @Query("SELECT * FROM app_groups WHERE id = :id LIMIT 1")
    suspend fun getAppGroup(id: String): AppGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAppGroup(group: AppGroupEntity)

    @Update
    suspend fun updateAppGroup(group: AppGroupEntity)

    @Query("DELETE FROM app_groups WHERE id = :id")
    suspend fun deleteAppGroup(id: String)

    @Query("DELETE FROM app_groups")
    suspend fun deleteAllAppGroups()

    @Query("UPDATE app_groups SET remainingSeconds = limitMinutes * 60, isBlocked = 0")
    suspend fun resetAllAppGroupTimers()
}
