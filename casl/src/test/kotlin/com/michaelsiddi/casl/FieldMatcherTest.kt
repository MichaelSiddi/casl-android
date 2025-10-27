package com.michaelsiddi.casl

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Direct tests for FieldMatcher to debug pattern generation.
 */
class FieldMatcherTest {

    @Test
    fun `test exact match`() {
        val matcher = FieldMatcher.createMatcher(listOf("title", "content"))

        assertTrue(matcher("title"))
        assertTrue(matcher("content"))
        assertFalse(matcher("author"))
    }

    @Test
    fun `test simple wildcard pattern`() {
        try {
            val matcher = FieldMatcher.createMatcher(listOf("author.*"))

            assertTrue(matcher("author.*"))
            assertTrue(matcher("author.name"))
            assertTrue(matcher("author.age"))
            assertTrue(matcher("author"))  // Pattern also matches parent
            assertFalse(matcher("author.publication.name"))
        } catch (e: Exception) {
            println("Error creating matcher: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
