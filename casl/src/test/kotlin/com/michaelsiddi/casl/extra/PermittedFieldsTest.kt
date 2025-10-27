package com.michaelsiddi.casl.extra

import com.michaelsiddi.casl.Ability
import com.michaelsiddi.casl.subject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for permittedFieldsOf utility.
 * Based on casl-js permitted_fields.spec.js
 */
class PermittedFieldsTest {

    private val defaultOptions = PermittedFieldsOptions { rule ->
        rule.fields ?: listOf("title", "description")
    }

    @Test
    fun `returns empty list for Ability with empty rules`() {
        val ability = Ability.builder().build()

        val fields = permittedFieldsOf(ability, "read", "Post", defaultOptions)

        assertTrue(fields.isEmpty())
    }

    @Test
    fun `returns all fields if none of rules specify fields`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .build()

        val fields = permittedFieldsOf(ability, "read", "Post", defaultOptions)

        assertEquals(listOf("title", "description"), fields)
    }

    @Test
    fun `returns unique array of fields if there are duplicated fields across rules`() {
        val ability = Ability.builder()
            .can("read", "Post", "title")
            .can("read", "Post", listOf("title", "description"))
            .build()

        val fields = permittedFieldsOf(ability, "read", "Post", defaultOptions)

        assertEquals(2, fields.size)
        assertTrue(fields.containsAll(listOf("title", "description")))
    }

    @Test
    fun `returns unique fields for array which contains direct and inverted rules`() {
        val ability = Ability.builder()
            .can("read", "Post", listOf("title", "description"))
            .cannot("read", "Post", "description")
            .build()

        val fields = permittedFieldsOf(ability, "read", "Post", defaultOptions)

        assertEquals(1, fields.size)
        assertEquals(listOf("title"), fields)
    }

    @Test
    fun `allows to provide fieldsFrom option which extract fields from rule`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .build()

        val fields = permittedFieldsOf(ability, "read", "Post", PermittedFieldsOptions { rule ->
            rule.fields ?: listOf("title")
        })

        assertEquals(listOf("title"), fields)
    }

    @Test
    fun `works with subject instances`() {
        val ability = Ability.builder()
            .can("read", "Post", mapOf("published" to true), listOf("title", "content"))
            .can("read", "Post", mapOf("published" to false), listOf("title"))
            .build()

        val publishedPost = subject("Post", "published" to true)
        val draftPost = subject("Post", "published" to false)

        val publishedFields = permittedFieldsOf(ability, "read", publishedPost, defaultOptions)
        val draftFields = permittedFieldsOf(ability, "read", draftPost, defaultOptions)

        assertTrue(publishedFields.containsAll(listOf("title", "content")))
        assertEquals(listOf("title"), draftFields)
    }

    @Test
    fun `AccessibleFields ofType returns fields for type`() {
        val ability = Ability.builder()
            .can("read", "Post", listOf("title", "content"))
            .build()

        val getAllFields: AllFieldsExtractor = { subjectType ->
            when (subjectType) {
                "Post" -> listOf("title", "content", "authorId", "createdAt")
                else -> emptyList()
            }
        }

        val accessibleFields = AccessibleFields(ability, "read", getAllFields)
        val fields = accessibleFields.ofType("Post")

        assertEquals(listOf("title", "content"), fields)
    }

    @Test
    fun `AccessibleFields of returns fields for instance`() {
        val ability = Ability.builder()
            .can("read", "Post", mapOf("draft" to true), listOf("title"))
            .can("read", "Post", mapOf("draft" to false), listOf("title", "content"))
            .build()

        val getAllFields: AllFieldsExtractor = { _ ->
            listOf("title", "content")
        }

        val accessibleFields = AccessibleFields(ability, "read", getAllFields)
        val publishedPost = subject("Post", "draft" to false)
        val fields = accessibleFields.of(publishedPost)

        assertTrue(fields.containsAll(listOf("title", "content")))
    }
}
