package com.focusapp.di

import android.content.Context
import com.focusapp.data.prefs.AppPreferences
import com.focusapp.domain.preferences.FocusPreferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {

    @Binds
    @Singleton
    abstract fun bindFocusPreferences(impl: AppPreferences): FocusPreferences

    companion object {
        @Provides
        @Singleton
        fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences =
            AppPreferences(context)
    }
}
