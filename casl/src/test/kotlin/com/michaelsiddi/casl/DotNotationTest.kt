package com.michaelsiddi.casl

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for dot notation support in conditions
 * Based on casl-js ability.spec.js dot notation tests
 */
class DotNotationTest {

    @Test
    fun `allows dot notation for nested object access`() {
        val ability = Ability.fromRules(listOf(
            RawRule(
                action = "update",
                subject = "Post",
                conditions = mapOf("author.id" to 123)
            )
        ))

        val myPost = subject("Post",
            "author" to mapOf("id" to 123, "name" to "John")
        )
        val otherPost = subject("Post",
            "author" to mapOf("id" to 456, "name" to "Jane")
        )

        assertTrue(ability.can("update", myPost))
        assertFalse(ability.can("update", otherPost))
    }

    @Test
    fun `allows dot notation for array index access`() {
        // Check if first author doesn't exist (empty array)
        val ability = Ability.fromRules(listOf(
            RawRule(
                action = "delete",
                subject = "Post",
                conditions = mapOf("authors.0" to mapOf("\$exists" to false))
            )
        ))

        val emptyAuthors = subject("Post", "authors" to emptyList<Any>())
        val withAuthors = subject("Post", "authors" to listOf("me", "other"))

        // Empty array: authors.0 doesn't exist, so $exists: false should match
        assertTrue(ability.can("delete", emptyAuthors), "Should allow delete when authors.0 doesn't exist")
        // Non-empty array: authors.0 exists, so $exists: false should NOT match
        assertFalse(ability.can("delete", withAuthors), "Should not allow delete when authors.0 exists")
    }

    @Test
    fun `allows dot notation with operators`() {
        val ability = Ability.fromRules(listOf(
            RawRule(
                action = "read",
                subject = "Post",
                conditions = mapOf("author.role" to mapOf("\$in" to listOf("admin", "editor")))
            )
        ))

        val adminPost = subject("Post",
            "author" to mapOf("role" to "admin")
        )
        val userPost = subject("Post",
            "author" to mapOf("role" to "user")
        )

        assertTrue(ability.can("read", adminPost))
        assertFalse(ability.can("read", userPost))
    }

    @Test
    fun `allows deeply nested dot notation`() {
        val ability = Ability.fromRules(listOf(
            RawRule(
                action = "update",
                subject = "Post",
                conditions = mapOf("metadata.author.permissions.canEdit" to true)
            )
        ))

        val editablePost = subject("Post",
            "metadata" to mapOf(
                "author" to mapOf(
                    "permissions" to mapOf(
                        "canEdit" to true
                    )
                )
            )
        )
        val readOnlyPost = subject("Post",
            "metadata" to mapOf(
                "author" to mapOf(
                    "permissions" to mapOf(
                        "canEdit" to false
                    )
                )
            )
        )

        assertTrue(ability.can("update", editablePost))
        assertFalse(ability.can("update", readOnlyPost))
    }

    @Test
    fun `dot notation with array element fields`() {
        val ability = Ability.fromRules(listOf(
            RawRule(
                action = "update",
                subject = "Post",
                conditions = mapOf("comments.0.author" to "Ted")
            )
        ))

        val tedPost = subject("Post",
            "comments" to listOf(
                mapOf("author" to "Ted", "text" to "Great!"),
                mapOf("author" to "John", "text" to "Nice")
            )
        )
        val johnPost = subject("Post",
            "comments" to listOf(
                mapOf("author" to "John", "text" to "Great!"),
                mapOf("author" to "Ted", "text" to "Nice")
            )
        )

        assertTrue(ability.can("update", tedPost))
        assertFalse(ability.can("update", johnPost))
    }

    @Test
    fun `dot notation returns null for missing paths`() {
        val ability = Ability.fromRules(listOf(
            RawRule(
                action = "read",
                subject = "Post",
                conditions = mapOf("author.bio.description" to mapOf("\$exists" to true))
            )
        ))

        val withBio = subject("Post",
            "author" to mapOf(
                "bio" to mapOf("description" to "A writer")
            )
        )
        val withoutBio = subject("Post",
            "author" to mapOf("name" to "John")
        )

        assertTrue(ability.can("read", withBio))
        assertFalse(ability.can("read", withoutBio))
    }

    @Test
    fun `dot notation with numeric comparisons`() {
        val ability = Ability.fromRules(listOf(
            RawRule(
                action = "delete",
                subject = "Post",
                conditions = mapOf("stats.likes" to mapOf("\$gt" to 100))
            )
        ))

        val popularPost = subject("Post",
            "stats" to mapOf("likes" to 150, "views" to 1000)
        )
        val unpopularPost = subject("Post",
            "stats" to mapOf("likes" to 50, "views" to 100)
        )

        assertTrue(ability.can("delete", popularPost))
        assertFalse(ability.can("delete", unpopularPost))
    }

    @Test
    fun `combines dot notation with regular fields`() {
        val ability = Ability.fromRules(listOf(
            RawRule(
                action = "update",
                subject = "Post",
                conditions = mapOf(
                    "published" to true,
                    "author.id" to 123
                )
            )
        ))

        val validPost = subject("Post",
            "published" to true,
            "author" to mapOf("id" to 123)
        )
        val unpublishedPost = subject("Post",
            "published" to false,
            "author" to mapOf("id" to 123)
        )
        val wrongAuthor = subject("Post",
            "published" to true,
            "author" to mapOf("id" to 456)
        )

        assertTrue(ability.can("update", validPost))
        assertFalse(ability.can("update", unpublishedPost))
        assertFalse(ability.can("update", wrongAuthor))
    }
}
