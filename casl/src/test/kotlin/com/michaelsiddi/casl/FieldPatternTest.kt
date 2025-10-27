package com.michaelsiddi.casl

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for field pattern matching with wildcards.
 * Based on casl-js ability.spec.js lines 498-581
 */
class FieldPatternTest {

    @Test
    fun `allows to act on any 1st level field (e_g_, author_*)`() {
        val ability = Ability.builder()
            .can("read", "Post", "author.*")
            .build()

        assertTrue(ability.can("read", "Post", "author"))
        assertTrue(ability.can("read", "Post", "author.*"))
        assertTrue(ability.can("read", "Post", "author.name"))
        assertTrue(ability.can("read", "Post", "author.age"))
        assertFalse(ability.can("read", "Post", "author.publication.name"))
    }

    @Test
    fun `allows to act on field at any depth (e_g_, author_**)`() {
        val ability = Ability.builder()
            .can("read", "Post", "author.**")
            .build()

        assertTrue(ability.can("read", "Post", "author"))
        assertTrue(ability.can("read", "Post", "author.**"))
        assertTrue(ability.can("read", "Post", "author.name"))
        assertTrue(ability.can("read", "Post", "author.age"))
        assertTrue(ability.can("read", "Post", "author.publication.name"))
    }

    @Test
    fun `allows to act on fields defined by star in the middle of path (e_g_, author_*_name)`() {
        val ability = Ability.builder()
            .can("read", "Post", "author.*.name")
            .build()

        assertFalse(ability.can("read", "Post", "author"))
        assertTrue(ability.can("read", "Post", "author.*.name"))
        assertTrue(ability.can("read", "Post", "author.publication.name"))
        assertFalse(ability.can("read", "Post", "author.publication.startDate"))
        assertFalse(ability.can("read", "Post", "author.publication.country.name"))
    }

    @Test
    fun `allows to act on fields defined by 2 stars in the middle of path (e_g_, author_**_name)`() {
        val ability = Ability.builder()
            .can("read", "Post", "author.**.name")
            .build()

        assertFalse(ability.can("read", "Post", "author"))
        assertTrue(ability.can("read", "Post", "author.**.name"))
        assertTrue(ability.can("read", "Post", "author.publication.name"))
        assertFalse(ability.can("read", "Post", "author.publication.startDate"))
        assertTrue(ability.can("read", "Post", "author.publication.country.name"))
    }

    @Test
    fun `allows to act on fields defined by star at the beginning (e_g_, *_name)`() {
        val ability = Ability.builder()
            .can("read", "Post", "*.name")
            .build()

        assertTrue(ability.can("read", "Post", "author.name"))
        assertTrue(ability.can("read", "Post", "*.name"))
        assertFalse(ability.can("read", "Post", "author.publication.name"))
    }

    @Test
    fun `allows to act on fields defined by 2 stars at the beginning (e_g_, **_name)`() {
        val ability = Ability.builder()
            .can("read", "Post", "**.name")
            .build()

        assertTrue(ability.can("read", "Post", "author.name"))
        assertTrue(ability.can("read", "Post", "**.name"))
        assertTrue(ability.can("read", "Post", "author.publication.name"))
    }

    @Test
    fun `allows to act on fields defined by stars (e_g_, author_address_street*)`() {
        val ability = Ability.builder()
            .can("read", "Post", "author.address.street*")
            .build()

        assertTrue(ability.can("read", "Post", "author.address.street"))
        assertTrue(ability.can("read", "Post", "author.address.street1"))
        assertTrue(ability.can("read", "Post", "author.address.street2"))
        assertFalse(ability.can("read", "Post", "author.address"))
    }

    @Test
    fun `correctly works with special regexp symbols`() {
        val ability = Ability.builder()
            .can("read", "Post", "author?.address+.street*")
            .build()

        assertTrue(ability.can("read", "Post", "author?.address+.street"))
        assertTrue(ability.can("read", "Post", "author?.address+.street1"))
        assertTrue(ability.can("read", "Post", "author?.address+.street2"))
        assertFalse(ability.can("read", "Post", "author?.address+"))
    }

    @Test
    fun `can match field patterns`() {
        val ability = Ability.builder()
            .can("read", "Post", "vehicle.*.generic.*")
            .build()

        assertTrue(ability.can("read", "Post", "vehicle.profile.generic.item"))
        assertTrue(ability.can("read", "Post", "vehicle.*.generic.signal"))
        assertTrue(ability.can("read", "Post", "vehicle.profile.generic.*"))
        assertFalse(ability.can("read", "Post", "vehicle.*.user.*"))
    }

    @Test
    fun `supports multiple field patterns`() {
        val ability = Ability.builder()
            .can("read", "Post", listOf("title", "author.*", "content"))
            .build()

        assertTrue(ability.can("read", "Post", "title"))
        assertTrue(ability.can("read", "Post", "content"))
        assertTrue(ability.can("read", "Post", "author.name"))
        assertTrue(ability.can("read", "Post", "author.age"))
        assertFalse(ability.can("read", "Post", "createdAt"))
    }

    @Test
    fun `supports inverted field patterns`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .cannot("read", "Post", "author.*")
            .build()

        assertTrue(ability.can("read", "Post", "title"))
        assertTrue(ability.can("read", "Post", "content"))
        assertFalse(ability.can("read", "Post", "author.name"))
        assertFalse(ability.can("read", "Post", "author.age"))
        assertTrue(ability.can("read", "Post", "author.publication.name")) // Deeper level allowed
    }

    @Test
    fun `field patterns work with conditions`() {
        val post = subject("Post", "published" to true, "authorId" to 123)

        val ability = Ability.builder()
            .can("read", "Post", mapOf("published" to true), listOf("author.*"))
            .build()

        assertTrue(ability.can("read", post, "author.name"))
        assertTrue(ability.can("read", post, "author.age"))
        assertFalse(ability.can("read", post, "title"))
    }

    @Test
    fun `prefix wildcard matches correctly`() {
        val ability = Ability.builder()
            .can("read", "Post", "status*")
            .build()

        assertTrue(ability.can("read", "Post", "status"))
        assertTrue(ability.can("read", "Post", "status1"))
        assertTrue(ability.can("read", "Post", "statusCode"))
        assertTrue(ability.can("read", "Post", "statusMessage"))
        assertFalse(ability.can("read", "Post", "state"))
    }

    @Test
    fun `exact match still works when no wildcards present`() {
        val ability = Ability.builder()
            .can("read", "Post", listOf("title", "content", "author"))
            .build()

        assertTrue(ability.can("read", "Post", "title"))
        assertTrue(ability.can("read", "Post", "content"))
        assertTrue(ability.can("read", "Post", "author"))
        assertFalse(ability.can("read", "Post", "author.name"))
        assertFalse(ability.can("read", "Post", "createdAt"))
    }
}
