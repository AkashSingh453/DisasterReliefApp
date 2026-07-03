package com.disasterrelief.app.di

import com.disasterrelief.app.BuildConfig
import com.disasterrelief.proto.SyncServiceGrpcKt
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder
import javax.inject.Singleton
import java.net.URI

/**
 * Hilt module providing the gRPC client for cloud synchronization.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    val BASEURL = BuildConfig.SYNC_BASE_URL

    @Provides
    @Singleton
    fun provideGrpcChannel(): ManagedChannel {
        // Parse host and port from the base URL string
        val uri = URI(BASEURL)
        val host = uri.host
        val port = uri.port.takeIf { it != -1 } ?: 80

        return OkHttpChannelBuilder.forAddress(host, port)
            .usePlaintext() // HTTP/2 plaintext for local development/testing
            .build()
    }

    @Provides
    @Singleton
    fun provideSyncServiceStub(channel: ManagedChannel): SyncServiceGrpcKt.SyncServiceCoroutineStub {
        return SyncServiceGrpcKt.SyncServiceCoroutineStub(channel)
    }
}
