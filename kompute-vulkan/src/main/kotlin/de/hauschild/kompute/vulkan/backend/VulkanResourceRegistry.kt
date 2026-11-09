package de.hauschild.kompute.vulkan.backend

import java.lang.ref.Cleaner
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Tracks the resources that live from a dispatch until its result is read or closed, so that none of
 * them outlives the backend.
 *
 * A resource is released when
 * - it is closed by its owner, which [unregister]s it, or
 * - its owner became unreachable without closing it: [closeWhenUnreachable] queues it through a
 *   shared [Cleaner], and it is closed on the next [register] or [close] call, or
 * - the registry is [close]d, which happens when the backend is closed.
 *
 * Queueing instead of closing in the [Cleaner] thread keeps all releases on the caller's thread,
 * like [VulkanBufferCache] does.
 */
internal class VulkanResourceRegistry : AutoCloseable {
    private val open = LinkedHashSet<AutoCloseable>()
    private val unreachable = ConcurrentLinkedQueue<AutoCloseable>()
    private var closed = false

    /**
     * Starts tracking [resource] and releases the resources of unreachable owners.
     *
     * @param resource the resource to track, it has to call [unregister] when it is closed
     * @throws IllegalStateException if the registry is already closed
     */
    @Synchronized
    fun register(resource: AutoCloseable) {
        check(!closed) { "The backend is closed" }
        flushUnreachable()
        open.add(resource)
    }

    /**
     * Stops tracking [resource], e.g. because it was closed.
     *
     * @param resource the resource to forget
     */
    @Synchronized
    fun unregister(resource: AutoCloseable) {
        open.remove(resource)
    }

    /**
     * Makes sure [resource] is closed once [owner] has become unreachable without closing it.
     *
     * @param owner the object the user holds on to, it must not be referenced by [resource]
     * @param resource the resource to release
     */
    fun closeWhenUnreachable(
        owner: Any,
        resource: AutoCloseable,
    ) {
        cleaner.register(owner) { unreachable.add(resource) }
    }

    /**
     * Closes the resources of all owners that have become unreachable.
     */
    @Synchronized
    fun flushUnreachable() {
        generateSequence { unreachable.poll() }.forEach { it.close() }
    }

    @Synchronized
    override fun close() {
        closed = true
        flushUnreachable()
        open.toList().forEach { it.close() }
        open.clear()
    }

    companion object {
        private val cleaner = Cleaner.create()
    }
}
