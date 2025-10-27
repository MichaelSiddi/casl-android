package com.michaelsiddi.casl

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
public class Ability internal constructor(
    rules: List<Rule>,
    private val options: AbilityOptions = AbilityOptions()
) {

    @Volatile
    private var ruleIndex: RuleIndex = RuleIndex.fromRules(rules, options)

    private val eventEmitter = EventEmitter()

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
     * Check if action is permitted on a JSONObject subject.
     *
     * This overload automatically converts the JSONObject to a Map and creates
     * a ForcedSubject with the specified type.
     *
     * Example (Kotlin):
     * ```kotlin
     * val postJson = JSONObject("""{"author": {"id": 123}}""")
     * val canUpdate = ability.can("update", "Post", postJson)
     * ```
     *
     * Example (Java):
     * ```java
     * JSONObject postJson = new JSONObject("{\"author\": {\"id\": 123}}");
     * boolean canUpdate = ability.can("update", "Post", postJson);
     * ```
     *
     * @param action The action to check (e.g., "read", "update", "delete")
     * @param subjectType The subject type name (e.g., "Post", "User")
     * @param json The JSONObject containing the subject's attributes
     * @param field Optional field name for field-level permissions
     * @return true if permitted, false otherwise (default deny)
     *
     * @throws IllegalArgumentException if action or subjectType is blank
     */
    @JvmOverloads
    public fun can(
        action: String,
        subjectType: String,
        json: org.json.JSONObject,
        field: String? = null
    ): Boolean {
        require(subjectType.isNotBlank()) { "subjectType must not be blank" }
        val subject = subjectFromJson(subjectType, json)
        return can(action, subject, field)
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
     * Check if action is prohibited on a JSONObject subject.
     *
     * This overload automatically converts the JSONObject to a Map and creates
     * a ForcedSubject with the specified type.
     *
     * Example:
     * ```kotlin
     * val postJson = JSONObject("""{"author": {"id": 123}}""")
     * val cannotUpdate = ability.cannot("update", "Post", postJson)
     * ```
     *
     * @param action The action to check
     * @param subjectType The subject type name
     * @param json The JSONObject containing the subject's attributes
     * @param field Optional field name for field-level permissions
     * @return true if prohibited, false otherwise
     *
     * @throws IllegalArgumentException if action or subjectType is blank
     */
    @JvmOverloads
    public fun cannot(
        action: String,
        subjectType: String,
        json: org.json.JSONObject,
        field: String? = null
    ): Boolean {
        return !can(action, subjectType, json, field)
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
     * Emits UPDATE event before updating, and UPDATED event after.
     * Ongoing permission checks continue using the old rule set.
     * New permission checks use the new rule set immediately after update completes.
     *
     * @param rawRules The new rules in serializable format
     * @throws IllegalArgumentException if any rule is invalid
     * @throws IllegalStateException if rules cannot be applied
     */
    public fun update(rawRules: List<RawRule>) {
        // Emit UPDATE event before changing rules
        eventEmitter.emit(EventType.UPDATE, UpdateEvent(this, rawRules))

        synchronized(this) {
            val internalRules = rawRules.map { it.toRule(options.resolveAction) }
            ruleIndex = RuleIndex.fromRules(internalRules, options)
        }

        // Emit UPDATED event after rules are changed
        eventEmitter.emit(EventType.UPDATED, UpdateEvent(this, rawRules))
    }

    /**
     * Subscribe to ability events.
     *
     * Available events:
     * - UPDATE: fired before rules are updated
     * - UPDATED: fired after rules have been updated
     *
     * Example:
     * ```kotlin
     * val unsubscribe = ability.on(EventType.UPDATED) { event ->
     *     println("Rules updated: ${event.rules.size} rules")
     * }
     *
     * // Later, to unsubscribe:
     * unsubscribe()
     * ```
     *
     * @param eventType The event type to listen to
     * @param handler The handler function called when event fires
     * @return Function to unsubscribe this handler
     */
    public fun on(eventType: EventType, handler: EventHandler): Unsubscribe {
        return eventEmitter.on(eventType, handler)
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

    /**
     * Get all rules that match the given action and subject type.
     *
     * Returns rules in order of priority (first to last), allowing inspection
     * of what rules would be considered for a permission check.
     *
     * @param action The action to match
     * @param subjectType The subject type (as string, e.g., "Post")
     * @param field Optional field name to filter rules
     * @return List of matching rules
     */
    @JvmOverloads
    public fun rulesFor(action: String, subjectType: String, field: String? = null): List<RawRule> {
        val snapshot = ruleIndex
        return snapshot.rulesFor(action, subjectType, field).map { it.toRawRule() }
    }

    /**
     * Get all actions defined for a given subject type.
     *
     * Useful for building UI that shows what actions a user can perform
     * on a particular resource type.
     *
     * @param subjectType The subject type (as string, e.g., "Post")
     * @return Set of action names
     */
    public fun actionsFor(subjectType: String): Set<String> {
        val snapshot = ruleIndex
        return snapshot.actionsFor(subjectType)
    }

    /**
     * Get the relevant rule for a specific permission check.
     *
     * Returns the rule that would determine the outcome of a can() check,
     * following last-match-wins precedence. Useful for understanding why
     * a permission was granted or denied.
     *
     * @param action The action to check
     * @param subject The subject instance or type
     * @param field Optional field name
     * @return The relevant rule, or null if no rule matches
     */
    @JvmOverloads
    public fun relevantRuleFor(action: String, subject: Any?, field: String? = null): RawRule? {
        val snapshot = ruleIndex
        return snapshot.findMatchingRule(action, subject, field)?.toRawRule()
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
         * @param options Optional configuration for alias resolution and custom options
         * @return Ability instance with the given rules
         * @throws IllegalArgumentException if any rule is invalid
         */
        @JvmStatic
        @JvmOverloads
        public fun fromRules(
            rawRules: List<RawRule>,
            options: AbilityOptions = AbilityOptions()
        ): Ability {
            val internalRules = rawRules.map { it.toRule(options.resolveAction) }
            return Ability(internalRules, options)
        }
    }
}
