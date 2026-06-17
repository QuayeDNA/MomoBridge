package com.momobridge.di

import android.content.Context
import androidx.room.Room
import com.momobridge.data.local.ApiKeyDao
import com.momobridge.data.local.MomoBridgeDatabase
import com.momobridge.data.local.SmsTransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MomoBridgeDatabase {
        return Room.databaseBuilder(
            context,
            MomoBridgeDatabase::class.java,
            "momobridge.db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSmsTransactionDao(database: MomoBridgeDatabase): SmsTransactionDao {
        return database.smsTransactionDao()
    }

    @Provides
    fun provideApiKeyDao(database: MomoBridgeDatabase): ApiKeyDao {
        return database.apiKeyDao()
    }
}
