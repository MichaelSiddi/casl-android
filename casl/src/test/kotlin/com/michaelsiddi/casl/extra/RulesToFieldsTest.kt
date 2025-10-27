package com.michaelsiddi.casl.extra

import com.michaelsiddi.casl.Ability
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for rulesToFields utility.
 * Based on casl-js rules_to_fields.spec.js
 */
class RulesToFieldsTest {

    @Test
    fun `returns empty map for empty Ability instance`() {
        val ability = Ability.builder().build()
        val fields = rulesToFields(ability, "read", "Post")

        assertTrue(fields.isEmpty())
    }

    @Test
    fun `returns empty map if Ability contains only inverted rules`() {
        val ability = Ability.builder()
            .cannot("read", "Post", mapOf("id" to 5))
            .cannot("read", "Post", mapOf("private" to true))
            .build()

        val fields = rulesToFields(ability, "read", "Post")

        assertTrue(fields.isEmpty())
    }

    @Test
    fun `returns empty map for Ability instance with rules without conditions`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .build()

        val fields = rulesToFields(ability, "read", "Post")

        assertTrue(fields.isEmpty())
    }

    @Test
    fun `extracts field values from direct rule conditions`() {
        val ability = Ability.builder()
            .can("read", "Post", mapOf("id" to 5))
            .can("read", "Post", mapOf("private" to true))
            .build()

        val fields = rulesToFields(ability, "read", "Post")

        assertEquals(
            mapOf("id" to 5, "private" to true),
            fields
        )
    }

    @Test
    fun `correctly sets values for fields declared with dot notation`() {
        val ability = Ability.builder()
            .can("read", "Post", mapOf("id" to 5))
            .can("read", "Post", mapOf("state.private" to true))
            .build()

        val fields = rulesToFields(ability, "read", "Post")

        assertEquals(5, fields["id"])
        @Suppress("UNCHECKED_CAST")
        val state = fields["state"] as? Map<String, Any?>
        assertEquals(true, state?.get("private"))
    }

    @Test
    fun `skips plain object values (MongoDB query expressions)`() {
        val ability = Ability.builder()
            .can("read", "Post", mapOf("state" to mapOf("\$in" to listOf("draft", "review"))))
            .can("read", "Post", mapOf("private" to true))
            .build()

        val fields = rulesToFields(ability, "read", "Post")

        assertEquals(
            mapOf("private" to true),
            fields
        )
    }

    @Test
    fun `handles multiple levels of dot notation`() {
        val ability = Ability.builder()
            .can("read", "Post", mapOf("author.address.city" to "NYC"))
            .can("read", "Post", mapOf("author.address.state" to "NY"))
            .can("read", "Post", mapOf("author.name" to "John"))
            .build()

        val fields = rulesToFields(ability, "read", "Post")

        @Suppress("UNCHECKED_CAST")
        val author = fields["author"] as? Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val address = author?.get("address") as? Map<String, Any?>

        assertEquals("John", author?.get("name"))
        assertEquals("NYC", address?.get("city"))
        assertEquals("NY", address?.get("state"))
    }

    @Test
    fun `handles various primitive types`() {
        val ability = Ability.builder()
            .can("create", "Post", mapOf(
                "authorId" to 123,
                "title" to "Test Post",
                "published" to false,
                "rating" to 4.5
            ))
            .build()

        val fields = rulesToFields(ability, "create", "Post")

        assertEquals(123, fields["authorId"])
        assertEquals("Test Post", fields["title"])
        assertEquals(false, fields["published"])
        assertEquals(4.5, fields["rating"])
    }

    @Test
    fun `handles null values`() {
        val ability = Ability.builder()
            .can("read", "Post", mapOf("deletedAt" to null))
            .build()

        val fields = rulesToFields(ability, "read", "Post")

        assertTrue(fields.containsKey("deletedAt"))
        assertEquals(null, fields["deletedAt"])
    }

    @Test
    fun `later rules override earlier rules for same field`() {
        val ability = Ability.builder()
            .can("read", "Post", mapOf("status" to "draft"))
            .can("read", "Post", mapOf("status" to "published"))
            .build()

        val fields = rulesToFields(ability, "read", "Post")

        assertEquals(
            mapOf("status" to "published"),
            fields
        )
    }

    @Test
    fun `returns empty map for non-matching action`() {
        val ability = Ability.builder()
            .can("read", "Post", mapOf("id" to 5))
            .build()

        val fields = rulesToFields(ability, "update", "Post")

        assertTrue(fields.isEmpty())
    }

    @Test
    fun `returns empty map for non-matching subject type`() {
        val ability = Ability.builder()
            .can("read", "Post", mapOf("id" to 5))
            .build()

        val fields = rulesToFields(ability, "read", "Comment")

        assertTrue(fields.isEmpty())
    }

    @Test
    fun `combines conditions from multiple rules`() {
        val ability = Ability.builder()
            .can("create", "Post", mapOf("authorId" to 123))
            .can("create", "Post", mapOf("organizationId" to 456))
            .can("create", "Post", mapOf("status" to "draft"))
            .build()

        val fields = rulesToFields(ability, "create", "Post")

        assertEquals(
            mapOf(
                "authorId" to 123,
                "organizationId" to 456,
                "status" to "draft"
            ),
            fields
        )
    }
}
