package com.michaelsiddi.casl.extra

import com.michaelsiddi.casl.Ability
import com.michaelsiddi.casl.RawRule

/**
 * Function type that converts a rule to a query object.
 * Used to transform rule conditions into database-specific query format.
 */
public typealias RuleToQueryConverter<T> = (RawRule) -> T

/**
 * Query structure with optional $and/$or operators.
 * Compatible with MongoDB query format.
 */
public data class AbilityQuery<T>(
    val `$or`: List<T>? = null,
    val `$and`: List<T>? = null
)

/**
 * Convert ability rules to a database query.
 *
 * This utility analyzes rules and generates a query that can be used to
 * fetch only the records a user has permission to access. Supports
 * both regular and inverted rules with proper precedence handling.
 *
 * Example:
 * ```kotlin
 * val ability = Ability.builder()
 *     .can("read", "Post", mapOf("published" to true))
 *     .can("read", "Post", mapOf("authorId" to 123))
 *     .cannot("read", "Post", mapOf("draft" to true))
 *     .build()
 *
 * val query = rulesToQuery(ability, "read", "Post") { rule ->
 *     rule.conditions ?: emptyMap()
 * }
 *
 * // Result:
 * // {
 * //   "$or": [
 * //     {"published": true},
 * //     {"authorId": 123}
 * //   ],
 * //   "$and": [
 * //     {"draft": {"$ne": true}}
 * //   ]
 * // }
 * ```
 *
 * @param ability The ability instance
 * @param action The action to check
 * @param subjectType The subject type
 * @param convert Function to convert rule to query format
 * @return AbilityQuery with $or/$and conditions, or null if no access
 */
public fun <T> rulesToQuery(
    ability: Ability,
    action: String,
    subjectType: String,
    convert: RuleToQueryConverter<T>
): AbilityQuery<T>? {
    val and = mutableListOf<T>()
    val or = mutableListOf<T>()
    val rules = ability.rulesFor(action, subjectType)

    var foundUnconditionalAllow = false

    for (rule in rules) {
        val list = if (rule.inverted) and else or

        if (rule.conditions == null) {
            if (rule.inverted) {
                // Inverted rule without conditions blocks everything
                // Example:
                // can('read', 'Post', { id: 2 })
                // cannot('read', 'Post')  // Blocks all further rules
                // can('read', 'Post', { id: 5 })
                break
            } else {
                // Regular rule without conditions allows everything
                // Clear previous $or conditions but continue processing $and (cannot rules)
                // Example:
                // can('read', 'Post', { id: 1 })
                // can('read', 'Post')  // Allows all
                // cannot('read', 'Post', { status: 'draft' })
                or.clear()
                foundUnconditionalAllow = true
            }
        } else {
            // Only add to $or if we haven't found unconditional allow yet
            if (!foundUnconditionalAllow || rule.inverted) {
                list.add(convert(rule))
            }
        }
    }

    // If there are no positive conditions and no unconditional allow, user has no access
    if (or.isEmpty() && !foundUnconditionalAllow) return null

    // Return combined query
    return when {
        or.isEmpty() && and.isEmpty() -> AbilityQuery() // Unconditional allow with no restrictions
        or.isEmpty() -> AbilityQuery(`$and` = and) // Unconditional allow with restrictions
        and.isEmpty() -> AbilityQuery(`$or` = or) // Conditional allow with no restrictions
        else -> AbilityQuery(`$or` = or, `$and` = and) // Conditional allow with restrictions
    }
}

/**
 * MongoDB-compatible query builder helper.
 *
 * Converts rule conditions to MongoDB query format, properly handling
 * inverted rules by wrapping them in $ne operators.
 *
 * Example:
 * ```kotlin
 * val query = rulesToMongoQuery(ability, "read", "Post")
 *
 * // Use with MongoDB driver:
 * collection.find(query)
 * ```
 */
public fun rulesToMongoQuery(
    ability: Ability,
    action: String,
    subjectType: String
): Map<String, Any>? {
    val query = rulesToQuery(ability, action, subjectType) { rule ->
        if (rule.inverted && rule.conditions != null) {
            // Invert conditions for cannot rules
            invertConditions(rule.conditions)
        } else {
            rule.conditions ?: emptyMap()
        }
    }

    return when {
        query == null -> null
        query.`$or` == null && query.`$and` == null -> emptyMap()
        query.`$or` == null -> mapOf("\$and" to query.`$and`!!)
        query.`$and` == null -> mapOf("\$or" to query.`$or`!!)
        else -> mapOf("\$or" to query.`$or`!!, "\$and" to query.`$and`!!)
    }
}

/**
 * Invert conditions for cannot rules.
 * Wraps values in $ne operators to express negation.
 */
private fun invertConditions(conditions: Map<String, Any?>): Map<String, Any?> {
    return conditions.mapValues { (_, value) ->
        when {
            value is Map<*, *> && value.keys.any { it.toString().startsWith("$") } -> {
                // Already has operator, need complex inversion
                // For simplicity, wrap in $not
                mapOf("\$not" to value)
            }
            else -> {
                // Simple value, wrap in $ne
                mapOf("\$ne" to value)
            }
        }
    }
}
