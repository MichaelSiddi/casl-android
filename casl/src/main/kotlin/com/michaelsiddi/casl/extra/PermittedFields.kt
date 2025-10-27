package com.michaelsiddi.casl.extra

import com.michaelsiddi.casl.Ability
import com.michaelsiddi.casl.RawRule

/**
 * Function type that extracts fields from a rule.
 * Returns all fields that the rule applies to.
 */
public typealias FieldsExtractor = (RawRule) -> List<String>

/**
 * Options for permittedFieldsOf function.
 *
 * @property fieldsFrom Function that extracts fields from a rule.
 *                      Should return all fields if rule has no field restrictions.
 */
public data class PermittedFieldsOptions(
    val fieldsFrom: FieldsExtractor
)

/**
 * Get all fields that are permitted for a specific action on a subject.
 *
 * This utility applies last-match-wins precedence to determine which fields
 * are accessible, properly handling inverted rules that remove permissions.
 *
 * Example:
 * ```kotlin
 * val ability = Ability.builder()
 *     .can("read", "Post", listOf("title", "content", "authorId"))
 *     .cannot("read", "Post", listOf("authorId"))  // Remove authorId
 *     .build()
 *
 * val options = PermittedFieldsOptions { rule ->
 *     rule.fields ?: listOf("title", "content", "authorId")  // All fields
 * }
 *
 * val fields = permittedFieldsOf(ability, "read", "Post", options)
 * // Returns: ["title", "content"] (authorId was removed by cannot rule)
 * ```
 *
 * @param ability The ability instance
 * @param action The action to check
 * @param subject The subject (can be type string or instance)
 * @param options Options including fieldsFrom extractor
 * @return List of permitted field names
 */
public fun permittedFieldsOf(
    ability: Ability,
    action: String,
    subject: Any,
    options: PermittedFieldsOptions
): List<String> {
    val subjectType = when (subject) {
        is String -> subject
        else -> com.michaelsiddi.casl.SubjectTypeDetector.detectType(subject)
    }

    val rules = ability.rulesFor(action, subjectType)
    val uniqueFields = mutableSetOf<String>()

    // Process rules in reverse order (last-match-wins)
    // But build the set forward, using add/remove based on inverted flag
    var i = rules.size
    while (i-- > 0) {
        val rule = rules[i]

        // Check if rule matches the subject instance (if it has conditions)
        val matchesSubject = if (rule.conditions != null && subject !is String) {
            ability.can(action, subject)  // Use ability to check conditions
        } else {
            true
        }

        if (matchesSubject) {
            val fields = options.fieldsFrom(rule)

            if (rule.inverted) {
                // Inverted rule removes fields
                uniqueFields.removeAll(fields.toSet())
            } else {
                // Regular rule adds fields
                uniqueFields.addAll(fields)
            }
        }
    }

    return uniqueFields.toList()
}

/**
 * Function type that returns all possible fields for a subject type.
 * Used as fallback when a rule has no field restrictions.
 */
public typealias AllFieldsExtractor = (String) -> List<String>

/**
 * Helper class to make custom accessibleFieldsBy helper function.
 *
 * Example:
 * ```kotlin
 * val getAllFields: AllFieldsExtractor = { subjectType ->
 *     when (subjectType) {
 *         "Post" -> listOf("title", "content", "authorId", "createdAt")
 *         "Comment" -> listOf("text", "authorId", "postId")
 *         else -> emptyList()
 *     }
 * }
 *
 * val accessibleFields = AccessibleFields(ability, "read", getAllFields)
 *
 * // Get fields for a type
 * val postFields = accessibleFields.ofType("Post")
 *
 * // Get fields for an instance
 * val myPostFields = accessibleFields.of(myPost)
 * ```
 */
public class AccessibleFields(
    private val ability: Ability,
    private val action: String,
    private val getAllFields: AllFieldsExtractor
) {
    /**
     * Returns accessible fields for a subject type.
     *
     * @param subjectType The subject type (e.g., "Post")
     * @return List of accessible field names
     */
    public fun ofType(subjectType: String): List<String> {
        return permittedFieldsOf(ability, action, subjectType, PermittedFieldsOptions {
            getRuleFields(subjectType, it)
        })
    }

    /**
     * Returns accessible fields for a particular subject instance.
     *
     * @param subject The subject instance
     * @return List of accessible field names
     */
    public fun of(subject: Any): List<String> {
        val subjectType = com.michaelsiddi.casl.SubjectTypeDetector.detectType(subject)
        return permittedFieldsOf(ability, action, subject, PermittedFieldsOptions {
            getRuleFields(subjectType, it)
        })
    }

    private fun getRuleFields(subjectType: String, rule: RawRule): List<String> {
        return rule.fields ?: getAllFields(subjectType)
    }
}
