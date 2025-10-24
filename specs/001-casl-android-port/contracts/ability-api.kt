/**
 * CASL Android Authorization Library - Public API Contract
 *
 * This file defines the public API surface that consumers depend on.
 * Changes to this API are breaking changes requiring major version bump.
 *
 * Feature: 001-casl-android-port
 * Date: 2025-10-23
 */

package com.casl

import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Main authorization manager. Stores rules and evaluates permissions.
 *
 * Thread-safe: All methods can be called from multiple threads concurrently.
 *
 * Example:
 * ```kotlin
 * val ability = Ability.builder()
 *     .can("read", "BlogPost")
 *     .cannot("delete", "BlogPost")
 *     .build()
 *
 * if (ability.can("read", blogPost)) {
 *     // Allow access
 * }
 * ```
 *
 * Java example:
 * ```java
 * Ability ability = Ability.builder()
 *     .can("read", "BlogPost")
 *     .cannot("delete", "BlogPost")
 *     .build();
 *
 * if (ability.can("read", blogPost)) {
 *     // Allow access
 * }
 * ```
 */
class Ability private constructor(rules: List<Rule>) {

    /**
     * Check if action is permitted on subject.
     *
     * @param action The action to check (e.g., "read", "update", "delete")
     * @param subject The resource being accessed (or type name as String)
     * @param field Optional field name for field-level permissions
     * @return true if permitted, false otherwise (default deny)
     *
     * @throws IllegalArgumentException if action is blank
     */
    @JvmOverloads
    fun can(action: String, subject: Any?, field: String? = null): Boolean

    /**
     * Check if action is prohibited on subject. Opposite of can().
     *
     * @param action The action to check
     * @param subject The resource being accessed (or type name as String)
     * @param field Optional field name for field-level permissions
     * @return true if prohibited, false otherwise
     *
     * @throws IllegalArgumentException if action is blank
     */
    @JvmOverloads
    fun cannot(action: String, subject: Any?, field: String? = null): Boolean

    /**
     * Asynchronous version of can(). For use with Kotlin coroutines.
     *
     * Note: Current implementation delegates to synchronous can() as permission
     * checks complete in <1ms. This signature exists for API consistency and
     * future-proofing if async work becomes necessary.
     *
     * @param action The action to check
     * @param subject The resource being accessed (or type name as String)
     * @param field Optional field name for field-level permissions
     * @return true if permitted, false otherwise
     *
     * @throws IllegalArgumentException if action is blank
     */
    @JvmOverloads
    suspend fun canAsync(action: String, subject: Any?, field: String? = null): Boolean

    /**
     * Asynchronous version of cannot(). For use with Kotlin coroutines.
     *
     * @param action The action to check
     * @param subject The resource being accessed (or type name as String)
     * @param field Optional field name for field-level permissions
     * @return true if prohibited, false otherwise
     *
     * @throws IllegalArgumentException if action is blank
     */
    @JvmOverloads
    suspend fun cannotAsync(action: String, subject: Any?, field: String? = null): Boolean

    /**
     * Replace all rules atomically. Thread-safe.
     *
     * Ongoing permission checks continue using the old rule set.
     * New permission checks use the new rule set immediately after update completes.
     *
     * @param rawRules The new rules in serializable format
     * @throws IllegalArgumentException if any rule is invalid
     * @throws IllegalStateException if rules cannot be applied
     */
    fun update(rawRules: List<RawRule>)

    /**
     * Export current rules for serialization (e.g., to JSON).
     *
     * The returned list is a snapshot - modifications do not affect the Ability.
     *
     * @return List of rules in serializable format
     */
    fun exportRules(): List<RawRule>

    companion object {
        /**
         * Create a new builder for defining rules.
         *
         * @return A new AbilityBuilder instance
         */
        @JvmStatic
        fun builder(): AbilityBuilder

        /**
         * Create Ability from pre-existing rules.
         *
         * @param rawRules List of rules in serializable format
         * @return Ability instance with the given rules
         * @throws IllegalArgumentException if any rule is invalid
         */
        @JvmStatic
        fun fromRules(rawRules: List<RawRule>): Ability
    }
}

