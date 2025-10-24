package com.casl

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Edge case and correctness validation tests.
 *
 * Tests:
 * - Null subjects
 * - Missing attributes
 * - Conflicting rules (last-match-wins)
 * - Deep equality matching
 * - Numeric type coercion
 */
class EdgeCaseTest {

    @Test
    fun `null subject returns false for any permission check`() {
        val ability = Ability.builder()
            .can("read", "Resource")
            .build()

        assertFalse(ability.can("read", null))
        assertTrue(ability.cannot("read", null))
    }

    @Test
    fun `missing attribute in condition fails match`() {
        val ability = Ability.builder()
            .can("update", "Resource", mapOf("ownerId" to "user123"), null)
            .build()

        data class Resource(val id: String) // Missing ownerId field

        val resource = Resource("res1")
        assertFalse(ability.can("update", resource))
    }

    @Test
    fun `conflicting rules use last-match-wins precedence`() {
        // First allow, then deny - deny wins
        val ability1 = Ability.builder()
            .can("delete", "Post")
            .cannot("delete", "Post")
            .build()

        data class Post(val id: String)
        val post = Post("post1")

        assertFalse(ability1.can("delete", post), "Last rule (cannot) should win")

        // First deny, then allow - allow wins
        val ability2 = Ability.builder()
            .cannot("delete", "Post")
            .can("delete", "Post")
            .build()

        assertTrue(ability2.can("delete", post), "Last rule (can) should win")
    }

    @Test
    fun `condition-specific rules override general rules`() {
        val ability = Ability.builder()
            .can("update", "Post") // General rule
            .cannot("update", "Post", mapOf("published" to true), null) // Specific rule
            .build()

        data class Post(val id: String, val published: Boolean)

        val draftPost = Post("post1", false)
        val publishedPost = Post("post2", true)

        assertTrue(ability.can("update", draftPost), "Can update draft post")
        assertFalse(ability.can("update", publishedPost), "Cannot update published post")
    }

    @Test
    fun `deep equality matching works for nested conditions`() {
        val ability = Ability.builder()
            .can(
                "access", "Resource",
                mapOf(
                    "metadata" to mapOf(
                        "owner" to mapOf(
                            "id" to "user123",
                            "role" to "admin"
                        )
                    )
                ),
                null
            )
            .build()

        data class Owner(val id: String, val role: String)
        data class Metadata(val owner: Owner)
        data class Resource(val id: String, val metadata: Metadata)

        val matchingResource = Resource(
            "res1",
            Metadata(Owner("user123", "admin"))
        )

        val nonMatchingResource = Resource(
            "res2",
            Metadata(Owner("user456", "admin"))
        )

        assertTrue(ability.can("access", matchingResource), "Nested conditions should match")
        assertFalse(ability.can("access", nonMatchingResource), "Nested conditions should not match")
    }

    @Test
    fun `numeric type coercion works across Int Long Double`() {
        data class Resource(val id: String, val count: Number)

        val ability = Ability.builder()
            .can("access", "Resource", mapOf("count" to 42), null)
            .build()

        val resInt = Resource("r1", 42)
        val resLong = Resource("r2", 42L)
        val resDouble = Resource("r3", 42.0)

        assertTrue(ability.can("access", resInt), "Int should match")
        assertTrue(ability.can("access", resLong), "Long should match via coercion")
        assertTrue(ability.can("access", resDouble), "Double should match via coercion")
    }

    @Test
    fun `list equality works in conditions`() {
        val ability = Ability.builder()
            .can("access", "Resource", mapOf("tags" to listOf("public", "featured")), null)
            .build()

        data class Resource(val id: String, val tags: List<String>)

        val matchingResource = Resource("r1", listOf("public", "featured"))
        val nonMatchingResource = Resource("r2", listOf("public", "archived"))
        val differentOrderResource = Resource("r3", listOf("featured", "public"))

        assertTrue(ability.can("access", matchingResource), "Exact list match should work")
        assertFalse(ability.can("access", nonMatchingResource), "Different list should not match")
        assertFalse(ability.can("access", differentOrderResource), "Order matters in lists")
    }

    @Test
    fun `field-level permissions correctly restrict access`() {
        val ability = Ability.builder()
            .can("read", "User", null, listOf("name", "email"))
            .can("read", "User", mapOf("id" to "me"), listOf("phone", "address"))
            .build()

        data class User(val id: String, val name: String, val email: String, val phone: String, val address: String)

        val myUser = User("me", "John", "john@example.com", "555-1234", "123 Main St")
        val otherUser = User("other", "Jane", "jane@example.com", "555-5678", "456 Oak Ave")

        // Public fields - anyone can read
        assertTrue(ability.can("read", myUser, "name"))
        assertTrue(ability.can("read", otherUser, "name"))
        assertTrue(ability.can("read", otherUser, "email"))

        // Private fields - only owner can read
        assertTrue(ability.can("read", myUser, "phone"))
        assertFalse(ability.can("read", otherUser, "phone"))

        // No field specified - checks resource-level permission
        assertTrue(ability.can("read", myUser))
        assertTrue(ability.can("read", otherUser))
    }

    @Test
    fun `empty conditions map matches any subject`() {
        val ability = Ability.builder()
            .can("read", "Resource", emptyMap(), null)
            .build()

        data class Resource(val id: String, val anyField: String)

        val resource = Resource("r1", "value")
        assertTrue(ability.can("read", resource), "Empty conditions should match any subject")
    }

    @Test
    fun `null condition values match null subject fields`() {
        val ability = Ability.builder()
            .can("access", "Resource", mapOf("optional" to null), null)
            .build()

        data class Resource(val id: String, val optional: String?)

        val resourceWithNull = Resource("r1", null)
        val resourceWithValue = Resource("r2", "value")

        assertTrue(ability.can("access", resourceWithNull), "Null condition should match null field")
        assertFalse(ability.can("access", resourceWithValue), "Null condition should not match non-null field")
    }

    @Test
    fun `string subject types work as type literals`() {
        val ability = Ability.builder()
            .can("read", "BlogPost")
            .cannot("read", "PrivatePost")
            .build()

        assertTrue(ability.can("read", "BlogPost"), "String literal should work as subject type")
        assertFalse(ability.can("read", "PrivatePost"), "Cannot rule should apply to string literal")
    }

    @Test
    fun `runtime rule updates replace entire rule set`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .can("update", "Post")
            .build()

        data class Post(val id: String)
        val post = Post("p1")

        assertTrue(ability.can("read", post))
        assertTrue(ability.can("update", post))

        // Update with new rules
        val newRules = listOf(
            RawRule("delete", "Post", null, null, false)
        )
        ability.update(newRules)

        // Old rules should no longer apply
        assertFalse(ability.can("read", post), "Old read rule should be gone")
        assertFalse(ability.can("update", post), "Old update rule should be gone")
        assertTrue(ability.can("delete", post), "New delete rule should work")
    }
}
