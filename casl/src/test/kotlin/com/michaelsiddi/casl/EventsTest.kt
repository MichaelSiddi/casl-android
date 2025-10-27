package com.michaelsiddi.casl

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for ability event system.
 * Based on casl-js event system tests.
 */
class EventsTest {

    @Test
    fun `triggers UPDATE event before rules are updated`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .build()

        var eventFired = false
        var eventRules: List<RawRule>? = null
        var canReadBefore = false

        ability.on(EventType.UPDATE) { event ->
            eventFired = true
            eventRules = event.rules
            canReadBefore = ability.can("delete", subject("Post", "id" to 1))
        }

        val newRules = listOf(RawRule(action = "delete", subject = "Post"))
        ability.update(newRules)

        assertTrue(eventFired, "UPDATE event should fire")
        assertEquals(newRules, eventRules)
        assertFalse(canReadBefore, "Old rules should still be active during UPDATE event")
    }

    @Test
    fun `triggers UPDATED event after rules have been updated`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .build()

        var eventFired = false
        var eventRules: List<RawRule>? = null
        var canDeleteAfter = false

        ability.on(EventType.UPDATED) { event ->
            eventFired = true
            eventRules = event.rules
            canDeleteAfter = ability.can("delete", subject("Post", "id" to 1))
        }

        val newRules = listOf(RawRule(action = "delete", subject = "Post"))
        ability.update(newRules)

        assertTrue(eventFired, "UPDATED event should fire")
        assertEquals(newRules, eventRules)
        assertTrue(canDeleteAfter, "New rules should be active during UPDATED event")
    }

    @Test
    fun `allows to unsubscribe from UPDATE event`() {
        val ability = Ability.builder().build()

        var callCount = 0
        val unsubscribe = ability.on(EventType.UPDATE) {
            callCount++
        }

        ability.update(emptyList())
        assertEquals(1, callCount)

        unsubscribe()
        ability.update(emptyList())
        assertEquals(1, callCount, "Handler should not be called after unsubscribe")
    }

    @Test
    fun `allows to unsubscribe from UPDATED event`() {
        val ability = Ability.builder().build()

        var callCount = 0
        val unsubscribe = ability.on(EventType.UPDATED) {
            callCount++
        }

        ability.update(emptyList())
        assertEquals(1, callCount)

        unsubscribe()
        ability.update(emptyList())
        assertEquals(1, callCount, "Handler should not be called after unsubscribe")
    }

    @Test
    fun `calling unsubscribe multiple times does not remove other handlers`() {
        val ability = Ability.builder().build()

        var handler1Calls = 0
        var handler2Calls = 0

        val unsubscribe1 = ability.on(EventType.UPDATE) {
            handler1Calls++
        }
        ability.on(EventType.UPDATE) {
            handler2Calls++
        }

        unsubscribe1()
        unsubscribe1() // Call twice
        ability.update(emptyList())

        assertEquals(0, handler1Calls, "First handler should not be called")
        assertEquals(1, handler2Calls, "Second handler should still be called")
    }

    @Test
    fun `can unsubscribe inside event handler`() {
        val ability = Ability.builder().build()

        var handler1Calls = 0
        var handler2Calls = 0

        lateinit var unsubscribe1: Unsubscribe
        unsubscribe1 = ability.on(EventType.UPDATED) {
            handler1Calls++
            unsubscribe1()
        }
        ability.on(EventType.UPDATED) {
            handler2Calls++
        }

        ability.update(emptyList())
        assertEquals(1, handler1Calls)
        assertEquals(1, handler2Calls)

        ability.update(emptyList())
        assertEquals(1, handler1Calls, "First handler should not be called after self-unsubscribe")
        assertEquals(2, handler2Calls, "Second handler should still be called")
    }

    @Test
    fun `multiple handlers for same event are all called`() {
        val ability = Ability.builder().build()

        var handler1Called = false
        var handler2Called = false
        var handler3Called = false

        ability.on(EventType.UPDATED) { handler1Called = true }
        ability.on(EventType.UPDATED) { handler2Called = true }
        ability.on(EventType.UPDATED) { handler3Called = true }

        ability.update(emptyList())

        assertTrue(handler1Called)
        assertTrue(handler2Called)
        assertTrue(handler3Called)
    }

    @Test
    fun `event includes ability reference`() {
        val ability = Ability.builder().build()

        var eventAbility: Ability? = null

        ability.on(EventType.UPDATED) { event ->
            eventAbility = event.ability
        }

        ability.update(emptyList())

        assertEquals(ability, eventAbility, "Event should include reference to ability")
    }

    @Test
    fun `event includes new rules`() {
        val ability = Ability.builder().build()

        val newRules = listOf(
            RawRule(action = "read", subject = "Post"),
            RawRule(action = "update", subject = "Post")
        )

        var eventRules: List<RawRule>? = null

        ability.on(EventType.UPDATED) { event ->
            eventRules = event.rules
        }

        ability.update(newRules)

        assertEquals(newRules, eventRules)
    }

    @Test
    fun `UPDATE event fires before UPDATED event`() {
        val ability = Ability.builder().build()

        val callOrder = mutableListOf<String>()

        ability.on(EventType.UPDATE) {
            callOrder.add("update")
        }
        ability.on(EventType.UPDATED) {
            callOrder.add("updated")
        }

        ability.update(emptyList())

        assertEquals(listOf("update", "updated"), callOrder)
    }

    @Test
    fun `exception in one handler does not prevent other handlers from running`() {
        val ability = Ability.builder().build()

        var handler2Called = false

        ability.on(EventType.UPDATED) {
            throw RuntimeException("Handler error")
        }
        ability.on(EventType.UPDATED) {
            handler2Called = true
        }

        // Should not throw
        ability.update(emptyList())

        assertTrue(handler2Called, "Second handler should run despite first handler throwing")
    }

    @Test
    fun `can subscribe to both UPDATE and UPDATED events`() {
        val ability = Ability.builder().build()

        var updateCalls = 0
        var updatedCalls = 0

        ability.on(EventType.UPDATE) { updateCalls++ }
        ability.on(EventType.UPDATED) { updatedCalls++ }

        ability.update(emptyList())

        assertEquals(1, updateCalls)
        assertEquals(1, updatedCalls)
    }
}
