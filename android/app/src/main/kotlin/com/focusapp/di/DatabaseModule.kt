package com.focusapp.di

import android.content.Context
import androidx.room.Room
import com.focusapp.data.db.FocusDatabase
import com.focusapp.data.db.dao.BadgeDao
import com.focusapp.data.db.dao.DistractionEventDao
import com.focusapp.data.db.dao.FocusSessionDao
import com.focusapp.data.db.dao.ReminderDao
import com.focusapp.data.db.dao.UserProfileDao
import com.focusapp.data.repository.BadgeRepositoryImpl
import com.focusapp.data.repository.DistractionRepositoryImpl
import com.focusapp.data.repository.SessionRepositoryImpl
import com.focusapp.data.repository.UserProfileRepositoryImpl
import com.focusapp.domain.repository.BadgeRepository
import com.focusapp.domain.repository.DistractionRepository
import com.focusapp.domain.repository.SessionRepository
import com.focusapp.domain.repository.UserProfileRepository
import dagger.Binds
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
    fun provideDatabase(@ApplicationContext context: Context): FocusDatabase =
        Room.databaseBuilder(
            context,
            FocusDatabase::class.java,
            FocusDatabase.DATABASE_NAME,
        ).build()

    @Provides
    fun provideFocusSessionDao(db: FocusDatabase): FocusSessionDao = db.focusSessionDao()

    @Provides
    fun provideDistractionEventDao(db: FocusDatabase): DistractionEventDao = db.distractionEventDao()

    @Provides
    fun provideUserProfileDao(db: FocusDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    fun provideBadgeDao(db: FocusDatabase): BadgeDao = db.badgeDao()

    @Provides
    fun provideReminderDao(db: FocusDatabase): ReminderDao = db.reminderDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindDistractionRepository(impl: DistractionRepositoryImpl): DistractionRepository

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(impl: UserProfileRepositoryImpl): UserProfileRepository

    @Binds
    @Singleton
    abstract fun bindBadgeRepository(impl: BadgeRepositoryImpl): BadgeRepository
}
