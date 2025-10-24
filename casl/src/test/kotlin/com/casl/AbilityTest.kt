package com.casl

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for basic Ability permission checking
 * Matches iOS AbilityTests.swift
 */
class AbilityTest {

    @Test
    fun `Ability initialization with empty rules`() {
        val ability = Ability.builder().build()
        assertNotNull(ability)
    }

    @Test
    fun `can returns true for allowed action`() {
        val ability = Ability.builder()
            .can("read", "BlogPost")
            .build()

        assertTrue(ability.can("read", "BlogPost"), "Should be able to read BlogPost")
    }

    @Test
    fun `can returns false for forbidden action`() {
        val ability = Ability.builder()
            .can("read", "BlogPost")
            .build()

        assertFalse(ability.can("delete", "BlogPost"), "Should not be able to delete BlogPost")
    }

    @Test
    fun `cannot returns true for denied action`() {
        val ability = Ability.builder()
            .cannot("delete", "BlogPost")
            .build()

        assertTrue(ability.cannot("delete", "BlogPost"), "Should not be able to delete BlogPost")
    }

    @Test
    fun `default deny behavior when no rules`() {
        val ability = Ability.builder().build()

        assertFalse(ability.can("read", "BlogPost"), "Should deny read by default")
        assertFalse(ability.can("create", "BlogPost"), "Should deny create by default")
        assertFalse(ability.can("update", "BlogPost"), "Should deny update by default")
    }

    @Test
    fun `manage action wildcard matching`() {
        val ability = Ability.builder()
            .can("manage", "BlogPost")
            .build()

        assertTrue(ability.can("read", "BlogPost"), "'manage' should allow read")
        assertTrue(ability.can("create", "BlogPost"), "'manage' should allow create")
        assertTrue(ability.can("update", "BlogPost"), "'manage' should allow update")
        assertTrue(ability.can("delete", "BlogPost"), "'manage' should allow delete")
    }

    @Test
    fun `all subject wildcard matching`() {
        val ability = Ability.builder()
            .can("read", "all")
            .build()

        assertTrue(ability.can("read", "BlogPost"), "'all' should match BlogPost")
        assertTrue(ability.can("read", "Comment"), "'all' should match Comment")
        assertTrue(ability.can("read", "User"), "'all' should match User")
    }

    @Test
    fun `string subjects work as type literals`() {
        val ability = Ability.builder()
            .can("read", "BlogPost")
            .build()

        assertTrue(ability.can("read", "BlogPost"))
        assertFalse(ability.can("update", "BlogPost"))
    }

    @Test
    fun `inverted rules deny permissions`() {
        val ability = Ability.builder()
            .can("manage", "BlogPost")
            .cannot("delete", "BlogPost")
            .build()

        assertTrue(ability.can("read", "BlogPost"))
        assertTrue(ability.can("update", "BlogPost"))
        assertFalse(ability.can("delete", "BlogPost"), "Cannot rule should deny deletion")
    }

    @Test
    fun `multiple subjects in one rule`() {
        val ability = Ability.builder()
            .can("read", "BlogPost")
            .can("read", "Comment")
            .build()

        assertTrue(ability.can("read", "BlogPost"))
        assertTrue(ability.can("read", "Comment"))
        assertFalse(ability.can("read", "User"))
    }
}
