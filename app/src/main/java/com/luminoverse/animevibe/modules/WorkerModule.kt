package com.luminoverse.animevibe.modules

import com.luminoverse.animevibe.utils.AnimeBroadcastNotificationWorker
import com.luminoverse.animevibe.utils.ChildWorkerFactory
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
    @StringKey("com.luminoverse.animevibe.utils.AnimeBroadcastNotificationWorker")
    fun bindAnimeBroadcastNotificationWorker(
        factory: AnimeBroadcastNotificationWorker.Factory
    ): ChildWorkerFactory
}