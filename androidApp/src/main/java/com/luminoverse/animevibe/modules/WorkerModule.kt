package com.luminoverse.animevibe.modules

import com.luminoverse.animevibe.utils.workers.BroadcastNotificationWorker
import com.luminoverse.animevibe.utils.workers.UnfinishedWatchNotificationWorker
import com.luminoverse.animevibe.utils.workers.NotificationDeliveryWorker
import com.luminoverse.animevibe.utils.workers.WidgetUpdateWorker
import com.luminoverse.animevibe.utils.factories.ChildWorkerFactory
import com.luminoverse.animevibe.utils.workers.DebugNotificationWorker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkerModule {

    @Binds
    @IntoMap
    @StringKey("com.luminoverse.animevibe.utils.workers.UnfinishedWatchNotificationWorker")
    @Singleton
    abstract fun bindUnfinishedWatchNotificationWorkerFactory(
        factory: UnfinishedWatchNotificationWorker.Factory
    ): ChildWorkerFactory

    @Binds
    @IntoMap
    @StringKey("com.luminoverse.animevibe.utils.workers.BroadcastNotificationWorker")
    @Singleton
    abstract fun bindBroadcastNotificationWorkerFactory(
        factory: BroadcastNotificationWorker.Factory
    ): ChildWorkerFactory

    @Binds
    @IntoMap
    @StringKey("com.luminoverse.animevibe.utils.workers.NotificationDeliveryWorker")
    @Singleton
    abstract fun bindNotificationDeliveryWorkerFactory(
        factory: NotificationDeliveryWorker.Factory
    ): ChildWorkerFactory

    @Binds
    @IntoMap
    @StringKey("com.luminoverse.animevibe.utils.workers.DebugNotificationWorker")
    @Singleton
    abstract fun bindDebugNotificationWorkerFactory(
        factory: DebugNotificationWorker.Factory
    ): ChildWorkerFactory

    @Binds
    @IntoMap
    @StringKey("com.luminoverse.animevibe.utils.workers.WidgetUpdateWorker")
    @Singleton
    abstract fun bindWidgetUpdateWorkerFactory(
        factory: WidgetUpdateWorker.Factory
    ): ChildWorkerFactory
}