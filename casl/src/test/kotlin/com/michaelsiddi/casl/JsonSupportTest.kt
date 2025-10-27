package com.michaelsiddi.casl

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for JSONObject support in CASL.
 *
 * These tests verify that JSONObject can be used directly with ability.can()
 * and that JSON structures are correctly converted to Map structures for
 * condition matching.
 *
 * Uses Robolectric to provide Android framework classes (JSONObject, JSONArray)
 * in unit tests.
 */
@RunWith(RobolectricTestRunner::class)
class JsonSupportTest {

    @Test
    fun `jsonObjectToMap converts simple JSON`() {
        val json = JSONObject("""
            {
                "id": 123,
                "name": "John Doe",
                "active": true
            }
        """)

        val map = jsonObjectToMap(json)

        assertEquals(123, map["id"])
        assertEquals("John Doe", map["name"])
        assertEquals(true, map["active"])
    }

    @Test
    fun `jsonObjectToMap converts nested JSON objects`() {
        val json = JSONObject("""
            {
                "id": 1,
                "author": {
                    "id": 123,
                    "name": "John Doe"
                }
            }
        """)

        val map = jsonObjectToMap(json)

        assertEquals(1, map["id"])
        @Suppress("UNCHECKED_CAST")
        val author = map["author"] as? Map<String, Any?>
        assertNotNull(author)
        assertEquals(123, author["id"])
        assertEquals("John Doe", author["name"])
    }

    @Test
    fun `jsonObjectToMap converts deeply nested JSON`() {
        val json = JSONObject("""
            {
                "metadata": {
                    "author": {
                        "permissions": {
                            "canEdit": true
                        }
                    }
                }
            }
        """)

        val map = jsonObjectToMap(json)

        @Suppress("UNCHECKED_CAST")
        val metadata = map["metadata"] as? Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val author = metadata?.get("author") as? Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val permissions = author?.get("permissions") as? Map<String, Any?>

        assertEquals(true, permissions?.get("canEdit"))
    }

    @Test
    fun `jsonObjectToMap converts JSON arrays`() {
        val json = JSONObject("""
            {
                "tags": ["featured", "tech", "news"],
                "scores": [1, 2, 3]
            }
        """)

        val map = jsonObjectToMap(json)

        @Suppress("UNCHECKED_CAST")
        val tags = map["tags"] as? List<Any?>
        assertNotNull(tags)
        assertEquals(listOf("featured", "tech", "news"), tags)

        @Suppress("UNCHECKED_CAST")
        val scores = map["scores"] as? List<Any?>
        assertNotNull(scores)
        assertEquals(listOf(1, 2, 3), scores)
    }

    @Test
    fun `jsonObjectToMap handles null values`() {
        val json = JSONObject()
        json.put("id", 123)
        json.put("deletedAt", JSONObject.NULL)

        val map = jsonObjectToMap(json)

        assertEquals(123, map["id"])
        assertNull(map["deletedAt"])
    }

    @Test
    fun `jsonArrayToList converts array of objects`() {
        val jsonArray = JSONArray("""
            [
                {"id": 1, "name": "John"},
                {"id": 2, "name": "Jane"}
            ]
        """)

        val list = jsonArrayToList(jsonArray)

        assertEquals(2, list.size)

        @Suppress("UNCHECKED_CAST")
        val first = list[0] as? Map<String, Any?>
        assertNotNull(first)
        assertEquals(1, first["id"])
        assertEquals("John", first["name"])
    }

    @Test
    fun `subjectFromJson creates ForcedSubject`() {
        val json = JSONObject("""
            {
                "id": 123,
                "author": {
                    "id": 456
                }
            }
        """)

        val subject = subjectFromJson("Post", json)

        assertEquals("Post", subject.getSubjectType())
        assertEquals(123, subject["id"])

        @Suppress("UNCHECKED_CAST")
        val author = subject["author"] as? Map<String, Any?>
        assertNotNull(author)
        assertEquals(456, author["id"])
    }

    @Test
    fun `ability can() works with JSONObject - simple condition`() {
        val ability = Ability.builder()
            .can("update", "Post", conditions = mapOf("author.id" to 123))
            .build()

        val myPost = JSONObject("""
            {
                "id": 1,
                "author": {
                    "id": 123,
                    "name": "John Doe"
                }
            }
        """)

        val otherPost = JSONObject("""
            {
                "id": 2,
                "author": {
                    "id": 456,
                    "name": "Jane Smith"
                }
            }
        """)

        assertTrue(ability.can("update", "Post", myPost))
        assertFalse(ability.can("update", "Post", otherPost))
    }

    @Test
    fun `ability can() works with JSONObject - multiple conditions`() {
        val ability = Ability.builder()
            .can("update", "Post", conditions = mapOf(
                "author.id" to 123,
                "status" to "draft",
                "published" to false
            ))
            .build()

        val validPost = JSONObject("""
            {
                "author": {"id": 123},
                "status": "draft",
                "published": false
            }
        """)

        val invalidPost = JSONObject("""
            {
                "author": {"id": 123},
                "status": "published",
                "published": true
            }
        """)

        assertTrue(ability.can("update", "Post", validPost))
        assertFalse(ability.can("update", "Post", invalidPost))
    }

