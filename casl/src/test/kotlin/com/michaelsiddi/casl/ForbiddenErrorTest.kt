package com.michaelsiddi.casl

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for ForbiddenError exception class.
 * Based on casl-js ForbiddenError tests.
 */
class ForbiddenErrorTest {

    private fun setupAbility(): Ability {
        return Ability.builder()
            .can("read", "Post")
            .can("update", "Post")
            .cannot("delete", "Post")
            .build()
    }

    @Test
    fun `throws exception on disallowed action`() {
        val ability = setupAbility()
        val error = ForbiddenError(ability)

        assertFailsWith<ForbiddenError> {
            error.throwUnlessCan("archive", subject("Post", "id" to 1))
        }
    }

    @Test
    fun `does not throw exception on allowed action`() {
        val ability = setupAbility()
        val error = ForbiddenError(ability)

        // Should not throw
        error.throwUnlessCan("read", subject("Post", "id" to 1))
    }

    @Test
    fun `thrown error includes context information`() {
        val ability = setupAbility()
        val error = ForbiddenError(ability)
        val post = subject("Post", "id" to 1)

        var thrownError: ForbiddenError? = null
        try {
            error.throwUnlessCan("archive", post)
        } catch (e: ForbiddenError) {
            thrownError = e
        }

        assertNotNull(thrownError)
        assertEquals("archive", thrownError.action)
        assertEquals(post, thrownError.subject)
        assertEquals("Post", thrownError.subjectType)
        assertEquals(ability, thrownError.ability)
        assertNull(thrownError.field)
    }

    @Test
    fun `thrown error includes field information when provided`() {
        // Setup ability that allows update but only on specific fields
        val ability = Ability.builder()
            .can("update", "Post", "title")
            .build()

        val error = ForbiddenError(ability)
        val post = subject("Post", "id" to 1)

        var thrownError: ForbiddenError? = null
        try {
            error.throwUnlessCan("update", post, "secret")
        } catch (e: ForbiddenError) {
            thrownError = e
        }

        assertNotNull(thrownError)
        assertEquals("update", thrownError.action)
        assertEquals("secret", thrownError.field)
    }

    @Test
    fun `has default error message`() {
        val ability = setupAbility()
        val error = ForbiddenError(ability)

        try {
            error.throwUnlessCan("delete", subject("Post", "id" to 1))
        } catch (e: ForbiddenError) {
            assertTrue(e.message.contains("delete"))
            assertTrue(e.message.contains("Post"))
        }
    }

    @Test
    fun `default error message includes field when provided`() {
        val ability = setupAbility()
        val error = ForbiddenError(ability)

        try {
            error.throwUnlessCan("update", subject("Post", "id" to 1), "authorId")
        } catch (e: ForbiddenError) {
            assertTrue(e.message.contains("update"))
            assertTrue(e.message.contains("Post"))
            assertTrue(e.message.contains("authorId"))
        }
    }

    @Test
    fun `allows custom error message`() {
        val ability = setupAbility()
        val error = ForbiddenError(ability)
        val customMessage = "You don't have the card!"

        var thrownError: ForbiddenError? = null
        try {
            error.setMessage(customMessage).throwUnlessCan("delete", subject("Post", "id" to 1))
        } catch (e: ForbiddenError) {
            thrownError = e
        }

        assertNotNull(thrownError)
        assertEquals(customMessage, thrownError.message)
    }

    @Test
    fun `unlessCan returns null when action is allowed`() {
        val ability = setupAbility()
        val error = ForbiddenError(ability)

        val result = error.unlessCan("read", subject("Post", "id" to 1))

        assertNull(result)
    }

    @Test
    fun `unlessCan returns error when action is not allowed`() {
        val ability = setupAbility()
        val error = ForbiddenError(ability)

        val result = error.unlessCan("delete", subject("Post", "id" to 1))

        assertNotNull(result)
        assertEquals(error, result)
        assertEquals("delete", result.action)
        assertEquals("Post", result.subjectType)
    }

    @Test
    fun `can be created with factory method`() {
        val ability = setupAbility()
        val error = ForbiddenError.from(ability)

        assertNotNull(error)
        assertEquals(ability, error.ability)
    }

    @Test
    fun `works with conditions`() {
        val ability = Ability.builder()
            .can("update", "Post", mapOf("authorId" to 123))
            .build()

        val error = ForbiddenError(ability)
        val myPost = subject("Post", "authorId" to 123)
        val otherPost = subject("Post", "authorId" to 456)

        // Should not throw for my post
        error.throwUnlessCan("update", myPost)

        // Should throw for other post
        assertFailsWith<ForbiddenError> {
            error.throwUnlessCan("update", otherPost)
        }
    }

    @Test
    fun `works with field-level permissions`() {
        val ability = Ability.builder()
            .can("update", "Post", "title")
            .build()

        val error = ForbiddenError(ability)
        val post = subject("Post", "id" to 1)

        // Should not throw for allowed field
        error.throwUnlessCan("update", post, "title")

        // Should throw for disallowed field
        assertFailsWith<ForbiddenError> {
            error.throwUnlessCan("update", post, "content")
        }
    }

