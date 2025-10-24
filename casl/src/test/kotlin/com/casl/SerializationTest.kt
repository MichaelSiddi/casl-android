package com.casl

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for JSON serialization and deserialization
 * Matches iOS SerializationTests.swift
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SerializationTest {

    @Test
    fun `RawRule encoding simple`() {
        val rule = RawRule("read", "BlogPost")
        val json = rule.toJson()

        assertTrue(json.contains("\"action\""), "Should encode action")
        assertTrue(json.contains("\"subject\""), "Should encode subject")
    }

    @Test
    fun `RawRule decoding simple`() {
        val json = """
        {
            "action": "read",
            "subject": "BlogPost"
        }
        """

        val rule = RawRule.fromJson(json)

        assertEquals("read", rule.action)
        assertEquals("BlogPost", rule.subject)
    }

    @Test
    fun `RawRule with conditions`() {
        val rule = RawRule(
            action = "update",
            subject = "BlogPost",
            conditions = mapOf("authorId" to "user123"),
            inverted = false
        )

        val json = rule.toJson()
        val decoded = RawRule.fromJson(json)

        assertEquals("update", decoded.action)
        assertEquals("BlogPost", decoded.subject)
        assertNotNull(decoded.conditions)
        assertEquals("user123", decoded.conditions!!["authorId"])
    }

    @Test
    fun `RawRule with fields`() {
        val rule = RawRule(
            action = "read",
            subject = "User",
            fields = listOf("name", "email")
        )

        val json = rule.toJson()
        val decoded = RawRule.fromJson(json)

        assertEquals(listOf("name", "email"), decoded.fields)
    }

    @Test
    fun `RawRule with inverted flag`() {
        val rule = RawRule("delete", "BlogPost", inverted = true)

        val json = rule.toJson()
        val decoded = RawRule.fromJson(json)

        assertEquals(true, decoded.inverted)
    }

    @Test
    fun `Ability export rules`() {
        val ability = Ability.builder()
            .can("read", "BlogPost")
            .can("update", "BlogPost", mapOf("authorId" to "user123"), null)
            .cannot("delete", "BlogPost")
            .build()

        val rules = ability.exportRules()

        assertEquals(3, rules.size)
        assertEquals("read", rules[0].action)
        assertEquals("update", rules[1].action)
        assertEquals("delete", rules[2].action)
        assertEquals(true, rules[2].inverted)
    }

    @Test
    fun `Ability export and import`() {
        val original = Ability.builder()
            .can("read", "BlogPost")
            .can("update", "BlogPost", mapOf("authorId" to "user123"), null)
            .build()

        // Export
        val rules = original.exportRules()

        // Serialize
        val json = RawRule.listToJson(rules)

        // Deserialize
        val decodedRules = RawRule.listFromJson(json)

        // Import
        val imported = Ability(decodedRules.map { it.toRule() })

        // Verify behavior is identical
        assertTrue(imported.can("read", "BlogPost"))
    }

    @Test
    fun `list serialization`() {
        val rules = listOf(
            RawRule("read", "BlogPost"),
            RawRule("update", "BlogPost", mapOf("authorId" to "user123"))
        )

        val json = RawRule.listToJson(rules)
        val decoded = RawRule.listFromJson(json)

        assertEquals(2, decoded.size)
        assertEquals("read", decoded[0].action)
        assertEquals("update", decoded[1].action)
    }

    @Test
    fun `nested conditions serialization`() {
        val rule = RawRule(
            action = "access",
            subject = "Resource",
            conditions = mapOf(
                "metadata" to mapOf(
                    "owner" to mapOf(
                        "id" to "user123"
                    )
                )
            )
        )

        val json = rule.toJson()
        val decoded = RawRule.fromJson(json)

        assertNotNull(decoded.conditions)
        assertTrue(decoded.conditions!!.containsKey("metadata"))
    }
}
