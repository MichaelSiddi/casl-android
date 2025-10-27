package com.michaelsiddi.casl

import org.json.JSONArray
import org.json.JSONObject

/**
 * Utility functions for working with JSON in CASL.
 *
 * These functions provide seamless integration between Android's JSONObject/JSONArray
 * and CASL's Map-based subject system.
 */

/**
 * Converts a JSONObject to a nested Map structure suitable for CASL subjects.
 *
 * This handles:
 * - Nested JSONObjects → nested Maps
 * - JSONArrays → Lists
 * - JSONObject.NULL → null
 * - Primitive values (String, Int, Double, Boolean, etc.)
 *
 * Example:
 * ```kotlin
 * val json = JSONObject("""
 *     {
 *         "id": 123,
 *         "author": {
 *             "id": 456,
 *             "name": "John"
 *         }
 *     }
 * """)
 *
 * val map = jsonObjectToMap(json)
 * // Result: mapOf(
 * //     "id" to 123,
 * //     "author" to mapOf("id" to 456, "name" to "John")
 * // )
 * ```
 *
 * @param json The JSONObject to convert
 * @return A Map<String, Any?> with nested structure preserved
 */
public fun jsonObjectToMap(json: JSONObject): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    json.keys().forEach { key ->
        val value = json.get(key)
        map[key] = when (value) {
            JSONObject.NULL -> null
            is JSONObject -> jsonObjectToMap(value)
            is JSONArray -> jsonArrayToList(value)
            else -> value
        }
    }
    return map
}

/**
 * Converts a JSONArray to a List, recursively converting nested objects.
 *
 * Example:
 * ```kotlin
 * val jsonArray = JSONArray("""[{"id": 1}, {"id": 2}]""")
 * val list = jsonArrayToList(jsonArray)
 * // Result: listOf(mapOf("id" to 1), mapOf("id" to 2))
 * ```
 *
 * @param jsonArray The JSONArray to convert
 * @return A List<Any?> with nested structures converted
 */
public fun jsonArrayToList(jsonArray: JSONArray): List<Any?> {
    return (0 until jsonArray.length()).map { i ->
        when (val value = jsonArray.get(i)) {
            JSONObject.NULL -> null
            is JSONObject -> jsonObjectToMap(value)
            is JSONArray -> jsonArrayToList(value)
            else -> value
        }
    }
}

/**
 * Creates a ForcedSubject from a JSONObject.
 *
 * This is a convenience function that combines jsonObjectToMap() and subject().
 *
 * Example:
 * ```kotlin
 * val postJson = JSONObject("""
 *     {
 *         "id": 1,
 *         "author": {"id": 123}
 *     }
 * """)
 *
 * val post = subjectFromJson("Post", postJson)
 * ability.can("update", post)
 * ```
 *
 * @param type The subject type name (e.g., "Post", "User")
 * @param json The JSONObject containing the subject's attributes
 * @return A ForcedSubject with attributes from the JSON
 */
public fun subjectFromJson(type: String, json: JSONObject): ForcedSubject {
    val attributes = jsonObjectToMap(json)
    return subject(type, attributes)
}
