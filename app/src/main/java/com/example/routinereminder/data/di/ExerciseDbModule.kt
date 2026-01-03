package com.example.routinereminder.data.di

import com.example.routinereminder.data.exercisedb.ExerciseDbRepository
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ExerciseDbModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient()

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    @Named("exerciseDbBaseUrl")
    fun provideExerciseDbBaseUrl(): String = ExerciseDbRepository.DEFAULT_BASE_URL
}
