package com.michaelsiddi.casl

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for field-level permissions
 * Based on casl-js ability.spec.js "per field abilities" section
 */
class FieldPermissionsTest {

    @Test
    fun `allows to define per field rules`() {
        val ability = Ability.builder()
            .can("read", "Post", "title")
            .build()

        assertTrue(ability.can("read", "Post"))
        assertTrue(ability.can("read", "Post", "title"))
        assertFalse(ability.can("read", "Post", "description"))
    }

    @Test
    fun `allows to define rules for several fields`() {
        val ability = Ability.builder()
            .can("read", "Post", listOf("title", "id"))
            .build()

        assertTrue(ability.can("read", "Post"))
        assertTrue(ability.can("read", "Post", "title"))
        assertTrue(ability.can("read", "Post", "id"))
        assertFalse(ability.can("read", "Post", "description"))
    }

    @Test
    fun `allows to define inverted rules for a field`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .cannot("read", "Post", "description")
            .build()

        assertTrue(ability.can("read", "Post"))
        assertTrue(ability.can("read", "Post", "title"))
        assertFalse(ability.can("read", "Post", "description"))
    }

    @Test
    fun `allows to perform actions on all attributes if none is specified`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .build()

        assertTrue(ability.can("read", "Post", "title"))
        assertTrue(ability.can("read", "Post", "description"))
        assertTrue(ability.can("read", "Post", "anyField"))
    }

    @Test
    fun `field restrictions work with conditions`() {
        val ability = Ability.fromRules(listOf(
            RawRule(
                action = "read",
                subject = "Post",
                fields = listOf("title", "content"),
                conditions = mapOf("published" to true)
            )
        ))

        val publishedPost = subject("Post", "published" to true)
        val draftPost = subject("Post", "published" to false)

        // Published post - can read specified fields
        assertTrue(ability.can("read", publishedPost, "title"))
        assertTrue(ability.can("read", publishedPost, "content"))
        assertFalse(ability.can("read", publishedPost, "secret"))

        // Draft post - cannot read any fields
        assertFalse(ability.can("read", draftPost, "title"))
        assertFalse(ability.can("read", draftPost, "content"))
    }

    @Test
    fun `field-level check without field parameter checks if at least one field accessible`() {
        val ability = Ability.builder()
            .can("read", "Post", "title")
            .build()

        // Should return true because at least one field (title) is accessible
        assertTrue(ability.can("read", "Post"))
    }

    @Test
    fun `multiple field rules combine correctly`() {
        val ability = Ability.builder()
            .can("read", "Post", "title")
            .can("read", "Post", "author")
            .can("update", "Post", listOf("content", "tags"))
            .build()

        // Read permissions
        assertTrue(ability.can("read", "Post", "title"))
        assertTrue(ability.can("read", "Post", "author"))
        assertFalse(ability.can("read", "Post", "content"))

        // Update permissions
        assertTrue(ability.can("update", "Post", "content"))
        assertTrue(ability.can("update", "Post", "tags"))
        assertFalse(ability.can("update", "Post", "title"))
    }

    @Test
    fun `field restrictions with inverted rules and last-match-wins`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .cannot("read", "Post", listOf("password", "secret"))
            .build()

        assertTrue(ability.can("read", "Post", "title"))
        assertTrue(ability.can("read", "Post", "content"))
        assertFalse(ability.can("read", "Post", "password"))
        assertFalse(ability.can("read", "Post", "secret"))
    }

    @Test
    fun `can override field restrictions with later rules`() {
        val ability = Ability.builder()
            .cannot("read", "Post", "admin")
            .can("read", "Post", "adminField")
            .build()

        // Later rule overrides
        assertTrue(ability.can("read", "Post", "adminField"))
    }
}
