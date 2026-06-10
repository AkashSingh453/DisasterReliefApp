package com.disasterrelief.app.mesh

import com.disasterrelief.app.data.sync.CrdtSyncEngine
import com.disasterrelief.app.data.sync.SyncPayload
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles byte-level payload construction and reconstruction for Nearby Connections transport.
 *
 * Conversion pipeline:
 * ```
 * OUTBOUND: SyncPayload → JSON String → UTF-8 byte[] → Payload.fromBytes()
 * INBOUND:  Payload.asBytes() → UTF-8 byte[] → JSON String → SyncPayload
 * ```
 *
 * This layer exists to isolate the serialization format (currently JSON) from both the
 * mesh manager and the CRDT engine. If a more compact format (e.g., Protocol Buffers,
 * MessagePack) is needed in the future, only this class needs to change.
 */
@Singleton
class MeshPayloadHandler @Inject constructor() {

    /**
     * Encodes a [SyncPayload] into a byte array for transmission via Nearby Connections.
     *
     * Uses the [CrdtSyncEngine]'s JSON serializer for consistency with the cloud sync format.
     * The resulting bytes are compact (no pretty-printing, defaults encoded).
     *
     * @param payload The sync payload to encode.
     * @param syncEngine The CRDT engine providing the JSON serializer.
     * @return UTF-8 encoded byte array of the JSON representation.
     */
    fun encodePayload(payload: SyncPayload, syncEngine: CrdtSyncEngine): ByteArray {
        val jsonString = syncEngine.encodeToJsonString(payload)
        return jsonString.toByteArray(Charsets.UTF_8)
    }

    /**
     * Decodes a byte array received from Nearby Connections back into a [SyncPayload].
     *
     * @param bytes Raw bytes received from [com.google.android.gms.nearby.connection.Payload.asBytes].
     * @param syncEngine The CRDT engine providing the JSON deserializer.
     * @return The deserialized [SyncPayload] ready for CRDT merge.
     * @throws kotlinx.serialization.SerializationException if the bytes contain invalid JSON.
     */
    fun decodePayload(bytes: ByteArray, syncEngine: CrdtSyncEngine): SyncPayload {
        val jsonString = bytes.toString(Charsets.UTF_8)
        return syncEngine.decodeFromJsonString(jsonString)
    }
}
