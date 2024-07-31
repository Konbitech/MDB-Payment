package com.konbini.mdbpayment.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.konbini.mdbpayment.AppSettings
import com.konbini.mdbpayment.base.OkInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Singleton
    @Provides
    fun provideRetrofit(gson: Gson) : Retrofit = Retrofit.Builder()
        .baseUrl(AppSettings.Fiuu.PaymentUrl)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(OkInterceptor.client)
        .build()

    @Provides
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
        .create()
}