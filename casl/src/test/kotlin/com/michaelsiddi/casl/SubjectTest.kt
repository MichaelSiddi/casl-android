package com.michaelsiddi.casl

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for subject() helper function and ForcedSubject
 */
class SubjectTest {

    @Test
    fun `subject creates ForcedSubject with type and attributes`() {
        val user = subject("User", mapOf(
            "id" to 123,
            "role" to "admin"
        ))

        assertEquals("User", user.getSubjectType())
        assertEquals(123, user["id"])
        assertEquals("admin", user["role"])
    }

    @Test
    fun `subject with vararg pairs creates ForcedSubject`() {
        val user = subject("User",
            "id" to 123,
            "role" to "admin"
        )

        assertEquals("User", user.getSubjectType())
        assertEquals(123, user["id"])
        assertEquals("admin", user["role"])
    }

    @Test
    fun `subject with empty attributes`() {
        val user = subject("User")

        assertEquals("User", user.getSubjectType())
        assertEquals(null, user["id"])
    }

    @Test
    fun `SubjectTypeDetector detects ForcedSubject type`() {
        val user = subject("ChannelOptionField", mapOf("id" to 1))

        val detectedType = SubjectTypeDetector.detectType(user)

        assertEquals("ChannelOptionField", detectedType)
    }

    @Test
    fun `ability can check permissions on ForcedSubject`() {
        val rawRule = RawRule(
            action = "read",
            subject = "ChannelOptionField"
        )

        val ability = Ability.fromRules(listOf(rawRule))
        val channelOption = subject("ChannelOptionField", "id" to 1, "name" to "test")

        assertTrue(ability.can("read", channelOption), "Should allow read on ChannelOptionField")
        assertFalse(ability.can("delete", channelOption), "Should not allow delete on ChannelOptionField")
    }

    @Test
    fun `ability checks conditions on ForcedSubject attributes`() {
        val rawRule = RawRule(
            action = "update",
            subject = "User",
            conditions = mapOf("role" to "admin")
        )

        val ability = Ability.fromRules(listOf(rawRule))

        // Admin user should be able to update
        val adminUser = subject("User", "id" to 1, "role" to "admin")
        assertTrue(ability.can("update", adminUser), "Admin should be able to update")

        // Regular user should not be able to update
        val regularUser = subject("User", "id" to 2, "role" to "user")
        assertFalse(ability.can("update", regularUser), "Regular user should not be able to update")
    }

    @Test
    fun `ForcedSubject has() method checks attribute existence`() {
        val user = subject("User", "id" to 123)

        assertTrue(user.has("id"))
        assertFalse(user.has("name"))
    }

    @Test
    fun `ForcedSubject toMap() returns all attributes`() {
        val user = subject("User",
            "id" to 123,
            "name" to "John",
            "role" to "admin"
        )

        val map = user.toMap()

        assertEquals(3, map.size)
        assertEquals(123, map["id"])
        assertEquals("John", map["name"])
        assertEquals("admin", map["role"])
    }

    @Test
    fun `ability checks permissions with nested conditions on ForcedSubject`() {
        val rawRule = RawRule(
            action = "delete",
            subject = "Post",
            conditions = mapOf(
                "author" to mapOf("id" to 123)
            )
        )

        val ability = Ability.fromRules(listOf(rawRule))

        // Post by author 123 should be deletable
        val ownPost = subject("Post",
            "id" to 1,
            "author" to mapOf("id" to 123)
        )
        assertTrue(ability.can("delete", ownPost), "Should allow delete own post")

        // Post by another author should not be deletable
        val otherPost = subject("Post",
            "id" to 2,
            "author" to mapOf("id" to 456)
        )
        assertFalse(ability.can("delete", otherPost), "Should not allow delete other's post")
    }

    @Test
    fun `real world example - channel option field permissions`() {
        // Setup rules similar to your use case
        val rules = listOf(
            RawRule(
                action = listOf("read", "update"),
                subject = "ChannelOptionField",
                conditions = mapOf("userId" to 123)
            )
        )

        val ability = Ability.fromRules(rules)

        // User 123's channel option field
        val myOption = subject("ChannelOptionField",
            "id" to 1,
            "userId" to 123,
            "value" to "something"
        )

        assertTrue(ability.can("read", myOption), "Should allow read own option")
        assertTrue(ability.can("update", myOption), "Should allow update own option")
        assertFalse(ability.can("delete", myOption), "Should not allow delete (not in rules)")

        // Another user's channel option field
        val otherOption = subject("ChannelOptionField",
            "id" to 2,
            "userId" to 456,
            "value" to "something else"
        )

        assertFalse(ability.can("read", otherOption), "Should not allow read other's option")
        assertFalse(ability.can("update", otherOption), "Should not allow update other's option")
    }
}