/**
 * Fluent builder for creating Ability instances.
 *
 * Not thread-safe: Each builder should be used from a single thread.
 *
 * Example:
 * ```kotlin
 * val ability = AbilityBuilder()
 *     .can("read", "BlogPost")
 *     .can("update", "BlogPost", conditions = mapOf("authorId" to userId))
 *     .cannot("delete", "BlogPost", conditions = mapOf("published" to true))
 *     .build()
 * ```
 */
class AbilityBuilder {

    /**
     * Add a positive permission rule.
     *
     * @param action The action to permit (e.g., "read", "update")
     * @param subject The subject type (e.g., "BlogPost", "Comment")
     * @param conditions Optional attribute matchers (e.g., mapOf("authorId" to currentUserId))
     * @param fields Optional field restrictions (e.g., listOf("title", "content"))
     * @return this builder for chaining
     *
     * @throws IllegalArgumentException if action or subject is blank
     */
    @JvmOverloads
    fun can(
        action: String,
        subject: String,
        conditions: Map<String, Any?>? = null,
        fields: List<String>? = null
    ): AbilityBuilder

    /**
     * Add a negative permission rule (prohibition).
     *
     * @param action The action to prohibit
     * @param subject The subject type
     * @param conditions Optional attribute matchers
     * @param fields Optional field restrictions
     * @return this builder for chaining
     *
     * @throws IllegalArgumentException if action or subject is blank
     */
    @JvmOverloads
    fun cannot(
        action: String,
        subject: String,
        conditions: Map<String, Any?>? = null,
        fields: List<String>? = null
    ): AbilityBuilder

    /**
     * Remove all accumulated rules from this builder.
     *
     * @return this builder for chaining
     */
    fun clear(): AbilityBuilder

    /**
     * Build the Ability instance with accumulated rules.
     *
     * This builder can continue to be used after build() to create additional
     * Ability instances.
     *
     * @return A new Ability instance
     * @throws IllegalStateException if builder state is invalid
     */
    fun build(): Ability
}

/**
 * Serializable representation of an authorization rule.
 *
 * Suitable for JSON encoding/decoding and transmission between client and server
 * for isomorphic authorization.
 *
 * Example JSON:
 * ```json
 * {
 *   "action": "read",
 *   "subject": "BlogPost",
 *   "conditions": { "authorId": "user123" },
 *   "fields": ["title", "content"],
 *   "inverted": false
 * }
 * ```
 */
data class RawRule(
    /**
     * The action this rule applies to (e.g., "read", "update", "delete").
     * Must not be blank.
     */
    val action: String,

    /**
     * The subject type this rule applies to (e.g., "BlogPost", "Comment").
     * Must not be blank.
     */
    val subject: String,

    /**
     * Optional conditions that must match subject attributes.
     * Keys are attribute names, values are expected values.
     * Supports nested maps and lists for complex matching.
     */
    val conditions: Map<String, Any?>? = null,

    /**
     * Optional field restrictions.
     * If specified, rule only applies to these specific fields.
     * If null, rule applies to entire resource.
     */
    val fields: List<String>? = null,

    /**
     * True for prohibition rules (cannot), false for permission rules (can).
     */
    val inverted: Boolean = false
) {
    init {
        require(action.isNotBlank()) { "action must not be blank" }
        require(subject.isNotBlank()) { "subject must not be blank" }
        require(fields == null || fields.isNotEmpty()) { "fields must not be empty if specified" }
    }

    companion object {
        /**
         * Parse RawRule from JSON string.
         *
         * @param json JSON string representation
         * @return Parsed RawRule
         * @throws org.json.JSONException if JSON is malformed
         * @throws IllegalArgumentException if required fields are missing or invalid
         */
        @JvmStatic
        fun fromJson(json: String): RawRule

        /**
         * Parse multiple RawRules from JSON array string.
         *
         * @param json JSON array string representation
         * @return List of parsed RawRules
         * @throws org.json.JSONException if JSON is malformed
         * @throws IllegalArgumentException if any rule is invalid
         */
        @JvmStatic
        fun listFromJson(json: String): List<RawRule>
    }

    /**
     * Serialize this rule to JSON string.
     *
     * @return JSON string representation
     */
    fun toJson(): String

    /**
     * Serialize list of rules to JSON array string.
     *
     * @return JSON array string representation
     */
    companion object {
        /**
         * Serialize list of rules to JSON array string.
         *
         * @param rules List of rules to serialize
         * @return JSON array string representation
         */
        @JvmStatic
        fun listToJson(rules: List<RawRule>): String
    }
}
