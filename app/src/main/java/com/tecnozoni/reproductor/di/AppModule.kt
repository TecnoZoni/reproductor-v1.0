package com.tecnozoni.reproductor.di

import android.content.Context
import androidx.room.Room
import com.tecnozoni.reproductor.data.local.AppDatabase
import com.tecnozoni.reproductor.data.local.CustomOrderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt para lo que no se puede crear con @Inject constructor (Room).
 * Le enseña a Hilt cómo construir la base de datos y el DAO.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "reproductor.db").build()

    @Provides
    fun provideCustomOrderDao(db: AppDatabase): CustomOrderDao = db.customOrderDao()
}
