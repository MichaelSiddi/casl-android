package com.michaelsiddi.casl

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
     * - ForcedSubject instances (created with subject() helper)
     * - Kotlin data classes (via reflection)
     * - Java POJOs (via reflection)
     * - Maps (directly)
     */
    private fun extractFields(subject: Any): Map<String, Any?> = when (subject) {
        is ForcedSubject -> subject.toMap()
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
     * Deep equality comparison with type coercion and MongoDB-style operator support.
     *
     * Handles:
     * - Null values
     * - MongoDB-style operators ($in, $gt, $lt, etc.)
     * - Numeric type coercion
     * - Nested maps
     * - Nested lists
     * - Objects compared against maps (extracts fields from objects)
     * - Standard equality
     */
    private fun deepEquals(expected: Any?, actual: Any?): Boolean = when {
        expected == null && actual == null -> true
        expected == null || actual == null -> false
        // Check for MongoDB-style operators
        expected is Map<*, *> && isOperatorMap(expected) -> matchOperators(expected, actual)
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
     * Check if a map contains MongoDB-style operators.
     */
    private fun isOperatorMap(map: Map<*, *>): Boolean {
        return map.keys.any { it is String && it.toString().startsWith("$") }
    }

    /**
     * Match value against MongoDB-style operators.
     *
     * Supported operators:
     * - $eq: equals
     * - $ne: not equals
     * - $gt: greater than
     * - $gte: greater than or equals
     * - $lt: less than
     * - $lte: less than or equals
     * - $in: in array
     * - $nin: not in array
     * - $exists: field exists
     * - $regex: regex pattern matching
     * - $size: array size
     * - $all: array contains all elements
     * - $elemMatch: array element matches condition
     */
    private fun matchOperators(operators: Map<*, *>, actual: Any?): Boolean {
        return operators.all { (op, value) ->
            val operator = op.toString()
            when (operator) {
                "\$eq" -> deepEquals(value, actual)
                "\$ne" -> !deepEquals(value, actual)
                "\$gt" -> compareNumbers(actual, value) { a, b -> a > b }
                "\$gte" -> compareNumbers(actual, value) { a, b -> a >= b }
                "\$lt" -> compareNumbers(actual, value) { a, b -> a < b }
                "\$lte" -> compareNumbers(actual, value) { a, b -> a <= b }
                "\$in" -> matchIn(actual, value)
                "\$nin" -> !matchIn(actual, value)
                "\$exists" -> {
                    val shouldExist = value as? Boolean ?: true
                    (actual != null) == shouldExist
                }
                "\$regex" -> matchRegex(actual, value)
                "\$size" -> matchSize(actual, value)
                "\$all" -> matchAll(actual, value)
                "\$elemMatch" -> matchElemMatch(actual, value)
                else -> {
                    // Unknown operator, treat as regular field
                    deepEquals(value, actual)
                }
            }
        }
    }

    /**
     * Compare numbers with a comparison function.
     */
    private fun compareNumbers(actual: Any?, expected: Any?, compare: (Double, Double) -> Boolean): Boolean {
        if (actual == null || expected == null) return false
        if (!isNumeric(actual) || !isNumeric(expected)) return false
        return compare(toDouble(actual), toDouble(expected))
    }

    /**
     * Match $in operator - check if value is in array.
     */
    private fun matchIn(actual: Any?, expected: Any?): Boolean {
        if (expected !is List<*>) return false
        return expected.any { deepEquals(it, actual) }
    }

    /**
     * Match $regex operator - check if string matches regex.
     */
    private fun matchRegex(actual: Any?, pattern: Any?): Boolean {
        if (actual !is String || pattern !is String) return false
        return try {
            Regex(pattern).matches(actual)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Match $size operator - check array size.
     */
    private fun matchSize(actual: Any?, expected: Any?): Boolean {
        if (actual !is List<*> || expected == null || !isNumeric(expected)) return false
        return actual.size == (expected as Number).toInt()
    }

    /**
     * Match $all operator - check if array contains all elements.
     */
    private fun matchAll(actual: Any?, expected: Any?): Boolean {
        if (actual !is List<*> || expected !is List<*>) return false
        return expected.all { expectedItem ->
            actual.any { actualItem -> deepEquals(expectedItem, actualItem) }
        }
    }

    /**
     * Match $elemMatch operator - check if array has element matching condition.
     */
    private fun matchElemMatch(actual: Any?, expected: Any?): Boolean {
        if (actual !is List<*>) return false
        if (expected !is Map<*, *>) return false

        return actual.any { element ->
            if (element == null) return@any false
            val elementFields = when (element) {
                is Map<*, *> -> element.mapKeys { it.key.toString() }.mapValues { it.value }
                else -> extractFields(element)
            }
            // Check if all conditions in expected match the element
            expected.all { (key, value) ->
                val keyStr = key.toString()
                val actualValue = elementFields[keyStr]
                deepEquals(value, actualValue)
            }
        }
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
        // Expected map can have fewer keys than actual when matching conditions
        // (actual object may have more fields than what we're checking)
        // Only check that all expected keys exist and match in actual
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
