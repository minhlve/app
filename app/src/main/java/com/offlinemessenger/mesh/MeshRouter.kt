package com.offlinemessenger.mesh

import java.util.concurrent.ConcurrentHashMap

/** Store-and-forward router. It never decrypts a packet; only the destination owns that key. */
class MeshRouter(
    private val self: NodeId,
    private val store: PacketStore,
    private val transport: MeshTransport,
    private val onDeliveredHere: (MeshPacket) -> Unit,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val seen = ExpiringIdSet()
    private val routes = ConcurrentHashMap<NodeId, Route>()

    fun onPeerAvailable(peer: NodeId) {
        routes[peer] = Route(peer, peer, 1, clock() + ROUTE_TTL_MS)
        flush()
    }

    fun onPacket(from: NodeId, packet: MeshPacket) {
        if (!packet.canTravel(clock()) || !seen.add(packet.id, clock())) return
        if (MeshPacket.sha256(packet.payload) != packet.checksum) return
        if (packet.destination == self) {
            store.remove(packet.id); onDeliveredHere(packet)
            return
        }
        // Learn a reverse route to the packet's source; no content inspection occurs.
        routes[packet.origin] = Route(packet.origin, from, 1, clock() + ROUTE_TTL_MS)
        forwardOrStore(packet.forwarded())
    }

    fun submit(packet: MeshPacket) {
        require(packet.origin == self) { "Only local packets may be submitted" }
        seen.add(packet.id, clock())
        forwardOrStore(packet)
    }

    fun flush() {
        store.pending().forEach { pending ->
            if (!pending.packet.canTravel(clock())) store.update(pending.copy(state = DeliveryState.EXPIRED))
            else forwardOrStore(pending.packet)
        }
    }

    private fun forwardOrStore(packet: MeshPacket) {
        if (!packet.canTravel(clock())) { store.update(PendingPacket(packet, DeliveryState.EXPIRED)); return }
        val route = routes[packet.destination]
        val sent = route != null && transport.isAvailable(route.nextHop) && transport.send(route.nextHop, packet)
        if (sent) store.update(PendingPacket(packet, DeliveryState.RELAYING, clock()))
        else store.update(PendingPacket(packet, DeliveryState.QUEUED, clock()))
    }

    companion object { private const val ROUTE_TTL_MS = 10 * 60 * 1000L }
}

interface MeshTransport { fun isAvailable(peer: NodeId): Boolean; fun send(peer: NodeId, packet: MeshPacket): Boolean }
interface PacketStore { fun pending(): List<PendingPacket>; fun update(pending: PendingPacket); fun remove(id: String) }

class MemoryPacketStore : PacketStore {
    private val packets = ConcurrentHashMap<String, PendingPacket>()
    override fun pending() = packets.values.toList()
    override fun update(pending: PendingPacket) { packets[pending.packet.id] = pending }
    override fun remove(id: String) { packets.remove(id) }
}

private class ExpiringIdSet {
    private val ids = ConcurrentHashMap<String, Long>()
    fun add(id: String, now: Long): Boolean {
        ids.entries.removeIf { it.value < now - 24 * 60 * 60 * 1000L }
        return ids.putIfAbsent(id, now) == null
    }
}
