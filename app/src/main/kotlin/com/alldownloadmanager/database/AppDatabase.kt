package com.alldownloadmanager.database
import android.content.Context
import androidx.room.*

@Database(entities = [DownloadEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloads(): DownloadDao
    companion object { @Volatile private var instance: AppDatabase? = null
        fun get(context: Context) = instance ?: synchronized(this) { instance ?: Room.databaseBuilder(context, AppDatabase::class.java, "downloads.db").build().also { instance = it } }
    }
}