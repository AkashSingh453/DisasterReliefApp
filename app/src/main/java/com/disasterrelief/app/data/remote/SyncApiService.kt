package com.disasterrelief.app.data.remote

import com.disasterrelief.app.data.sync.SyncPayload
import com.disasterrelief.app.data.sync.SyncResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit service interface for cloud synchronization with the Ktor backend.
 *
 * Endpoints:
 * - [uploadSync]: Pushes local database delta to the server for global merge.
 * - [downloadSync]: Pulls server-side delta for local CRDT merge.
 *
 * Both endpoints use [SyncPayload] as the transport format, ensuring consistency
 * between mesh (P2P) and cloud (HTTP) data exchange.
 */
interface SyncApiService {

    /**
     * Uploads the local database state to the cloud server.
     * The server performs LWW merge against its PostgreSQL store.
     *
     * @param payload The batch of records to upload (SOS, messages, nodes).
     * @return [SyncResponse] with acceptance status and server timestamp.
     */
    @POST("api/v1/sync/upload")
    suspend fun uploadSync(@Body payload: SyncPayload): Response<SyncResponse>

    /**
     * Downloads records from the server that have been modified since [sinceTimestamp].
     * The client performs LWW merge against its local Room database.
     *
     * @param nodeId This device's permanent node UUID (for provenance tracking).
     * @param sinceTimestamp Epoch ms. Only records modified after this time are returned.
     * @return [SyncPayload] containing the server's delta.
     */
    @GET("api/v1/sync/download")
    suspend fun downloadSync(
        @Query("nodeId") nodeId: String,
        @Query("sinceTimestamp") sinceTimestamp: Long
    ): Response<SyncPayload>
}
