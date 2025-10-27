package com.michaelsiddi.casl.extra

import com.michaelsiddi.casl.RawRule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for packRules/unpackRules utilities.
 * Based on casl-js pack_rules.spec.ts
 */
class PackRulesTest {

    @Test
    fun `packRules converts array of rule objects to array of rule arrays`() {
        val rules = listOf(
            RawRule(action = "read", subject = "Post"),
            RawRule(action = "delete", subject = "Post")
        )

        val packed = packRules(rules)

        assertTrue(packed.all { it is List<*> })
    }

    @Test
    fun `packRules puts actions as 1st element of rule array`() {
        val rules = listOf(RawRule(action = "read", subject = "Post"))

        val packed = packRules(rules)

        assertEquals("read", packed[0][0])
        assertEquals(2, packed[0].size)
    }

    @Test
    fun `packRules joins actions with comma if value is an array`() {
        val rules = listOf(RawRule(action = listOf("read", "update"), subject = "Post"))

        val packed = packRules(rules)

        assertEquals("read,update", packed[0][0])
        assertEquals(2, packed[0].size)
    }

    @Test
    fun `packRules puts subject as 2nd element of rule array`() {
        val rules = listOf(RawRule(action = "read", subject = "Post"))

        val packed = packRules(rules)

        assertEquals("Post", packed[0][1])
    }

    @Test
    fun `packRules adds conditions as 3rd element when present`() {
        val conditions = mapOf("published" to true)
        val rules = listOf(RawRule(action = "read", subject = "Post", conditions = conditions))

        val packed = packRules(rules)

        assertEquals(conditions, packed[0][2])
    }

    @Test
    fun `packRules adds inverted flag as 4th element when true`() {
        val rules = listOf(RawRule(action = "read", subject = "Post", inverted = true))

        val packed = packRules(rules)

        assertEquals(1, packed[0][3])
    }

    @Test
    fun `packRules adds fields as 5th element when present`() {
        val rules = listOf(RawRule(action = "read", subject = "Post", fields = listOf("title", "content")))

        val packed = packRules(rules)

        assertEquals("title,content", packed[0][4])
    }

    @Test
    fun `packRules removes trailing falsy values`() {
        val rules = listOf(
            RawRule(action = "read", subject = "Post"),  // No conditions, inverted, or fields
            RawRule(action = "read", subject = "Post", inverted = false)  // inverted is false
        )

        val packed = packRules(rules)

        assertEquals(2, packed[0].size)  // Only action and subject
        assertEquals(2, packed[1].size)  // inverted=false is removed
    }

    @Test
    fun `unpackRules converts packed rules back to RawRule objects`() {
        val packed = listOf(
            listOf("read,update", "Post"),
            listOf("delete", "Post", 0, 1)
        )

        val rules = unpackRules(packed)

        assertEquals(2, rules.size)
        assertEquals(listOf("read", "update"), rules[0].action)
        assertEquals("Post", rules[0].subject)
        assertEquals("delete", rules[1].action)
        assertTrue(rules[1].inverted)
    }

    @Test
    fun `unpackRules handles conditions`() {
        val conditions = mapOf("published" to true)
        val packed = listOf(
            listOf("read", "Post", conditions)
        )

        val rules = unpackRules(packed)

        assertEquals(conditions, rules[0].conditions)
    }

    @Test
    fun `unpackRules handles fields`() {
        val packed = listOf(
            listOf("read", "Post", 0, 0, "title,content")
        )

        val rules = unpackRules(packed)

        assertEquals(listOf("title", "content"), rules[0].fields)
    }

    @Test
    fun `pack and unpack round trip preserves rules`() {
        val originalRules = listOf(
            RawRule(action = listOf("read", "update"), subject = "Post"),
            RawRule(action = "delete", subject = "Post", inverted = true),
            RawRule(
                action = "update",
                subject = "Post",
                conditions = mapOf("authorId" to 123),
                fields = listOf("title", "content")
            )
        )

        val packed = packRules(originalRules)
        val unpacked = unpackRules(packed)

        assertEquals(originalRules.size, unpacked.size)
        assertEquals(originalRules[0].action, unpacked[0].action)
        assertEquals(originalRules[1].inverted, unpacked[1].inverted)
        assertEquals(originalRules[2].conditions, unpacked[2].conditions)
        assertEquals(originalRules[2].fields, unpacked[2].fields)
    }

    @Test
    fun `packRules with custom packSubject function`() {
        val rules = listOf(RawRule(action = "read", subject = "Post"))

        val packed = packRules(rules) { subjectType ->
            "prefix_$subjectType"
        }

        assertEquals("prefix_Post", packed[0][1])
    }

    @Test
    fun `unpackRules with custom unpackSubject function`() {
        val packed = listOf(listOf("read", "prefix_Post"))

        val rules = unpackRules(packed) { subjectStr ->
            subjectStr.removePrefix("prefix_")
        }

        assertEquals("Post", rules[0].subject)
    }
}
