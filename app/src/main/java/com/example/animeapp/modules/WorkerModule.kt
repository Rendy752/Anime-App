package com.example.animeapp.modules

import com.example.animeapp.utils.AnimeBroadcastNotificationWorker
import com.example.animeapp.utils.ChildWorkerFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

@Module
@InstallIn(SingletonComponent::class)
interface WorkerModule {

    @Binds
    @IntoMap
    @StringKey("com.example.animeapp.utils.AnimeBroadcastNotificationWorker")
    fun bindAnimeBroadcastNotificationWorker(
        factory: AnimeBroadcastNotificationWorker.Factory
    ): ChildWorkerFactory
}