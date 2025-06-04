package com.example.pharmacistApp.di

import android.content.Context
import com.example.pharmacistApp.cloudinary.CloudinaryHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(SingletonComponent::class)
object CloudinaryModule {

    // This method will provide CloudinaryHelper as a singleton object after initializing it with the application context
    @Provides
    fun provideCloudinaryHelper(@ApplicationContext context: Context): CloudinaryHelper {
        // Initialize CloudinaryHelper with the application context
        CloudinaryHelper.initialize(context)
        return CloudinaryHelper
    }
}
