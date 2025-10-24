package com.michaelsiddi.casl

/**
 * Internal representation of an authorization rule.
 *
 * This class is immutable once created and optimized for permission checking.
 * Use [RawRule] for JSON serialization.
 *
 * @property action The action being authorized (e.g., "read", "update", "delete")
 * @property subjectType Type of resource (e.g., "BlogPost", "Comment")
 * @property conditions Optional attribute matchers for conditional rules
 * @property fields Optional field restrictions (null means all fields)
 * @property inverted True for "cannot" rules, false for "can" rules
 */
internal data class Rule(
    val action: String,
    val subjectType: String,
    val conditions: Map<String, Any?>? = null,
    val fields: Set<String>? = null,
    val inverted: Boolean = false
) {
    init {
        require(action.isNotBlank()) { "action must not be blank" }
        require(subjectType.isNotBlank()) { "subjectType must not be blank" }
    }

    /**
     * Check if this rule matches the given action.
     * Supports wildcard "manage" action that matches any action.
     *
     * @param actionToCheck The action to check
     * @return true if rule applies to this action
     */
    fun matchesAction(actionToCheck: String): Boolean {
        // "manage" is a special wildcard that matches all actions
        if (action == "manage") return true
        return action == actionToCheck
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
     */
    fun matchesField(field: String?): Boolean {
        if (fields == null) return true // No field restriction = applies to all fields
        if (field == null) return true // Resource-level check = applies
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
