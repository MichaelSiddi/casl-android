package com.michaelsiddi.casl

/**
 * Efficient storage and retrieval of rules optimized for permission checks.
 *
 * Rules are indexed by "action:subjectType" key for O(1) lookup.
 * Field-restricted rules have separate indexing.
 *
 * Thread-safe when used with immutable snapshots (volatile reads).
 */
internal class RuleIndex private constructor(
    private val resourceRules: Map<String, List<Rule>>,
    private val fieldRules: Map<String, Map<String, List<Rule>>>,
    private val allRules: List<Rule>
) {

    /**
     * Find the last matching rule for the given action, subject, and optional field.
     *
     * Implements last-match-wins precedence.
     * Supports wildcards: "manage" action matches all actions, "all" subject matches all subjects.
     *
     * @param action The action to check
     * @param subject The subject instance or type string
     * @param field Optional field name
     * @return Matching rule or null if none found
     */
    fun findMatchingRule(action: String, subject: Any?, field: String?): Rule? {
        if (subject == null) return null

        val subjectType = SubjectTypeDetector.detectType(subject)

        // Find all matching rules (including wildcards)
        val matchingRules = allRules.filter { rule ->
            rule.matchesAction(action) &&
            rule.matchesSubjectType(subjectType) &&
            rule.matchesField(field) &&
            rule.matches(subject)
        }

        // Return last matching rule (last-match-wins)
        return matchingRules.lastOrNull()
    }

    /**
     * Get all rules in this index (for export).
     */
    fun getAllRules(): List<Rule> = allRules

    companion object {
        /**
         * Build RuleIndex from list of rules.
         *
         * Expands rules with array actions into multiple index entries
         * (one per action), similar to casl-js implementation.
         *
         * @param rules List of internal Rule objects
         * @return New RuleIndex instance
         */
        fun fromRules(rules: List<Rule>): RuleIndex {
            val resourceRulesMap = mutableMapOf<String, MutableList<Rule>>()
            val fieldRulesMap = mutableMapOf<String, MutableMap<String, MutableList<Rule>>>()

            rules.forEach { rule ->
                // Get actions as list (handles both String and List<String>)
                val actions = rule.getActions()

                // Create index entries for each action
                actions.forEach { action ->
                    val key = "${action}:${rule.subjectType}"

                    if (rule.fields == null) {
                        // Resource-level rule
                        resourceRulesMap.getOrPut(key) { mutableListOf() }.add(rule)
                    } else {
                        // Field-level rule
                        rule.fields.forEach { field ->
                            val fieldMap = fieldRulesMap.getOrPut(field) { mutableMapOf() }
                            fieldMap.getOrPut(key) { mutableListOf() }.add(rule)
                        }
                    }
                }
            }

            return RuleIndex(
                resourceRules = resourceRulesMap,
                fieldRules = fieldRulesMap,
                allRules = rules.toList()
            )
        }

        /**
         * Create empty RuleIndex.
         */
        fun empty(): RuleIndex = RuleIndex(emptyMap(), emptyMap(), emptyList())
    }
}
