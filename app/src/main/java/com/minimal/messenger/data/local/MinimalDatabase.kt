package com.minimal.messenger.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ConversationEntity::class, MessageEntity::class, CallEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MinimalDatabase : RoomDatabase() {
    abstract fun minimalDao(): MinimalDao

    companion object {
        @Volatile
        private var INSTANCE: MinimalDatabase? = null

        fun getInstance(context: Context): MinimalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MinimalDatabase::class.java,
                    "minimal_messenger.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
