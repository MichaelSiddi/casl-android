package com.michaelsiddi.casl

/**
 * Internal representation of an authorization rule.
 *
 * This class is immutable once created and optimized for permission checking.
 * Use [RawRule] for JSON serialization.
 *
 * @property action The action(s) being authorized (single string or list of strings)
 * @property subjectType Type of resource (e.g., "BlogPost", "Comment")
 * @property conditions Optional attribute matchers for conditional rules
 * @property fields Optional field restrictions (null means all fields)
 * @property inverted True for "cannot" rules, false for "can" rules
 */
internal data class Rule(
    val action: Any, // Can be String or List<String>
    val subjectType: String,
    val conditions: Map<String, Any?>? = null,
    val fields: Set<String>? = null,
    val inverted: Boolean = false
) {
    init {
        when (action) {
            is String -> require(action.isNotBlank()) { "action must not be blank" }
            is List<*> -> {
                require(action.isNotEmpty()) { "action list must not be empty" }
                require(action.all { it is String && it.isNotBlank() }) {
                    "all actions in list must be non-blank strings"
                }
            }
            else -> throw IllegalArgumentException("action must be a String or List<String>")
        }
        require(subjectType.isNotBlank()) { "subjectType must not be blank" }
    }

    /**
     * Get actions as a list (normalizes single action to list).
     */
    internal fun getActions(): List<String> {
        return when (action) {
            is String -> listOf(action)
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                action as List<String>
            }
            else -> emptyList()
        }
    }

    /**
     * Check if this rule matches the given action.
     * Supports wildcard "manage" action that matches any action.
     * When action is a list, returns true if actionToCheck matches any action in the list.
     *
     * @param actionToCheck The action to check
     * @return true if rule applies to this action
     */
    fun matchesAction(actionToCheck: String): Boolean {
        return when (action) {
            is String -> {
                // "manage" is a special wildcard that matches all actions
                if (action == "manage") return true
                action == actionToCheck
            }
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                val actions = action as List<String>
                // Check for wildcard "manage" or if actionToCheck is in the list
                actions.contains("manage") || actions.contains(actionToCheck)
            }
            else -> false
        }
    }

    /**
     * Check if this rule matches the given subject type.
     * Supports wildcard "all" subject that matches any subject.
     *
     * @param subjectTypeToCheck The subject type to check
     * @return true if rule applies to this subject type
     */
    fun matchesSubjectType(subjectTypeToCheck: String): Boolean {
        // "all" is a special wildcard that matches all subjects
        if (subjectType == "all") return true
        return subjectType == subjectTypeToCheck
    }

    /**
     * Check if this rule matches the given subject instance.
     *
     * @param subject The subject to check conditions against
     * @return true if conditions match or no conditions specified
     */
    fun matches(subject: Any?): Boolean {
        return ConditionMatcher.matches(conditions, subject)
    }

    /**
     * Check if this rule applies to the specified field.
     *
     * @param field The field name to check (null means resource-level check)
     * @return true if rule applies to this field
     *
     * Note: When field is null (resource-level check) and this rule has field restrictions,
     * inverted rules return false to allow checking to continue to regular rules.
     */
    fun matchesField(field: String?): Boolean {
        if (fields == null) return true // No field restriction = applies to all fields

        if (field == null) {
            // Resource-level check: ignore inverted rules with field restrictions
            // They disallow specific fields, so we continue looking for regular rules
            return !inverted
        }

        return field in fields
    }

    /**
     * Convert to serializable RawRule format.
     *
     * @return RawRule instance
     */
    fun toRawRule(): RawRule {
        return RawRule(
            action = action,
            subject = subjectType,
            conditions = conditions,
            fields = fields?.toList(),
            inverted = inverted
        )
    }
}
