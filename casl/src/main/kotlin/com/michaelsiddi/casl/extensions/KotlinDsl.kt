@file:JvmName("CaslDsl")

package com.michaelsiddi.casl.extensions

import com.michaelsiddi.casl.Ability
import com.michaelsiddi.casl.AbilityBuilder
import com.michaelsiddi.casl.RawRule

/**
 * DSL function for building abilities with lambda syntax.
 *
 * Example:
 * ```kotlin
 * val ability = buildAbility {
 *     can("read", "BlogPost")
 *     can("update", "BlogPost", conditions = mapOf("authorId" to userId))
 *     cannot("delete", "BlogPost", conditions = mapOf("published" to true))
 * }
 * ```
 *
 * @param block Lambda with receiver for configuring the ability
 * @return Built Ability instance
 */
public inline fun buildAbility(block: AbilityBuilder.() -> Unit): Ability {
    return AbilityBuilder().apply(block).build()
}

/**
 * Extension property for extracting subject type name.
 *
 * Example:
 * ```kotlin
 * val post = BlogPost(...)
 * val typeName = post.subjectTypeName // "BlogPost"
 * ```
 */
public val Any.subjectTypeName: String
    get() = when (this) {
        is String -> this
        else -> this::class.simpleName ?: this.javaClass.simpleName
    }

/**
 * Filter rules by action.
 *
 * Example:
 * ```kotlin
 * val readRules = allRules.filterByAction("read")
 * ```
 */
public fun List<RawRule>.filterByAction(action: String): List<RawRule> {
    return filter { it.action == action }
}

/**
 * Filter rules by subject type.
 *
 * Example:
 * ```kotlin
 * val postRules = allRules.filterBySubject("BlogPost")
 * ```
 */
public fun List<RawRule>.filterBySubject(subject: String): List<RawRule> {
    return filter { it.subject == subject }
}

/**
 * Filter to only permission rules (non-inverted).
 *
 * Example:
 * ```kotlin
 * val permissions = allRules.onlyPermissions()
 * ```
 */
public fun List<RawRule>.onlyPermissions(): List<RawRule> {
    return filter { !it.inverted }
}

/**
 * Filter to only prohibition rules (inverted).
 *
 * Example:
 * ```kotlin
 * val prohibitions = allRules.onlyProhibitions()
 * ```
 */
public fun List<RawRule>.onlyProhibitions(): List<RawRule> {
    return filter { it.inverted }
}

/**
 * Conditions builder for inline DSL syntax.
 *
 * Internal helper class for enhanced can/cannot methods.
 */
public class ConditionsBuilder {
    private val conditions = mutableMapOf<String, Any?>()

    /**
     * Add a condition.
     *
     * Example:
     * ```kotlin
     * can("update", "BlogPost") {
     *     "authorId" to userId
     *     "status" to "draft"
     * }
     * ```
     */
    public infix fun String.to(value: Any?) {
        conditions[this] = value
    }

    @PublishedApi
    internal fun build(): Map<String, Any?> = conditions.toMap()
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
public inline fun AbilityBuilder.can(
    action: String,
    subject: String,
    conditionsBuilder: ConditionsBuilder.() -> Unit
): AbilityBuilder {
    val conditions = ConditionsBuilder().apply(conditionsBuilder).build()
    return can(action, subject, conditions, null)
}

/**
 * Enhanced cannot() with conditions builder.
 *
 * Example:
 * ```kotlin
 * buildAbility {
 *     cannot("delete", "BlogPost") {
 *         "published" to true
 *     }
 * }
 * ```
 */
public inline fun AbilityBuilder.cannot(
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
 * if (blogPost.canBeAccessedBy(ability, "read")) {
 *     // Show post
 * }
 * ```
 */
public fun Any?.canBeAccessedBy(ability: Ability, action: String, field: String? = null): Boolean {
    return this?.let { ability.can(action, it, field) } ?: false
}
