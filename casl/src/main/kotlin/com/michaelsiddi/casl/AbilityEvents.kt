package com.michaelsiddi.casl

/**
 * Type alias for event handler functions.
 */
public typealias EventHandler = (UpdateEvent) -> Unit

/**
 * Type alias for unsubscribe function returned by on().
 */
public typealias Unsubscribe = () -> Unit

/**
 * Event payload for update and updated events.
 *
 * @property ability The ability instance that was updated
 * @property rules The new rules that were applied
 */
public data class UpdateEvent(
    val ability: Ability,
    val rules: List<RawRule>
)

/**
 * Event types that can be listened to.
 */
public enum class EventType {
    /**
     * Fired before rules are updated.
     * Can be used to cleanup or prepare for rule changes.
     */
    UPDATE,

    /**
     * Fired after rules have been updated.
     * Can be used to react to rule changes.
     */
    UPDATED
}

/**
 * Simple event emitter for ability update events.
 * Thread-safe for adding/removing handlers and emitting events.
 */
internal class EventEmitter {
    // Use concurrent-safe collections
    private val handlers = mutableMapOf<EventType, MutableList<EventHandler>>()

    /**
     * Subscribe to an event.
     *
     * @param eventType The event type to listen to
     * @param handler The handler function to call when event fires
     * @return Function to unsubscribe this handler
     */
    @Synchronized
    fun on(eventType: EventType, handler: EventHandler): Unsubscribe {
        val eventHandlers = handlers.getOrPut(eventType) { mutableListOf() }
        eventHandlers.add(handler)

        // Return unsubscribe function
        return {
            synchronized(this) {
                handlers[eventType]?.remove(handler)
            }
        }
    }

    /**
     * Emit an event to all subscribers.
     *
     * @param eventType The event type to emit
     * @param event The event payload
     */
    @Synchronized
    fun emit(eventType: EventType, event: UpdateEvent) {
        // Get a snapshot of handlers to avoid concurrent modification
        val eventHandlers = handlers[eventType]?.toList() ?: return

        // Call handlers outside synchronized block to avoid potential deadlocks
        eventHandlers.forEach { handler ->
            try {
                handler(event)
            } catch (e: Exception) {
                // Silently catch handler exceptions to prevent one handler
                // from breaking others
                // In production, you might want to log this
            }
        }
    }

    /**
     * Check if there are any handlers for an event type.
     */
    @Synchronized
    fun hasHandlers(eventType: EventType): Boolean {
        return handlers[eventType]?.isNotEmpty() == true
    }

    /**
     * Remove all handlers for all events.
     */
    @Synchronized
    fun clear() {
        handlers.clear()
    }
}
