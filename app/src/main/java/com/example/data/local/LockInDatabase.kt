package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RestrictedAppEntity::class, RestrictedWebsiteEntity::class, AppGroupEntity::class],
    version = 2,
    exportSchema = false
)
abstract class LockInDatabase : RoomDatabase() {
    abstract fun lockInDao(): LockInDao

    companion object {
        @Volatile
        private var INSTANCE: LockInDatabase? = null

        fun getDatabase(context: Context): LockInDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let { return@synchronized it }
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LockInDatabase::class.java,
                    "lock_in_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
