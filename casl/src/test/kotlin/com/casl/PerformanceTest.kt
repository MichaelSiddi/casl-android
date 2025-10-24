package com.casl

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis
import kotlin.test.assertTrue

/**
 * Performance validation tests for CASL Android.
 *
 * Validates success criteria:
 * - SC-002: Permission checks <1ms for 100 rules
 * - SC-003: 1000+ concurrent checks
 * - SC-004: Serialization <10ms for 100 rules
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PerformanceTest {

    @Test
    fun `permission check completes in under 1ms for 100 rules`() {
        // Create ability with 100 rules
        val builder = Ability.builder()
        repeat(100) { i ->
            builder.can("action$i", "Subject$i")
        }
        val ability = builder.build()

        // Create test subject
        data class TestSubject(val id: String)
        val subject = TestSubject("test")

        // Warm up
        repeat(1000) {
            ability.can("action50", subject)
        }

        // Measure 10,000 permission checks
        val iterations = 10_000
        val elapsed = measureTimeMillis {
            repeat(iterations) {
                ability.can("action50", subject)
            }
        }

        val avgTimeMs = elapsed.toDouble() / iterations
        println("Average permission check time: ${avgTimeMs}ms (${iterations} iterations)")
        assertTrue(avgTimeMs < 1.0, "Permission check should complete in <1ms, was ${avgTimeMs}ms")
    }

    @Test
    fun `supports 1000 concurrent permission checks`() {
        val ability = Ability.builder()
            .can("read", "Resource")
            .can("update", "Resource", mapOf("ownerId" to "user123"), null)
            .build()

        data class Resource(val id: String, val ownerId: String)
        val resource = Resource("res1", "user123")

        // Launch 1000 concurrent threads
        val threads = List(1000) {
            Thread {
                repeat(10) {
                    ability.can("read", resource)
                    ability.can("update", resource)
                }
            }
        }

        val elapsed = measureTimeMillis {
            threads.forEach { it.start() }
            threads.forEach { it.join() }
        }

        println("1000 concurrent threads completed in ${elapsed}ms")
        assertTrue(elapsed < 5000, "Concurrent operations should complete in reasonable time")
    }

    @Test
    fun `serialization completes in under 10ms for 100 rules`() {
        // Create 100 rules
        val builder = Ability.builder()
        repeat(100) { i ->
            builder.can("action$i", "Subject$i", mapOf("id" to i), null)
        }
        val ability = builder.build()

        // Warm up
        repeat(100) {
            val rules = ability.exportRules()
            RawRule.listToJson(rules)
        }

        // Measure serialization
        val iterations = 1000
        val elapsed = measureTimeMillis {
            repeat(iterations) {
                val rules = ability.exportRules()
                RawRule.listToJson(rules)
            }
        }

        val avgTimeMs = elapsed.toDouble() / iterations
        println("Average serialization time: ${avgTimeMs}ms (${iterations} iterations)")
        assertTrue(avgTimeMs < 10.0, "Serialization should complete in <10ms, was ${avgTimeMs}ms")
    }

    @Test
    fun `stress test with 10000 concurrent operations`() {
        val ability = Ability.builder()
            .can("read", "Document")
            .can("update", "Document", mapOf("authorId" to "user1"), null)
            .cannot("delete", "Document", mapOf("locked" to true), null)
            .build()

        data class Document(val id: String, val authorId: String, val locked: Boolean)

        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        // Create 100 threads, each performing 100 operations
        val threads = List(100) {
            Thread {
                repeat(100) { i ->
                    try {
                        val doc = Document("doc$i", "user1", i % 2 == 0)
                        ability.can("read", doc)
                        ability.can("update", doc)
                        ability.can("delete", doc)
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        failureCount.incrementAndGet()
                    }
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        println("Stress test completed: ${successCount.get()} successes, ${failureCount.get()} failures")
        assertTrue(failureCount.get() == 0, "No failures expected in stress test")
        assertTrue(successCount.get() == 10_000, "All 10,000 operations should succeed")
    }

    @Test
    fun `deserialization performance matches serialization`() {
        val builder = Ability.builder()
        repeat(100) { i ->
            builder.can("action$i", "Subject$i", mapOf("id" to i, "nested" to mapOf("value" to i * 2)), null)
        }
        val ability = builder.build()

        val rules = ability.exportRules()
        val json = RawRule.listToJson(rules)

        // Warm up
        repeat(100) {
            RawRule.listFromJson(json)
        }

        // Measure deserialization
        val iterations = 1000
        val elapsed = measureTimeMillis {
            repeat(iterations) {
                RawRule.listFromJson(json)
            }
        }

        val avgTimeMs = elapsed.toDouble() / iterations
        println("Average deserialization time: ${avgTimeMs}ms (${iterations} iterations)")
        assertTrue(avgTimeMs < 10.0, "Deserialization should complete in <10ms, was ${avgTimeMs}ms")
    }
}
