package com.offlinemessenger.mesh

import java.security.MessageDigest
import java.util.UUID

typealias NodeId = String

enum class PacketType { MESSAGE, FILE_CHUNK, RECEIPT }
enum class DeliveryState { QUEUED, RELAYING, DELIVERED, EXPIRED, FAILED }

data class MeshPacket(
    val id: String = UUID.randomUUID().toString(),
    val type: PacketType,
    val origin: NodeId,
    val destination: NodeId,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val expiresAtMillis: Long = createdAtMillis + 24 * 60 * 60 * 1000,
    val ttl: Int = 8,
    val payload: ByteArray,
    val signature: ByteArray,
    val chunkIndex: Int = 0,
    val chunkCount: Int = 1,
    val checksum: String = sha256(payload)
) {
    fun forwarded() = copy(ttl = ttl - 1)
    fun canTravel(now: Long = System.currentTimeMillis()) = ttl > 0 && now < expiresAtMillis

    companion object {
        fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
            .digest(bytes).joinToString("") { "%02x".format(it) }
    }
}

data class Route(val destination: NodeId, val nextHop: NodeId, val hops: Int, val expiresAtMillis: Long)
data class PendingPacket(val packet: MeshPacket, val state: DeliveryState, val lastAttemptMillis: Long = 0)
