package com.michaelsiddi.casl.extra

import com.michaelsiddi.casl.Ability
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for rulesToQuery utility.
 * Based on casl-js rulesToQuery.spec.js
 */
class RulesToQueryTest {

    @Test
    fun `returns null when user has no permissions`() {
        val ability = Ability.builder()
            .cannot("read", "Post")
            .build()

        val query = rulesToQuery(ability, "read", "Post") { rule ->
            rule.conditions ?: emptyMap()
        }

        assertNull(query)
    }

    @Test
    fun `returns empty object when user can read all`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .build()

        val query = rulesToQuery(ability, "read", "Post") { rule ->
            rule.conditions ?: emptyMap()
        }

        assertNotNull(query)
        assertNull(query.`$or`)
        assertNull(query.`$and`)
    }

    @Test
    fun `returns $or query with single condition`() {
        val ability = Ability.builder()
            .can("read", "Post", mapOf("published" to true))
            .build()

        val query = rulesToQuery(ability, "read", "Post") { rule ->
            rule.conditions ?: emptyMap()
        }

        assertNotNull(query)
        assertNotNull(query.`$or`)
        assertEquals(1, query.`$or`!!.size)
        assertEquals(mapOf("published" to true), query.`$or`!![0])
    }

    @Test
    fun `returns $or query with multiple conditions`() {
        val ability = Ability.builder()
            .can("read", "Post", mapOf("published" to true))
            .can("read", "Post", mapOf("authorId" to 123))
            .build()

        val query = rulesToQuery(ability, "read", "Post") { rule ->
            rule.conditions ?: emptyMap()
        }

        assertNotNull(query)
        assertNotNull(query.`$or`)
        assertEquals(2, query.`$or`!!.size)
    }

    @Test
    fun `includes $and for inverted rules`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .cannot("read", "Post", mapOf("draft" to true))
            .build()

        val query = rulesToMongoQuery(ability, "read", "Post")

        assertNotNull(query)
        assertTrue(query.containsKey("\$and"))
    }

    @Test
    fun `stops processing on inverted rule without conditions`() {
        val ability = Ability.builder()
            .can("read", "Post", mapOf("id" to 2))
            .cannot("read", "Post")
            .can("read", "Post", mapOf("id" to 5))
            .build()

        val query = rulesToQuery(ability, "read", "Post") { rule ->
            rule.conditions ?: emptyMap()
        }

        assertNotNull(query)
        assertNotNull(query.`$or`)
        assertEquals(1, query.`$or`!!.size)  // Only first rule before cannot
    }

    @Test
    fun `allows all when regular rule without conditions appears`() {
        val ability = Ability.builder()
            .can("read", "Post", mapOf("id" to 1))
            .can("read", "Post")
            .cannot("read", "Post", mapOf("status" to "draft"))
            .build()

        val query = rulesToQuery(ability, "read", "Post") { rule ->
            rule.conditions ?: emptyMap()
        }

        assertNotNull(query)
        // Should have $and for the cannot rule but no $or since "can all" clears conditions
        assertNull(query.`$or`)
        assertNotNull(query.`$and`)
    }

    @Test
    fun `rulesToMongoQuery returns proper MongoDB format`() {
        val ability = Ability.builder()
            .can("read", "Post", mapOf("published" to true))
            .can("read", "Post", mapOf("authorId" to 123))
            .build()

        val query = rulesToMongoQuery(ability, "read", "Post")

        assertNotNull(query)
        assertTrue(query.containsKey("\$or"))

        @Suppress("UNCHECKED_CAST")
        val orConditions = query["\$or"] as List<Map<String, Any?>>
        assertEquals(2, orConditions.size)
    }

    @Test
    fun `rulesToMongoQuery inverts conditions for cannot rules`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .cannot("read", "Post", mapOf("draft" to true))
            .build()

        val query = rulesToMongoQuery(ability, "read", "Post")

        assertNotNull(query)
        assertTrue(query.containsKey("\$and"))

        @Suppress("UNCHECKED_CAST")
        val andConditions = query["\$and"] as List<Map<String, Any?>>
        assertEquals(1, andConditions.size)

        // Should have $ne wrapper
        val condition = andConditions[0]
        assertTrue(condition.containsKey("draft"))

        @Suppress("UNCHECKED_CAST")
        val draftCondition = condition["draft"] as Map<String, Any?>
        assertEquals(mapOf("\$ne" to true), draftCondition)
    }

    @Test
    fun `returns null for no permissions`() {
        val ability = Ability.builder().build()

        val query = rulesToMongoQuery(ability, "read", "Post")

        assertNull(query)
    }

    @Test
    fun `returns empty map for allow all`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .build()

        val query = rulesToMongoQuery(ability, "read", "Post")

        assertNotNull(query)
        assertTrue(query.isEmpty())
    }
}
