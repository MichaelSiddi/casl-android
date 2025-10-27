package com.michaelsiddi.casl.extra

import com.michaelsiddi.casl.Ability

/**
 * Extracts rules condition values into an object of default values.
 *
 * This is useful for creating default form values based on authorization rules.
 * For example, if a user can only create posts with authorId = 123, this function
 * will extract { "authorId": 123 } from the rules.
 *
 * Rules:
 * - Only processes non-inverted rules (positive "can" rules)
 * - Only processes rules with conditions
 * - Skips plain object values (MongoDB query operators like $in, $gt, etc.)
 * - Supports dot notation for nested fields (e.g., "author.id" -> { author: { id: value } })
 *
 * Example:
 * ```kotlin
 * val ability = Ability.builder()
 *     .can("create", "Post", mapOf("authorId" to 123))
 *     .can("create", "Post", mapOf("status" to "draft"))
 *     .build()
 *
 * val fields = rulesToFields(ability, "create", "Post")
 * // Result: { "authorId": 123, "status": "draft" }
 * ```
 *
 * @param ability The Ability instance to extract from
 * @param action The action to get rules for
 * @param subjectType The subject type to get rules for
 * @return Map of field names to default values
 */
public fun rulesToFields(
    ability: Ability,
    action: String,
    subjectType: String
): Map<String, Any?> {
    val rules = ability.rulesFor(action, subjectType)
    val result = mutableMapOf<String, Any?>()

    for (rule in rules) {
        // Skip inverted rules (cannot rules)
        if (rule.inverted) continue

        // Skip rules without conditions
        val conditions = rule.conditions ?: continue

        // Process each condition
        for ((fieldName, value) in conditions) {
            // Skip plain object values (MongoDB query operators)
            // A plain object is a Map that wasn't created from a custom class
            if (value != null && value is Map<*, *>) {
                // Skip this condition - it's likely a MongoDB operator like $in, $gt, etc.
                continue
            }

            // Set the value using dot notation support
            setByPath(result, fieldName, value)
        }
    }

    return result
}

/**
 * Set a value in a map using dot notation path.
 *
 * Examples:
 * - setByPath(map, "id", 5) -> { "id": 5 }
 * - setByPath(map, "author.id", 5) -> { "author": { "id": 5 } }
 * - setByPath(map, "author.address.city", "NYC") -> { "author": { "address": { "city": "NYC" } } }
 *
 * @param target The map to set the value in
 * @param path The dot-notation path
 * @param value The value to set
 */
private fun setByPath(target: MutableMap<String, Any?>, path: String, value: Any?) {
    if ('.' !in path) {
        // Simple case: no dots, just set directly
        target[path] = value
        return
    }

    // Complex case: dot notation
    val keys = path.split('.')
    var current: MutableMap<String, Any?> = target

    // Navigate/create nested maps for all keys except the last
    for (i in 0 until keys.size - 1) {
        val key = keys[i]
        val next = current[key]

        @Suppress("UNCHECKED_CAST")
        current = if (next is MutableMap<*, *>) {
            next as MutableMap<String, Any?>
        } else {
            // Create new nested map
            val newMap = mutableMapOf<String, Any?>()
            current[key] = newMap
            newMap
        }
    }

    // Set the final value
    current[keys.last()] = value
}
