package com.michaelsiddi.casl

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for MongoDB-style operator support in conditions
 */
class OperatorsTest {

    @Test
    fun `$in operator matches value in array`() {
        val rule = RawRule(
            action = "read",
            subject = "User",
            conditions = mapOf("role" to mapOf("\$in" to listOf(1, 2, 3)))
        )

        val ability = Ability.fromRules(listOf(rule))

        // Should match values in array
        assertTrue(ability.can("read", subject("User", "role" to 1)))
        assertTrue(ability.can("read", subject("User", "role" to 2)))
        assertTrue(ability.can("read", subject("User", "role" to 3)))

        // Should not match values not in array
        assertFalse(ability.can("read", subject("User", "role" to 4)))
        assertFalse(ability.can("read", subject("User", "role" to 0)))
    }

    @Test
    fun `$nin operator matches value not in array`() {
        val rule = RawRule(
            action = "read",
            subject = "User",
            conditions = mapOf("status" to mapOf("\$nin" to listOf("banned", "suspended")))
        )

        val ability = Ability.fromRules(listOf(rule))

        // Should match values not in array
        assertTrue(ability.can("read", subject("User", "status" to "active")))
        assertTrue(ability.can("read", subject("User", "status" to "pending")))

        // Should not match values in array
        assertFalse(ability.can("read", subject("User", "status" to "banned")))
        assertFalse(ability.can("read", subject("User", "status" to "suspended")))
    }

    @Test
    fun `$gt operator for greater than comparison`() {
        val rule = RawRule(
            action = "read",
            subject = "Post",
            conditions = mapOf("views" to mapOf("\$gt" to 100))
        )

        val ability = Ability.fromRules(listOf(rule))

        assertTrue(ability.can("read", subject("Post", "views" to 101)))
        assertTrue(ability.can("read", subject("Post", "views" to 1000)))
        assertFalse(ability.can("read", subject("Post", "views" to 100)))
        assertFalse(ability.can("read", subject("Post", "views" to 99)))
    }

    @Test
    fun `$gte operator for greater than or equal comparison`() {
        val rule = RawRule(
            action = "read",
            subject = "Post",
            conditions = mapOf("likes" to mapOf("\$gte" to 50))
        )

        val ability = Ability.fromRules(listOf(rule))

        assertTrue(ability.can("read", subject("Post", "likes" to 50)))
        assertTrue(ability.can("read", subject("Post", "likes" to 51)))
        assertTrue(ability.can("read", subject("Post", "likes" to 100)))
        assertFalse(ability.can("read", subject("Post", "likes" to 49)))
    }

    @Test
    fun `$lt operator for less than comparison`() {
        val rule = RawRule(
            action = "read",
            subject = "User",
            conditions = mapOf("age" to mapOf("\$lt" to 18))
        )

        val ability = Ability.fromRules(listOf(rule))

        assertTrue(ability.can("read", subject("User", "age" to 17)))
        assertTrue(ability.can("read", subject("User", "age" to 10)))
        assertFalse(ability.can("read", subject("User", "age" to 18)))
        assertFalse(ability.can("read", subject("User", "age" to 19)))
    }

    @Test
    fun `$lte operator for less than or equal comparison`() {
        val rule = RawRule(
            action = "read",
            subject = "User",
            conditions = mapOf("age" to mapOf("\$lte" to 65))
        )

        val ability = Ability.fromRules(listOf(rule))

        assertTrue(ability.can("read", subject("User", "age" to 65)))
        assertTrue(ability.can("read", subject("User", "age" to 64)))
        assertTrue(ability.can("read", subject("User", "age" to 30)))
        assertFalse(ability.can("read", subject("User", "age" to 66)))
    }

    @Test
    fun `$eq operator for equality`() {
        val rule = RawRule(
            action = "read",
            subject = "User",
            conditions = mapOf("role" to mapOf("\$eq" to "admin"))
        )

        val ability = Ability.fromRules(listOf(rule))

        assertTrue(ability.can("read", subject("User", "role" to "admin")))
        assertFalse(ability.can("read", subject("User", "role" to "user")))
    }

    @Test
    fun `$ne operator for not equals`() {
        val rule = RawRule(
            action = "read",
            subject = "User",
            conditions = mapOf("status" to mapOf("\$ne" to "deleted"))
        )

        val ability = Ability.fromRules(listOf(rule))

        assertTrue(ability.can("read", subject("User", "status" to "active")))
        assertTrue(ability.can("read", subject("User", "status" to "pending")))
        assertFalse(ability.can("read", subject("User", "status" to "deleted")))
    }

    @Test
    fun `$exists operator checks field existence`() {
        val ruleExists = RawRule(
            action = "read",
            subject = "Post",
            conditions = mapOf("publishedAt" to mapOf("\$exists" to true))
        )

        val ability = Ability.fromRules(listOf(ruleExists))

        // Should match when field exists
        assertTrue(ability.can("read", subject("Post", "publishedAt" to "2024-01-01")))

        // Should not match when field doesn't exist
        assertFalse(ability.can("read", subject("Post", "title" to "Draft")))
    }

