package com.michaelsiddi.casl

/**
 * Special property key used to store the forced subject type on objects.
 * Matches casl-js's __caslSubjectType__ field.
 */
private const val CASL_SUBJECT_TYPE_KEY = "__caslSubjectType__"

/**
 * Wrapper class that holds an object with a forced subject type.
 * This is similar to ForcedSubject in casl-js.
 */
public data class ForcedSubject internal constructor(
    val type: String,
    val attributes: Map<String, Any?>
) {
    /**
     * Get the forced subject type.
     */
    internal fun getSubjectType(): String = type

    /**
     * Get an attribute value by key.
     */
    public operator fun get(key: String): Any? = attributes[key]

    /**
     * Check if an attribute exists.
     */
    public fun has(key: String): Boolean = attributes.containsKey(key)

    /**
     * Get all attributes as a map.
     */
    public fun toMap(): Map<String, Any?> = attributes
}

/**
 * Creates a subject object with a forced type.
 *
 * This is useful when you want to check permissions on an object that doesn't
 * have a corresponding Kotlin class, or when you want to override the detected type.
 *
 * Matches the `subject()` function in casl-js.
 *
 * Example usage (Kotlin):
 * ```kotlin
 * val user = subject("User", mapOf(
 *     "id" to 123,
 *     "role" to "admin"
 * ))
 * ability.can("read", user)
 * ```
 *
 * Example usage (Java):
 * ```java
 * Map<String, Object> attrs = new HashMap<>();
 * attrs.put("id", 123);
 * attrs.put("role", "admin");
 * ForcedSubject user = SubjectKt.subject("User", attrs);
 * ability.can("read", user);
 * ```
 *
 * @param type The subject type name (e.g., "User", "BlogPost")
 * @param attributes The object attributes as a map
 * @return A ForcedSubject instance with the specified type and attributes
 */
public fun subject(type: String, attributes: Map<String, Any?> = emptyMap()): ForcedSubject {
    require(type.isNotBlank()) { "Subject type must not be blank" }
    return ForcedSubject(type, attributes)
}

/**
 * Kotlin convenience function that accepts vararg pairs.
 *
 * Example:
 * ```kotlin
 * val user = subject("User",
 *     "id" to 123,
 *     "role" to "admin"
 * )
 * ```
 */
public fun subject(type: String, vararg attributes: Pair<String, Any?>): ForcedSubject {
    return subject(type, mapOf(*attributes))
}