    @Test
    fun `ability can() works with JSONObject - deep nesting`() {
        val ability = Ability.builder()
            .can("read", "Post", conditions = mapOf(
                "metadata.author.permissions.canEdit" to true
            ))
            .build()

        val editablePost = JSONObject("""
            {
                "metadata": {
                    "author": {
                        "permissions": {
                            "canEdit": true
                        }
                    }
                }
            }
        """)

        val readOnlyPost = JSONObject("""
            {
                "metadata": {
                    "author": {
                        "permissions": {
                            "canEdit": false
                        }
                    }
                }
            }
        """)

        assertTrue(ability.can("read", "Post", editablePost))
        assertFalse(ability.can("read", "Post", readOnlyPost))
    }

    @Test
    fun `ability can() works with JSONObject - MongoDB operators`() {
        val ability = Ability.builder()
            .can("read", "Post", conditions = mapOf(
                "author.role" to mapOf("\$in" to listOf("admin", "editor"))
            ))
            .build()

        val adminPost = JSONObject("""
            {
                "author": {
                    "role": "admin"
                }
            }
        """)

        val userPost = JSONObject("""
            {
                "author": {
                    "role": "user"
                }
            }
        """)

        assertTrue(ability.can("read", "Post", adminPost))
        assertFalse(ability.can("read", "Post", userPost))
    }

    @Test
    fun `ability can() works with JSONObject - field-level permissions`() {
        val ability = Ability.builder()
            .can("update", "Post",
                conditions = mapOf("author.id" to 123),
                fields = listOf("title", "content")
            )
            .build()

        val myPost = JSONObject("""
            {
                "author": {"id": 123}
            }
        """)

        assertTrue(ability.can("update", "Post", myPost, "title"))
        assertTrue(ability.can("update", "Post", myPost, "content"))
        assertFalse(ability.can("update", "Post", myPost, "status"))
    }

    @Test
    fun `ability cannot() works with JSONObject`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .can("delete", "Post")  // Allow delete by default
            .cannot("delete", "Post", conditions = mapOf("published" to true))  // But not if published
            .build()

        val publishedPost = JSONObject("""{"published": true}""")
        val draftPost = JSONObject("""{"published": false}""")

        // Published post: cannot delete (matches cannot rule)
        assertTrue(ability.cannot("delete", "Post", publishedPost))

        // Draft post: can delete (matches can rule, doesn't match cannot rule)
        assertFalse(ability.cannot("delete", "Post", draftPost))
    }

    @Test
    fun `ability works with JSONObject from API response`() {
        // Simulate real-world scenario: JSON from API
        val currentUserId = 123

        val ability = Ability.builder()
            .can("update", "Post", conditions = mapOf("author.id" to currentUserId))
            .can("delete", "Post", conditions = mapOf("author.id" to currentUserId))
            .can("read", "Post")  // Anyone can read
            .build()

        // API response JSON
        val apiResponse = """
            {
                "id": 1,
                "title": "My Blog Post",
                "content": "Lorem ipsum...",
                "author": {
                    "id": 123,
                    "name": "John Doe",
                    "email": "john@example.com"
                },
                "metadata": {
                    "views": 100,
                    "likes": 50
                }
            }
        """

        val postJson = JSONObject(apiResponse)

        // Check permissions
        assertTrue(ability.can("read", "Post", postJson))
        assertTrue(ability.can("update", "Post", postJson))
        assertTrue(ability.can("delete", "Post", postJson))
    }

    @Test
    fun `ability works with JSONObject containing arrays`() {
        val ability = Ability.builder()
            .can("read", "Post", conditions = mapOf("tags.0" to "featured"))
            .build()

        val featuredPost = JSONObject("""
            {
                "tags": ["featured", "tech", "news"]
            }
        """)

        val regularPost = JSONObject("""
            {
                "tags": ["tech", "news"]
            }
        """)

        assertTrue(ability.can("read", "Post", featuredPost))
        assertFalse(ability.can("read", "Post", regularPost))
    }

    @Test
    fun `ability works with empty JSONObject`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .build()

        val emptyPost = JSONObject("{}")

        assertTrue(ability.can("read", "Post", emptyPost))
    }

    @Test
    fun `ability works with JSONObject containing null author`() {
        val ability = Ability.builder()
            .can("read", "Post")
            .cannot("update", "Post", conditions = mapOf("author.id" to 123))
            .build()

        val postWithNullAuthor = JSONObject()
        postWithNullAuthor.put("id", 1)
        postWithNullAuthor.put("author", JSONObject.NULL)

        // Can read (no conditions)
        assertTrue(ability.can("read", "Post", postWithNullAuthor))

        // Cannot update (author.id condition not met because author is null)
        assertFalse(ability.can("update", "Post", postWithNullAuthor))
    }

    @Test
    fun `ability works with complex real-world JSON`() {
        val currentUserId = 123
        val ability = Ability.builder()
            .can("update", "Post", conditions = mapOf(
                "author.id" to currentUserId,
                "status" to "draft",
                "metadata.locked" to false
            ))
            .build()

        val complexPost = JSONObject("""
            {
                "id": 1,
                "title": "My Post",
                "author": {
                    "id": 123,
                    "name": "John Doe",
                    "profile": {
                        "avatar": "https://...",
                        "bio": "Developer"
                    }
                },
                "status": "draft",
                "metadata": {
                    "locked": false,
                    "views": 100,
                    "comments": [
                        {"id": 1, "text": "Great!"},
                        {"id": 2, "text": "Thanks!"}
                    ]
                },
                "tags": ["kotlin", "android", "casl"]
            }
        """)

        assertTrue(ability.can("update", "Post", complexPost))
    }
}
