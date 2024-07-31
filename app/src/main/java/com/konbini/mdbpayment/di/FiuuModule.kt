package com.konbini.mdbpayment.di

import com.konbini.mdbpayment.data.remote.fiuu.FiuuRemoteDataSource
import com.konbini.mdbpayment.data.remote.fiuu.FiuuService
import com.konbini.mdbpayment.data.repository.FiuuRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FiuuModule {
    @Provides
    fun provideFiuuService(retrofit: Retrofit): FiuuService = retrofit.create(
        FiuuService::class.java
    )

    @Singleton
    @Provides
    fun provideFiuuRemoteDataSource(fiuuService: FiuuService) =
        FiuuRemoteDataSource(fiuuService)

    @Singleton
    @Provides
    fun provideFiuuRepository(
        remoteDataSource: FiuuRemoteDataSource
    ) = FiuuRepository(remoteDataSource)
}