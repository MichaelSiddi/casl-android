package com.casl

import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Utility for matching conditions against subjects using deep equality.
 *
 * Handles:
 * - Nested objects and maps
 * - Lists and arrays
 * - Numeric type coercion (Int, Long, Double)
 * - Null values
 *
 * Thread-safe: This object is stateless and can be called from multiple threads.
 */
public object ConditionMatcher {

    /**
     * Check if subject matches all conditions.
     *
     * @param conditions Map of field names to expected values
     * @param subject The subject to check conditions against
     * @return true if all conditions match, false otherwise
     */
    public fun matches(conditions: Map<String, Any?>?, subject: Any?): Boolean {
        if (conditions == null || conditions.isEmpty()) return true
        if (subject == null) return false

        val subjectFields = extractFields(subject)

        return conditions.all { (key, expectedValue) ->
            val actualValue = subjectFields[key]
            deepEquals(expectedValue, actualValue)
        }
    }

    /**
     * Extract fields from a subject as a map.
     *
     * Supports:
     * - Kotlin data classes (via reflection)
     * - Java POJOs (via reflection)
     * - Maps (directly)
     */
    private fun extractFields(subject: Any): Map<String, Any?> = when (subject) {
        is Map<*, *> -> subject.mapKeys { it.key.toString() }.mapValues { it.value }
        else -> {
            try {
                // Try Kotlin reflection first
                subject::class.memberProperties
                    .associate { prop ->
                        prop.isAccessible = true
                        prop.name to prop.call(subject)
                    }
            } catch (e: Exception) {
                // Fallback to Java reflection
                extractFieldsViaJavaReflection(subject)
            }
        }
    }

    /**
     * Extract fields using Java reflection (fallback method).
     */
    private fun extractFieldsViaJavaReflection(subject: Any): Map<String, Any?> {
        return try {
            subject.javaClass.declaredFields
                .associate { field ->
                    field.isAccessible = true
                    field.name to field.get(subject)
                }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Deep equality comparison with type coercion.
     *
     * Handles:
     * - Null values
     * - Numeric type coercion
     * - Nested maps
     * - Nested lists
     * - Objects compared against maps (extracts fields from objects)
     * - Standard equality
     */
    private fun deepEquals(expected: Any?, actual: Any?): Boolean = when {
        expected == null && actual == null -> true
        expected == null || actual == null -> false
        expected::class == actual::class -> expected == actual
        isNumeric(expected) && isNumeric(actual) -> numericEquals(expected, actual)
        expected is Map<*, *> && actual is Map<*, *> -> mapsEqual(expected, actual)
        expected is Map<*, *> && actual !is Map<*, *> -> {
            // Compare map against object by extracting object fields
            val actualFields = extractFields(actual)
            mapsEqual(expected, actualFields)
        }
        expected is List<*> && actual is List<*> -> listsEqual(expected, actual)
        else -> expected == actual
    }

    /**
     * Check if value is a numeric type.
     */
    private fun isNumeric(value: Any): Boolean =
        value is Number

    /**
     * Compare numeric values with type coercion.
     */
    private fun numericEquals(expected: Any, actual: Any): Boolean {
        val expectedDouble = toDouble(expected)
        val actualDouble = toDouble(actual)
        return expectedDouble == actualDouble
    }

    /**
     * Convert numeric value to Double for comparison.
     */
    private fun toDouble(value: Any): Double = when (value) {
        is Number -> value.toDouble()
        else -> 0.0
    }

    /**
     * Deep equality for maps.
     */
    private fun mapsEqual(expected: Map<*, *>, actual: Map<*, *>): Boolean {
        if (expected.size != actual.size) return false
        return expected.all { (key, value) ->
            actual.containsKey(key) && deepEquals(value, actual[key])
        }
    }

    /**
     * Deep equality for lists.
     */
    private fun listsEqual(expected: List<*>, actual: List<*>): Boolean {
        if (expected.size != actual.size) return false
        return expected.indices.all { i ->
            deepEquals(expected[i], actual[i])
        }
    }
}
