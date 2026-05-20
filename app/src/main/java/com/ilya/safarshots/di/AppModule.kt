package com.ilya.safarshots.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ilya.safarshots.core.location.AndroidLocationClient
import com.ilya.safarshots.core.location.LocationClient
import com.ilya.safarshots.data.local.AppDatabase
import com.ilya.safarshots.data.local.CollageDao
import com.ilya.safarshots.data.local.GeoPhotoDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE geo_photos ADD COLUMN mapSnapshotUri TEXT")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "geomapshots.db")
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration(false)
            .build()

    @Provides
    fun provideGeoPhotoDao(database: AppDatabase): GeoPhotoDao = database.geoPhotoDao()

    @Provides
    fun provideCollageDao(database: AppDatabase): CollageDao = database.collageDao()

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

@Module
@InstallIn(SingletonComponent::class)
interface BindingsModule {
    @Binds
    @Singleton
    fun bindLocationClient(impl: AndroidLocationClient): LocationClient
}
