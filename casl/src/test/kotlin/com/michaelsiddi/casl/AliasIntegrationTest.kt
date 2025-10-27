package com.michaelsiddi.casl

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for action aliases with Ability.
 * Tests that aliases work correctly with permission checking.
 */
class AliasIntegrationTest {

    @Test
    fun `ability with alias resolver expands aliases in rules`() {
        // Define alias: modify -> [update, delete]
        val resolver = createAliasResolver(
            mapOf("modify" to listOf("update", "delete"))
        )
        val options = AbilityOptions(resolveAction = resolver)

        // Define rule using alias "modify"
        val ability = Ability.fromRules(
            listOf(
                RawRule(action = "modify", subject = "Post")
            ),
            options
        )

        val post = subject("Post", "id" to 1)

        // Should allow both update and delete due to alias expansion
        assertTrue(ability.can("modify", post))
        assertTrue(ability.can("update", post))
        assertTrue(ability.can("delete", post))
        assertFalse(ability.can("read", post))
    }

    @Test
    fun `ability with nested aliases expands transitively`() {
        val resolver = createAliasResolver(
            mapOf(
                "modify" to listOf("update", "delete"),
                "access" to listOf("read", "modify")  // access -> read, update, delete
            )
        )
        val options = AbilityOptions(resolveAction = resolver)

        val ability = Ability.fromRules(
            listOf(
                RawRule(action = "access", subject = "Post")
            ),
            options
        )

        val post = subject("Post", "id" to 1)

        // All expanded actions should be allowed
        assertTrue(ability.can("access", post))
        assertTrue(ability.can("read", post))
        assertTrue(ability.can("modify", post))
        assertTrue(ability.can("update", post))
        assertTrue(ability.can("delete", post))
        assertFalse(ability.can("create", post))
    }

    @Test
    fun `ability with aliases works with conditions`() {
        val resolver = createAliasResolver(
            mapOf("modify" to listOf("update", "delete"))
        )
        val options = AbilityOptions(resolveAction = resolver)

        val ability = Ability.fromRules(
            listOf(
                RawRule(
                    action = "modify",
                    subject = "Post",
                    conditions = mapOf("authorId" to 123)
                )
            ),
            options
        )

        val myPost = subject("Post", "authorId" to 123)
        val otherPost = subject("Post", "authorId" to 456)

        // Should work with conditions
        assertTrue(ability.can("update", myPost))
        assertTrue(ability.can("delete", myPost))
        assertFalse(ability.can("update", otherPost))
        assertFalse(ability.can("delete", otherPost))
    }

    @Test
    fun `ability with aliases works with inverted rules`() {
        val resolver = createAliasResolver(
            mapOf("modify" to listOf("update", "delete"))
        )
        val options = AbilityOptions(resolveAction = resolver)

        val ability = Ability.fromRules(
            listOf(
                RawRule(action = "modify", subject = "Post"),
                RawRule(action = "delete", subject = "Post", inverted = true)
            ),
            options
        )

        val post = subject("Post", "id" to 1)

        // modify expands to [modify, update, delete]
        // But "cannot delete" overrides the delete from modify
        assertTrue(ability.can("modify", post))
        assertTrue(ability.can("update", post))
        assertFalse(ability.can("delete", post), "delete should be denied by inverted rule")
    }

    @Test
    fun `ability with aliases works with field restrictions`() {
        val resolver = createAliasResolver(
            mapOf("modify" to listOf("update", "delete"))
        )
        val options = AbilityOptions(resolveAction = resolver)

        val ability = Ability.fromRules(
            listOf(
                RawRule(
                    action = "modify",
                    subject = "Post",
                    fields = listOf("title", "content")
                )
            ),
            options
        )

        val post = subject("Post", "id" to 1)

        // Field restrictions should apply to expanded actions
        assertTrue(ability.can("update", post, "title"))
        assertTrue(ability.can("delete", post, "content"))
        assertFalse(ability.can("update", post, "authorId"))
    }

    @Test
    fun `ability without aliases works normally`() {
        // No alias resolver - actions should work as-is
        val ability = Ability.fromRules(
            listOf(
                RawRule(action = "update", subject = "Post")
            )
        )

        val post = subject("Post", "id" to 1)

        assertTrue(ability.can("update", post))
        assertFalse(ability.can("delete", post))
        assertFalse(ability.can("modify", post))
    }

    @Test
    fun `ability with multiple aliased actions in rule`() {
        val resolver = createAliasResolver(
            mapOf(
                "edit" to listOf("update"),
                "remove" to listOf("delete")
            )
        )
        val options = AbilityOptions(resolveAction = resolver)

        // Rule with array of aliased actions
        val ability = Ability.fromRules(
            listOf(
                RawRule(action = listOf("edit", "remove"), subject = "Post")
            ),
            options
        )

        val post = subject("Post", "id" to 1)

        assertTrue(ability.can("edit", post))
        assertTrue(ability.can("update", post))
        assertTrue(ability.can("remove", post))
        assertTrue(ability.can("delete", post))
        assertFalse(ability.can("read", post))
    }

    @Test
    fun `ability with alias and wildcard manage`() {
        val resolver = createAliasResolver(
            mapOf("modify" to listOf("update", "delete"))
        )
        val options = AbilityOptions(resolveAction = resolver)

        val ability = Ability.fromRules(
            listOf(
                RawRule(action = "manage", subject = "Post")
            ),
            options
        )

        val post = subject("Post", "id" to 1)

        // "manage" should still work as wildcard (not aliased)
        assertTrue(ability.can("manage", post))
        assertTrue(ability.can("read", post))
        assertTrue(ability.can("update", post))
        assertTrue(ability.can("delete", post))
        assertTrue(ability.can("modify", post))
        assertTrue(ability.can("anything", post))
    }
}
