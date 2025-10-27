package com.michaelsiddi.casl.extra

import com.michaelsiddi.casl.RawRule

/**
 * Packed rule representation as a tuple for efficient serialization.
 *
 * Format: [action, subject, conditions?, inverted?, fields?, reason?]
 * - Trailing falsy values are omitted for space efficiency
 * - Array values are joined with commas
 */
public typealias PackedRule = List<Any>

/**
 * Function type for converting subject type to string for packing.
 */
public typealias PackSubjectType = (String) -> String

/**
 * Function type for converting string back to subject type for unpacking.
 */
public typealias UnpackSubjectType = (String) -> String

/**
 * Pack rules into a compact array format for efficient network transfer or storage.
 *
 * Rules are converted to tuples with trailing empty values removed:
 * ```
 * [action, subject, conditions, inverted, fields, reason]
 * ```
 *
 * Example:
 * ```kotlin
 * val rules = listOf(
 *     RawRule(action = listOf("read", "update"), subject = "Post"),
 *     RawRule(action = "delete", subject = "Post", inverted = true)
 * )
 *
 * val packed = packRules(rules)
 * // [
 * //   ["read,update", "Post"],
 * //   ["delete", "Post", 0, 1]
 * // ]
 * ```
 *
 * @param rules List of rules to pack
 * @param packSubject Optional function to transform subject types
 * @return List of packed rules (tuples)
 */
public fun packRules(
    rules: List<RawRule>,
    packSubject: PackSubjectType? = null
): List<PackedRule> {
    return rules.map { rule ->
        // Convert action to comma-separated string
        val action = when (rule.action) {
            is String -> rule.action as String
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                (rule.action as List<String>).joinToString(",")
            }
            else -> rule.action.toString()
        }

        // Convert subject to string (possibly transformed)
        val subject = packSubject?.invoke(rule.subject) ?: rule.subject

        // Build packed rule array
        val packed = mutableListOf<Any>(
            action,
            subject,
            rule.conditions ?: 0,
            if (rule.inverted) 1 else 0,
            rule.fields?.joinToString(",") ?: 0,
            "" // reason placeholder (not used in casl-android yet)
        )

        // Remove trailing falsy values for space efficiency
        while (packed.isNotEmpty() && !isTruthy(packed.last())) {
            packed.removeLast()
        }

        packed.toList()
    }
}

/**
 * Unpack rules from compact array format back to RawRule objects.
 *
 * Example:
 * ```kotlin
 * val packed = listOf(
 *     listOf("read,update", "Post"),
 *     listOf("delete", "Post", 0, 1)
 * )
 *
 * val rules = unpackRules(packed)
 * // [
 * //   RawRule(action = listOf("read", "update"), subject = "Post"),
 * //   RawRule(action = "delete", subject = "Post", inverted = true)
 * // ]
 * ```
 *
 * @param packedRules List of packed rule tuples
 * @param unpackSubject Optional function to transform subject strings
 * @return List of unpacked RawRule objects
 */
public fun unpackRules(
    packedRules: List<PackedRule>,
    unpackSubject: UnpackSubjectType? = null
): List<RawRule> {
    return packedRules.map { packed ->
        val action = (packed[0] as String).split(",")
        val subjectStr = packed[1] as String
        val subject = unpackSubject?.invoke(subjectStr) ?: subjectStr

        // Extract optional fields (with safe indexing)
        val conditions = packed.getOrNull(2)?.let {
            if (it == 0 || it == "0") null else {
                @Suppress("UNCHECKED_CAST")
                it as? Map<String, Any?>
            }
        }

        val inverted = packed.getOrNull(3)?.let {
            when (it) {
                is Number -> it.toInt() == 1
                is String -> it == "1"
                is Boolean -> it
                else -> false
            }
        } ?: false

        val fields = packed.getOrNull(4)?.let {
            if (it == 0 || it == "0" || it == "") null
            else (it as String).split(",")
        }

        // Convert action to appropriate type
        val finalAction: Any = if (action.size == 1) action[0] else action

        RawRule(
            action = finalAction,
            subject = subject,
            conditions = conditions,
            fields = fields,
            inverted = inverted
        )
    }
}

/**
 * Check if a value is "truthy" for packing purposes.
 * Used to determine which trailing values can be omitted.
 */
private fun isTruthy(value: Any?): Boolean {
    return when (value) {
        null -> false
        is Boolean -> value
        is Number -> value.toDouble() != 0.0
        is String -> value.isNotEmpty()
        is Collection<*> -> value.isNotEmpty()
        is Map<*, *> -> value.isNotEmpty()
        else -> true
    }
}