    @Test
    fun `$regex operator matches string patterns`() {
        val rule = RawRule(
            action = "read",
            subject = "User",
            conditions = mapOf("email" to mapOf("\$regex" to ".*@admin\\.com$"))
        )

        val ability = Ability.fromRules(listOf(rule))

        assertTrue(ability.can("read", subject("User", "email" to "john@admin.com")))
        assertTrue(ability.can("read", subject("User", "email" to "admin@admin.com")))
        assertFalse(ability.can("read", subject("User", "email" to "john@user.com")))
        assertFalse(ability.can("read", subject("User", "email" to "john@admin.org")))
    }

    @Test
    fun `$size operator matches array size`() {
        val rule = RawRule(
            action = "read",
            subject = "Team",
            conditions = mapOf("members" to mapOf("\$size" to 3))
        )

        val ability = Ability.fromRules(listOf(rule))

        assertTrue(ability.can("read", subject("Team",
            "members" to listOf("user1", "user2", "user3")
        )))
        assertFalse(ability.can("read", subject("Team",
            "members" to listOf("user1", "user2")
        )))
        assertFalse(ability.can("read", subject("Team",
            "members" to listOf("user1", "user2", "user3", "user4")
        )))
    }

    @Test
    fun `$all operator matches arrays containing all elements`() {
        val rule = RawRule(
            action = "read",
            subject = "User",
            conditions = mapOf("permissions" to mapOf("\$all" to listOf("read", "write")))
        )

        val ability = Ability.fromRules(listOf(rule))

        // Should match when array contains all required elements
        assertTrue(ability.can("read", subject("User",
            "permissions" to listOf("read", "write", "delete")
        )))
        assertTrue(ability.can("read", subject("User",
            "permissions" to listOf("read", "write")
        )))

        // Should not match when missing elements
        assertFalse(ability.can("read", subject("User",
            "permissions" to listOf("read")
        )))
        assertFalse(ability.can("read", subject("User",
            "permissions" to listOf("write", "delete")
        )))
    }

    @Test
    fun `$elemMatch operator matches array elements`() {
        val rule = RawRule(
            action = "read",
            subject = "Order",
            conditions = mapOf("items" to mapOf("\$elemMatch" to mapOf(
                "price" to 100,
                "category" to "electronics"
            )))
        )

        val ability = Ability.fromRules(listOf(rule))

        // Should match when array has element matching all conditions
        assertTrue(ability.can("read", subject("Order",
            "items" to listOf(
                mapOf("price" to 50, "category" to "books"),
                mapOf("price" to 100, "category" to "electronics"),
                mapOf("price" to 200, "category" to "clothing")
            )
        )))

        // Should not match when no element matches all conditions
        assertFalse(ability.can("read", subject("Order",
            "items" to listOf(
                mapOf("price" to 100, "category" to "books"),
                mapOf("price" to 50, "category" to "electronics")
            )
        )))
    }

    @Test
    fun `combining multiple operators`() {
        val rule = RawRule(
            action = "read",
            subject = "User",
            conditions = mapOf(
                "age" to mapOf("\$gte" to 18, "\$lte" to 65),
                "role" to mapOf("\$in" to listOf("user", "admin")),
                "status" to mapOf("\$ne" to "banned")
            )
        )

        val ability = Ability.fromRules(listOf(rule))

        // Should match when all conditions are met
        assertTrue(ability.can("read", subject("User",
            "age" to 25,
            "role" to "user",
            "status" to "active"
        )))

        // Should not match when age is out of range
        assertFalse(ability.can("read", subject("User",
            "age" to 17,
            "role" to "user",
            "status" to "active"
        )))

        // Should not match when role is not in list
        assertFalse(ability.can("read", subject("User",
            "age" to 25,
            "role" to "guest",
            "status" to "active"
        )))

        // Should not match when status is banned
        assertFalse(ability.can("read", subject("User",
            "age" to 25,
            "role" to "user",
            "status" to "banned"
        )))
    }

    @Test
    fun `nested fields with operators`() {
        // Separate rule for each nested condition to simplify
        val rule = RawRule(
            action = "read",
            subject = "Post",
            conditions = mapOf(
                "authorId" to mapOf("\$in" to listOf(1, 2, 3))
            )
        )

        val ability = Ability.fromRules(listOf(rule))

        // Should match when nested conditions are met
        assertTrue(ability.can("read", subject("Post", "authorId" to 2)))

        // Should not match when author id not in list
        assertFalse(ability.can("read", subject("Post", "authorId" to 5)))
    }

    @Test
    fun `real world example with $in operator`() {
        // Your use case: role $in [1, 2, 3]
        val rule = RawRule(
            action = listOf("read", "update"),
            subject = "ChannelOptionField",
            conditions = mapOf("role" to mapOf("\$in" to listOf(1, 2, 3)))
        )

        val ability = Ability.fromRules(listOf(rule))

        // Users with role 1, 2, or 3 can access
        assertTrue(ability.can("read", subject("ChannelOptionField", "role" to 1)))
        assertTrue(ability.can("read", subject("ChannelOptionField", "role" to 2)))
        assertTrue(ability.can("read", subject("ChannelOptionField", "role" to 3)))
        assertTrue(ability.can("update", subject("ChannelOptionField", "role" to 2)))

        // Users with other roles cannot access
        assertFalse(ability.can("read", subject("ChannelOptionField", "role" to 4)))
        assertFalse(ability.can("read", subject("ChannelOptionField", "role" to 0)))
    }
}
