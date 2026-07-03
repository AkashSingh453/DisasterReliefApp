package com.disasterrelief.app.mesh

import com.disasterrelief.app.data.sync.SyncPayload
import com.disasterrelief.app.data.sync.toDomain
import com.disasterrelief.app.data.sync.toProto
import com.disasterrelief.proto.SyncPayloadProto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles byte-level payload construction and reconstruction for Nearby Connections transport.
 *
 * Conversion pipeline (now using Protocol Buffers instead of JSON):
 * ```
 * OUTBOUND: SyncPayload → SyncPayloadProto → protobuf byte[] → Payload.fromBytes()
 * INBOUND:  Payload.asBytes() → protobuf byte[] → SyncPayloadProto → SyncPayload
 * ```
 *
 * Protocol Buffers provide ~4x smaller payloads and ~20x faster parsing compared to JSON,
 * which is critical for low-bandwidth mesh networks and battery-constrained disaster scenarios.
 */
@Singleton
class MeshPayloadHandler @Inject constructor() {

    /**
     * Encodes a [SyncPayload] into a compact protobuf byte array for transmission
     * via Nearby Connections.
     *
     * @param payload The sync payload to encode.
     * @return Protobuf-encoded byte array (significantly smaller than JSON).
     */
    fun encodePayload(payload: SyncPayload): ByteArray {
        val bytes = payload.toProto().toByteArray()
        android.util.Log.d("MeshPayloadHandler", "Encoded payload for mesh transfer: ${bytes.size} bytes (SOS: ${payload.sosRequests.size}, Msgs: ${payload.messages.size})")
        return bytes
    }

    /**
     * Decodes a protobuf byte array received from Nearby Connections back into a [SyncPayload].
     *
     * @param bytes Raw bytes received from [com.google.android.gms.nearby.connection.Payload.asBytes].
     * @return The deserialized [SyncPayload] ready for CRDT merge.
     * @throws com.google.protobuf.InvalidProtocolBufferException if the bytes contain invalid protobuf data.
     */
    fun decodePayload(bytes: ByteArray): SyncPayload {
        val proto = SyncPayloadProto.parseFrom(bytes)
        android.util.Log.d("MeshPayloadHandler", "Decoded payload from mesh transfer: ${bytes.size} bytes (SOS: ${proto.sosRequestsCount}, Msgs: ${proto.messagesCount})")
        return proto.toDomain()
    }
}
