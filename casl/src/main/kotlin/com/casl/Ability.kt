package com.casl

import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Main authorization manager. Stores rules and evaluates permissions.
 *
 * Thread-safe: All methods can be called from multiple threads concurrently.
 *
 * Example usage (Kotlin):
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
 * Example usage (Java):
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
public class Ability internal constructor(rules: List<Rule>) {

    @Volatile
    private var ruleIndex: RuleIndex = RuleIndex.fromRules(rules)

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
    public fun can(action: String, subject: Any?, field: String? = null): Boolean {
        require(action.isNotBlank()) { "action must not be blank" }

        if (subject == null) return false

        val snapshot = ruleIndex // Volatile read for thread visibility
        val matchingRule = snapshot.findMatchingRule(action, subject, field)

        // Default deny: if no rule matches, deny access
        if (matchingRule == null) return false

        // If rule is inverted (cannot), deny access
        return !matchingRule.inverted
    }

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
    public fun cannot(action: String, subject: Any?, field: String? = null): Boolean {
        return !can(action, subject, field)
    }

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
    public suspend fun canAsync(action: String, subject: Any?, field: String? = null): Boolean {
        return can(action, subject, field)
    }

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
    public suspend fun cannotAsync(action: String, subject: Any?, field: String? = null): Boolean {
        return cannot(action, subject, field)
    }

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
    public fun update(rawRules: List<RawRule>) {
        synchronized(this) {
            val internalRules = rawRules.map { it.toRule() }
            ruleIndex = RuleIndex.fromRules(internalRules)
        }
    }

    /**
     * Export current rules for serialization (e.g., to JSON).
     *
     * The returned list is a snapshot - modifications do not affect the Ability.
     *
     * @return List of rules in serializable format
     */
    public fun exportRules(): List<RawRule> {
        val snapshot = ruleIndex
        return snapshot.getAllRules().map { it.toRawRule() }
    }

    public companion object {
        /**
         * Create a new builder for defining rules.
         *
         * @return A new AbilityBuilder instance
         */
        @JvmStatic
        public fun builder(): AbilityBuilder = AbilityBuilder()

        /**
         * Create Ability from pre-existing rules.
         *
         * @param rawRules List of rules in serializable format
         * @return Ability instance with the given rules
         * @throws IllegalArgumentException if any rule is invalid
         */
        @JvmStatic
        public fun fromRules(rawRules: List<RawRule>): Ability {
            val internalRules = rawRules.map { it.toRule() }
            return Ability(internalRules)
        }
    }
}