    @Test
    fun `error message describes action and subject type`() {
        val ability = setupAbility()
        val error = ForbiddenError(ability)

        try {
            error.throwUnlessCan("archive", subject("Post", "id" to 1))
        } catch (e: ForbiddenError) {
            val message = e.message
            assertTrue(message.contains("archive"), "Message should contain action")
            assertTrue(message.contains("Post"), "Message should contain subject type")
            assertTrue(message.contains("Cannot"), "Message should indicate denial")
        }
    }

    @Test
    fun `setMessage can be chained`() {
        val ability = setupAbility()
        val error = ForbiddenError(ability)

        // setMessage should return the error for chaining
        val result = error.setMessage("Custom message")

        assertEquals(error, result)
    }

    @Test
    fun `error can be reused for multiple checks`() {
        val ability = setupAbility()
        val error = ForbiddenError(ability)
        val post = subject("Post", "id" to 1)

        // First check
        val result1 = error.unlessCan("read", post)
        assertNull(result1)

        // Second check
        val result2 = error.unlessCan("delete", post)
        assertNotNull(result2)
        assertEquals("delete", result2.action)

        // Third check
        val result3 = error.unlessCan("update", post)
        assertNull(result3)
    }

    @Test
    fun `uses reason from forbidden rule as error message`() {
        val noCardMessage = "No credit card provided"
        val ability = Ability.builder()
            .cannot("update", "Post", reason = noCardMessage)
            .build()

        val error = ForbiddenError(ability)

        val thrownError = assertFailsWith<ForbiddenError> {
            error.throwUnlessCan("update", "Post")
        }

        assertEquals(noCardMessage, thrownError.message)
    }

    @Test
    fun `custom message overrides rule reason`() {
        val reasonMessage = "No credit card provided"
        val customMessage = "Custom error message"
        val ability = Ability.builder()
            .cannot("update", "Post", reason = reasonMessage)
            .build()

        val error = ForbiddenError(ability)
        error.setMessage(customMessage)

        val thrownError = assertFailsWith<ForbiddenError> {
            error.throwUnlessCan("update", "Post")
        }

        assertEquals(customMessage, thrownError.message)
    }

    @Test
    fun `setDefaultMessage sets global default message from function`() {
        // Set global default message generator
        ForbiddenError.setDefaultMessage { error ->
            "error -> ${error.action}-${error.subjectType}"
        }

        try {
            val ability = setupAbility()
            val error = ForbiddenError(ability)

            val thrownError = assertFailsWith<ForbiddenError> {
                error.throwUnlessCan("archive", "Post")
            }

            assertEquals("error -> archive-Post", thrownError.message)
        } finally {
            // Reset to default
            ForbiddenError.setDefaultMessage(null as String?)
        }
    }

    @Test
    fun `setDefaultMessage with static string sets global default message`() {
        val staticMessage = "Global custom error message"

        // Set global default message as static string
        ForbiddenError.setDefaultMessage(staticMessage)

        try {
            val ability = setupAbility()
            val error = ForbiddenError(ability)

            val thrownError = assertFailsWith<ForbiddenError> {
                error.throwUnlessCan("archive", "Post")
            }

            assertEquals(staticMessage, thrownError.message)
        } finally {
            // Reset to default
            ForbiddenError.setDefaultMessage(null as String?)
        }
    }

    @Test
    fun `instance setMessage overrides global default message`() {
        val globalMessage = "Global error"
        val instanceMessage = "Instance error"

        // Set global default message
        ForbiddenError.setDefaultMessage(globalMessage)

        try {
            val ability = setupAbility()
            val error = ForbiddenError(ability)

            // Override with instance message
            error.setMessage(instanceMessage)

            val thrownError = assertFailsWith<ForbiddenError> {
                error.throwUnlessCan("archive", "Post")
            }

            assertEquals(instanceMessage, thrownError.message)
        } finally {
            // Reset to default
            ForbiddenError.setDefaultMessage(null as String?)
        }
    }

    @Test
    fun `global default message applies to new error instances`() {
        ForbiddenError.setDefaultMessage { error ->
            "custom: ${error.action} on ${error.subjectType}"
        }

        try {
            val ability = setupAbility()

            // First error instance
            val error1 = ForbiddenError(ability)
            val thrown1 = assertFailsWith<ForbiddenError> {
                error1.throwUnlessCan("delete", "Post")
            }
            assertEquals("custom: delete on Post", thrown1.message)

            // Second error instance
            val error2 = ForbiddenError(ability)
            val thrown2 = assertFailsWith<ForbiddenError> {
                error2.throwUnlessCan("archive", "Post")
            }
            assertEquals("custom: archive on Post", thrown2.message)
        } finally {
            // Reset to default
            ForbiddenError.setDefaultMessage(null as String?)
        }
    }
}
