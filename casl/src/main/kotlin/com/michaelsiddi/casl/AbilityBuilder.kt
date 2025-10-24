package com.michaelsiddi.casl

import kotlin.jvm.JvmOverloads

/**
 * Fluent builder for creating [Ability] instances.
 *
 * Not thread-safe: Each builder should be used from a single thread.
 *
 * Example usage (Kotlin):
 * ```kotlin
 * val ability = AbilityBuilder()
 *     .can("read", "BlogPost")
 *     .can("update", "BlogPost", conditions = mapOf("authorId" to userId))
 *     .cannot("delete", "BlogPost", conditions = mapOf("published" to true))
 *     .build()
 * ```
 *
 * Example usage (Java):
 * ```java
 * Ability ability = new AbilityBuilder()
 *     .can("read", "BlogPost")
 *     .can("update", "BlogPost", Map.of("authorId", userId), null)
 *     .cannot("delete", "BlogPost", Map.of("published", true), null)
 *     .build();
 * ```
 */
public class AbilityBuilder {
    private val rules = mutableListOf<RawRule>()

    /**
     * Add a positive permission rule.
     *
     * @param action The action to permit (e.g., "read", "update", "delete")
     * @param subject The subject type (e.g., "BlogPost", "Comment")
     * @param conditions Optional attribute matchers (e.g., mapOf("authorId" to currentUserId))
     * @param fields Optional field restrictions (e.g., listOf("title", "content"))
     * @return this builder for chaining
     *
     * @throws IllegalArgumentException if action or subject is blank
     */
    @JvmOverloads
    public fun can(
        action: String,
        subject: String,
        conditions: Map<String, Any?>? = null,
        fields: List<String>? = null
    ): AbilityBuilder {
        require(action.isNotBlank()) { "action must not be blank" }
        require(subject.isNotBlank()) { "subject must not be blank" }

        rules.add(
            RawRule(
                action = action,
                subject = subject,
                conditions = conditions,
                fields = fields,
                inverted = false
            )
        )
        return this
    }

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
    public fun cannot(
        action: String,
        subject: String,
        conditions: Map<String, Any?>? = null,
        fields: List<String>? = null
    ): AbilityBuilder {
        require(action.isNotBlank()) { "action must not be blank" }
        require(subject.isNotBlank()) { "subject must not be blank" }

        rules.add(
            RawRule(
                action = action,
                subject = subject,
                conditions = conditions,
                fields = fields,
                inverted = true
            )
        )
        return this
    }

    /**
     * Remove all accumulated rules from this builder.
     *
     * @return this builder for chaining
     */
    public fun clear(): AbilityBuilder {
        rules.clear()
        return this
    }

    /**
     * Build the Ability instance with accumulated rules.
     *
     * This builder can continue to be used after build() to create additional
     * Ability instances.
     *
     * @return A new Ability instance
     * @throws IllegalStateException if builder state is invalid
     */
    public fun build(): Ability {
        val internalRules = rules.map { it.toRule() }
        return Ability(internalRules)
    }
}
