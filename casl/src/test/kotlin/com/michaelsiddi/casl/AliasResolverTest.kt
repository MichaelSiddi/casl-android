package com.michaelsiddi.casl

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests for action alias resolution system.
 * Based on casl-js alias resolution functionality.
 */
class AliasResolverTest {

    @Test
    fun `createAliasResolver expands single alias`() {
        val resolver = createAliasResolver(
            mapOf("modify" to listOf("update", "delete"))
        )

        val result = resolver("modify")

        // Should contain original action plus expanded aliases
        assertTrue(result.contains("modify"))
        assertTrue(result.contains("update"))
        assertTrue(result.contains("delete"))
        assertEquals(3, result.size)
    }

    @Test
    fun `createAliasResolver handles nested aliases`() {
        val resolver = createAliasResolver(
            mapOf(
                "modify" to listOf("update", "delete"),
                "access" to listOf("read", "modify")  // nested: access -> read, modify -> update, delete
            )
        )

        val result = resolver("access")

        // Should contain: access, read, modify, update, delete
        assertTrue(result.contains("access"))
        assertTrue(result.contains("read"))
        assertTrue(result.contains("modify"))
        assertTrue(result.contains("update"))
        assertTrue(result.contains("delete"))
        assertEquals(5, result.size)
    }

    @Test
    fun `createAliasResolver handles action lists`() {
        val resolver = createAliasResolver(
            mapOf("modify" to listOf("update", "delete"))
        )

        val result = resolver(listOf("read", "modify"))

        // Should expand modify but keep read as-is
        assertTrue(result.contains("read"))
        assertTrue(result.contains("modify"))
        assertTrue(result.contains("update"))
        assertTrue(result.contains("delete"))
        assertEquals(4, result.size)
    }

    @Test
    fun `createAliasResolver returns identity for non-aliased actions`() {
        val resolver = createAliasResolver(
            mapOf("modify" to listOf("update", "delete"))
        )

        val result = resolver("read")

        assertEquals(listOf("read"), result)
    }

    @Test
    fun `createAliasResolver throws on cycle detection`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            createAliasResolver(
                mapOf(
                    "foo" to listOf("bar"),
                    "bar" to listOf("foo")  // cycle: foo -> bar -> foo
                )
            )
        }

        assertTrue(exception.message!!.contains("cycle"))
    }

    @Test
    fun `createAliasResolver throws when using reserved action as alias`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            createAliasResolver(
                mapOf("manage" to listOf("read", "update"))
            )
        }

        assertTrue(exception.message!!.contains("manage"))
        assertTrue(exception.message!!.contains("reserved"))
    }

    @Test
    fun `createAliasResolver throws when aliasing to reserved action`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            createAliasResolver(
                mapOf("superpower" to listOf("manage"))
            )
        }

        assertTrue(exception.message!!.contains("manage"))
        assertTrue(exception.message!!.contains("reserved"))
    }

    @Test
    fun `createAliasResolver with custom reserved action`() {
        val options = AliasResolverOptions(anyAction = "admin")

        val exception = assertFailsWith<IllegalArgumentException> {
            createAliasResolver(
                mapOf("admin" to listOf("read", "write")),
                options
            )
        }

        assertTrue(exception.message!!.contains("admin"))
        assertTrue(exception.message!!.contains("reserved"))
    }

    @Test
    fun `createAliasResolver with skipValidate skips cycle check`() {
        val options = AliasResolverOptions(skipValidate = true)

        // This would normally throw, but with skipValidate it doesn't
        val resolver = createAliasResolver(
            mapOf(
                "foo" to listOf("bar"),
                "bar" to listOf("foo")
            ),
            options
        )

        // Note: actual execution would cause infinite loop, but creation succeeds
        // We're just testing that it doesn't throw during creation
    }

    @Test
    fun `identityResolver returns action as list without expansion`() {
        val result1 = identityResolver("read")
        assertEquals(listOf("read"), result1)

        val result2 = identityResolver(listOf("read", "update"))
        assertEquals(listOf("read", "update"), result2)
    }

    @Test
    fun `createAliasResolver handles complex nested aliases`() {
        val resolver = createAliasResolver(
            mapOf(
                "edit" to listOf("update"),
                "modify" to listOf("edit", "delete"),
                "manage_post" to listOf("create", "modify", "read")
                // manage_post expands to: manage_post, create, modify, edit, update, delete, read
            )
        )

        val result = resolver("manage_post")

        assertTrue(result.contains("manage_post"))
        assertTrue(result.contains("create"))
        assertTrue(result.contains("modify"))
        assertTrue(result.contains("edit"))
        assertTrue(result.contains("update"))
        assertTrue(result.contains("delete"))
        assertTrue(result.contains("read"))
        assertEquals(7, result.size)
    }

    @Test
    fun `createAliasResolver with self-referencing alias throws`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            createAliasResolver(
                mapOf("foo" to listOf("foo"))  // cycle: foo -> foo
            )
        }

        assertTrue(exception.message!!.contains("cycle"))
    }
}
