/**
 * CASL Android - Kotlin DSL Extensions
 *
 * Idiomatic Kotlin extensions for enhanced developer experience.
 * These are optional enhancements - core API works without them.
 *
 * Feature: 001-casl-android-port
 * Date: 2025-10-23
 */

package com.casl.extensions

import com.casl.Ability
import com.casl.AbilityBuilder
import com.casl.RawRule

/**
 * DSL function for building abilities with lambda syntax.
 *
 * Example:
 * ```kotlin
 * val ability = buildAbility {
 *     can("read", "BlogPost")
 *     can("update", "BlogPost") {
 *         conditions = mapOf("authorId" to currentUserId)
 *     }
 *     cannot("delete", "BlogPost") {
 *         conditions = mapOf("published" to true)
 *     }
 * }
 * ```
 */
inline fun buildAbility(block: AbilityBuilder.() -> Unit): Ability {
    return AbilityBuilder().apply(block).build()
}

/**
 * Infix operator for more readable permission checks.
 *
 * Example:
 * ```kotlin
 * if (user canPerform "read" on blogPost) {
 *     // Allow access
 * }
 * ```
 */
infix fun Any.canPerform(action: String): ActionContext {
    return ActionContext(action, this)
}

data class ActionContext(val action: String, val actor: Any) {
    infix fun on(subject: Any?): Boolean {
        // This would need access to Ability instance - typically stored in actor
        // Implementation detail for Phase 2
        throw NotImplementedError("Infix operators require design pattern for ability access")
    }
}

/**
 * Extension property for checking if subject type is a String (type literal).
 */
val Any?.isTypeLiteral: Boolean
    get() = this is String

/**
 * Operator overload for rule DSL (advanced usage).
 *
 * Example:
 * ```kotlin
 * val rule = "read" on "BlogPost"
 * ```
 */
infix fun String.on(subject: String): RuleDefinition {
    return RuleDefinition(action = this, subject = subject)
}

data class RuleDefinition(
    val action: String,
    val subject: String,
    val conditions: Map<String, Any?>? = null,
    val fields: List<String>? = null
) {
    infix fun where(conditionsPair: Pair<String, Any?>): RuleDefinition {
        return copy(conditions = mapOf(conditionsPair))
    }

    infix fun forFields(fieldList: List<String>): RuleDefinition {
        return copy(fields = fieldList)
    }

    fun toRawRule(inverted: Boolean = false): RawRule {
        return RawRule(
            action = action,
            subject = subject,
            conditions = conditions,
            fields = fields,
            inverted = inverted
        )
    }
}

/**
 * Extension for AbilityBuilder to accept RuleDefinition.
 *
 * Example:
 * ```kotlin
 * val ability = buildAbility {
 *     +"read" on "BlogPost"
 *     +"update" on "BlogPost" where ("authorId" to userId)
 * }
 * ```
 */
operator fun AbilityBuilder.plus(rule: RuleDefinition): AbilityBuilder {
    return can(rule.action, rule.subject, rule.conditions, rule.fields)
}

operator fun AbilityBuilder.minus(rule: RuleDefinition): AbilityBuilder {
    return cannot(rule.action, rule.subject, rule.conditions, rule.fields)
}

/**
 * Scope function for defining conditions inline.
 *
 * Example:
 * ```kotlin
 * val ability = buildAbility {
 *     can("update", "BlogPost") {
 *         "authorId" to currentUserId
 *     }
 * }
 * ```
 */
class ConditionsBuilder {
    private val conditions = mutableMapOf<String, Any?>()

    infix fun String.to(value: Any?) {
        conditions[this] = value
    }

    fun build(): Map<String, Any?> = conditions.toMap()
}

/**
 * Enhanced can() with conditions builder.
 *
 * Example:
 * ```kotlin
 * buildAbility {
 *     can("update", "BlogPost") {
 *         "authorId" to userId
 *         "status" to "draft"
 *     }
 * }
 * ```
 */
inline fun AbilityBuilder.can(
    action: String,
    subject: String,
    conditionsBuilder: ConditionsBuilder.() -> Unit
): AbilityBuilder {
    val conditions = ConditionsBuilder().apply(conditionsBuilder).build()
    return can(action, subject, conditions, null)
}

/**
 * Enhanced cannot() with conditions builder.
 */
inline fun AbilityBuilder.cannot(
    action: String,
    subject: String,
    conditionsBuilder: ConditionsBuilder.() -> Unit
): AbilityBuilder {
    val conditions = ConditionsBuilder().apply(conditionsBuilder).build()
    return cannot(action, subject, conditions, null)
}

/**
 * Extension for checking permissions with null safety.
 *
 * Example:
 * ```kotlin
 * blogPost?.let { ability.can("read", it) } ?: false
 * // Or using extension:
 * blogPost.canBeAccessedBy(ability, "read")
 * ```
 */
fun Any?.canBeAccessedBy(ability: Ability, action: String, field: String? = null): Boolean {
    return this?.let { ability.can(action, it, field) } ?: false
}

/**
 * Extension for list of rules manipulation.
 */
fun List<RawRule>.filterByAction(action: String): List<RawRule> {
    return filter { it.action == action }
}

fun List<RawRule>.filterBySubject(subject: String): List<RawRule> {
    return filter { it.subject == subject }
}

fun List<RawRule>.onlyPermissions(): List<RawRule> {
    return filter { !it.inverted }
}

fun List<RawRule>.onlyProhibitions(): List<RawRule> {
    return filter { it.inverted }
}

/**
 * Extension for merging multiple rule sets.
 */
operator fun List<RawRule>.plus(other: List<RawRule>): List<RawRule> {
    return this + other // Natural list concatenation
}

/**
 * Extension for subject type name extraction (utility).
 */
val Any.subjectTypeName: String
    get() = when (this) {
        is String -> this
        else -> this::class.simpleName ?: this.javaClass.simpleName
    }
