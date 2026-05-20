package com.ilya.safarshots.di

import com.ilya.safarshots.data.repository.CollageRepositoryImpl
import com.ilya.safarshots.data.repository.GeoPhotoRepositoryImpl
import com.ilya.safarshots.domain.repository.CollageRepository
import com.ilya.safarshots.domain.repository.GeoPhotoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    @Singleton
    fun bindGeoPhotoRepository(impl: GeoPhotoRepositoryImpl): GeoPhotoRepository

    @Binds
    @Singleton
    fun bindCollageRepository(impl: CollageRepositoryImpl): CollageRepository
}
