package com.michaelsiddi.casl

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for ability introspection methods (rulesFor, actionsFor, relevantRuleFor).
 * Based on casl-js introspection tests.
 */
class IntrospectionTest {

    @Test
    fun `rulesFor returns all matching rules for action and subject`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .can("update", "Post", mapOf("authorId" to 123))
            .can("delete", "Post")
            .build()

        val rules = ability.rulesFor("read", "Post")

        assertEquals(1, rules.size)
        assertEquals("read", rules[0].action)
        assertEquals("Post", rules[0].subject)
    }

    @Test
    fun `rulesFor returns multiple rules when multiple match`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .can("read", "Post", mapOf("published" to true))
            .build()

        val rules = ability.rulesFor("read", "Post")

        assertEquals(2, rules.size)
        assertTrue(rules.all { (it.action == "read" || it.action is List<*>) && it.subject == "Post" })
    }

    @Test
    fun `rulesFor returns empty list when no rules match`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .build()

        val rules = ability.rulesFor("delete", "Post")

        assertTrue(rules.isEmpty())
    }

    @Test
    fun `rulesFor filters by field when provided`() {
        val ability = Ability.builder()
            .can("update", "Post", "title")
            .can("update", "Post", "content")
            .build()

        val titleRules = ability.rulesFor("update", "Post", "title")

        assertEquals(1, titleRules.size)
        assertEquals(listOf("title"), titleRules[0].fields)
    }

    @Test
    fun `rulesFor includes wildcard manage action`() {
        val ability = Ability.builder()
            .can("manage", "Post")
            .can("read", "Post")
            .build()

        val rules = ability.rulesFor("update", "Post")

        // Should include the "manage" rule since it matches all actions
        assertEquals(1, rules.size)
        assertEquals("manage", rules[0].action)
    }

    @Test
    fun `rulesFor includes wildcard all subject`() {
        val ability = Ability.builder()
            .can("read", "all")
            .can("read", "Post")
            .build()

        val rules = ability.rulesFor("read", "Post")

        // Should include both the "all" wildcard and specific "Post" rule
        assertEquals(2, rules.size)
    }

    @Test
    fun `actionsFor returns all actions for subject type`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .can("update", "Post")
            .can("delete", "Post")
            .build()

        val actions = ability.actionsFor("Post")

        assertEquals(3, actions.size)
        assertTrue(actions.contains("read"))
        assertTrue(actions.contains("update"))
        assertTrue(actions.contains("delete"))
    }

    @Test
    fun `actionsFor returns empty set when no rules for subject`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .build()

        val actions = ability.actionsFor("Comment")

        assertTrue(actions.isEmpty())
    }

    @Test
    fun `actionsFor includes wildcard actions`() {
        val ability = Ability.builder()
            .can("manage", "Post")
            .build()

        val actions = ability.actionsFor("Post")

        assertTrue(actions.contains("manage"))
    }

    @Test
    fun `actionsFor includes actions from all wildcard subject`() {
        val ability = Ability.builder()
            .can("read", "all")
            .can("update", "Post")
            .build()

        val actions = ability.actionsFor("Post")

        assertEquals(2, actions.size)
        assertTrue(actions.contains("read"))
        assertTrue(actions.contains("update"))
    }

    @Test
    fun `actionsFor handles array actions`() {
        val ability = Ability.fromRules(
            listOf(
                RawRule(action = listOf("read", "update"), subject = "Post")
            )
        )

        val actions = ability.actionsFor("Post")

        assertTrue(actions.contains("read"))
        assertTrue(actions.contains("update"))
    }

    @Test
    fun `relevantRuleFor returns the matching rule`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .can("update", "Post")
            .build()

        val rule = ability.relevantRuleFor("read", subject("Post", "id" to 1))

        assertNotNull(rule)
        assertEquals("read", rule.action)
        assertEquals("Post", rule.subject)
    }

    @Test
    fun `relevantRuleFor returns null when no rule matches`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .build()

        val rule = ability.relevantRuleFor("delete", subject("Post", "id" to 1))

        assertNull(rule)
    }

    @Test
    fun `relevantRuleFor returns last matching rule`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .cannot("read", "Post", mapOf("draft" to true))
            .build()

        val draftPost = subject("Post", "draft" to true)
        val rule = ability.relevantRuleFor("read", draftPost)

        assertNotNull(rule)
        assertTrue(rule.inverted, "Should return the inverted rule (last match wins)")
    }

    @Test
    fun `relevantRuleFor works with field parameter`() {
        val ability = Ability.builder()
            .can("update", "Post", "title")
            .cannot("update", "Post", "secret")
            .build()

        val post = subject("Post", "id" to 1)

        val titleRule = ability.relevantRuleFor("update", post, "title")
        assertNotNull(titleRule)
        assertEquals(listOf("title"), titleRule.fields)

        val secretRule = ability.relevantRuleFor("update", post, "secret")
        assertNotNull(secretRule)
        assertTrue(secretRule.inverted)
    }

    @Test
    fun `relevantRuleFor respects conditions`() {
        val ability = Ability.builder()
            .can("update", "Post", mapOf("authorId" to 123))
            .build()

        val myPost = subject("Post", "authorId" to 123)
        val otherPost = subject("Post", "authorId" to 456)

        val myRule = ability.relevantRuleFor("update", myPost)
        assertNotNull(myRule)

        val otherRule = ability.relevantRuleFor("update", otherPost)
        assertNull(otherRule, "Should not match when conditions don't match")
    }

    @Test
    fun `actionsFor returns unique actions`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .can("read", "Post", mapOf("published" to true))
            .build()

        val actions = ability.actionsFor("Post")

        assertEquals(1, actions.size, "Should return unique actions only")
        assertTrue(actions.contains("read"))
    }
}
