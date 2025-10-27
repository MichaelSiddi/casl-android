package com.michaelsiddi.casl

/**
 * Type alias for action alias map (alias -> target actions).
 * Example: mapOf("modify" to listOf("update", "delete"))
 */
public typealias AliasesMap = Map<String, List<String>>

/**
 * Function type that resolves action aliases to expanded action lists.
 */
public typealias AliasResolver = (Any) -> List<String>

/**
 * Options for creating an alias resolver.
 *
 * @property skipValidate Skip validation for cycles and reserved actions (default: false)
 * @property anyAction The reserved action name that cannot be used as an alias (default: "manage")
 */
public data class AliasResolverOptions(
    val skipValidate: Boolean = false,
    val anyAction: String = "manage"
)

/**
 * Creates an alias resolver function that expands action aliases.
 *
 * Example:
 * ```kotlin
 * val resolver = createAliasResolver(mapOf(
 *     "modify" to listOf("update", "delete"),
 *     "access" to listOf("read", "modify")  // nested: access -> read, update, delete
 * ))
 *
 * resolver("modify") // returns listOf("modify", "update", "delete")
 * resolver("access") // returns listOf("access", "read", "modify", "update", "delete")
 * ```
 *
 * @param aliasMap Map of action aliases to their target actions
 * @param options Options for validation and reserved action name
 * @return Function that expands actions to include aliases
 * @throws IllegalArgumentException if cycles detected or reserved actions used
 */
public fun createAliasResolver(
    aliasMap: AliasesMap,
    options: AliasResolverOptions = AliasResolverOptions()
): AliasResolver {
    if (!options.skipValidate) {
        validateForCycles(aliasMap, options.anyAction)
    }

    return { action: Any ->
        when (action) {
            is String -> expandActions(aliasMap, listOf(action))
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                expandActions(aliasMap, action as List<String>)
            }
            else -> listOf()
        }
    }
}

/**
 * Expands actions by resolving aliases.
 * If an action has an alias, it adds the alias target actions to the result.
 *
 * @param aliasMap Map of aliases
 * @param rawActions Initial list of actions
 * @return Expanded list with all resolved aliases
 */
private fun expandActions(aliasMap: AliasesMap, rawActions: List<String>): List<String> {
    val actions = rawActions.toMutableList()
    var i = 0

    while (i < actions.size) {
        val action = actions[i++]

        if (aliasMap.containsKey(action)) {
            val aliasTargets = aliasMap[action]!!
            // Add alias targets to the list (will be processed in subsequent iterations)
            actions.addAll(aliasTargets)
        }
    }

    return actions
}

/**
 * Validates alias map for cycles and reserved action usage.
 *
 * @param aliasMap Map of aliases to validate
 * @param reservedAction The reserved action (e.g., "manage")
 * @throws IllegalArgumentException if validation fails
 */
private fun validateForCycles(aliasMap: AliasesMap, reservedAction: String) {
    // Check if reserved action is used as an alias
    if (aliasMap.containsKey(reservedAction)) {
        throw IllegalArgumentException(
            "Cannot use \"$reservedAction\" as an alias because it's reserved action."
        )
    }

    // Check each alias for cycles and reserved action usage
    val keys = aliasMap.keys.toList()
    for (key in keys) {
        try {
            expandActionsAndDetectCycles(aliasMap, listOf(key), reservedAction)
        } catch (e: IllegalArgumentException) {
            throw e
        }
    }
}

/**
 * Expands actions while detecting cycles and reserved action usage.
 *
 * @param aliasMap Map of aliases
 * @param rawActions Actions to expand
 * @param reservedAction Reserved action name
 * @return Expanded actions
 * @throws IllegalArgumentException if cycle or reserved action detected
 */
private fun expandActionsAndDetectCycles(
    aliasMap: AliasesMap,
    rawActions: List<String>,
    reservedAction: String
): List<String> {
    val actions = rawActions.toMutableList()
    var i = 0

    while (i < actions.size) {
        val action = actions[i++]

        if (aliasMap.containsKey(action)) {
            val aliasTargets = aliasMap[action]!!

            // Check for cycles: if any alias target is already in the expansion path
            val duplicate = findDuplicate(actions, aliasTargets)
            if (duplicate != null) {
                throw IllegalArgumentException(
                    "Detected cycle $duplicate -> ${actions.joinToString(", ")}"
                )
            }

            // Check if alias targets use reserved action
            if (aliasTargets.contains(reservedAction)) {
                throw IllegalArgumentException(
                    "Cannot make an alias to \"$reservedAction\" because this is reserved action"
                )
            }

            if (action == reservedAction) {
                throw IllegalArgumentException(
                    "Cannot make an alias to \"$reservedAction\" because this is reserved action"
                )
            }

            actions.addAll(aliasTargets)
        }
    }

    return actions
}

/**
 * Finds a duplicate action in the list.
 *
 * @param actions List of actions to search
 * @param actionToFind Action or list of actions to find
 * @return First duplicate found, or null if no duplicate
 */
private fun findDuplicate(actions: List<String>, actionToFind: List<String>): String? {
    for (action in actionToFind) {
        if (actions.contains(action)) {
            return action
        }
    }
    return null
}

/**
 * Identity resolver that doesn't expand aliases.
 * Returns the input action(s) as-is wrapped in a list.
 */
public val identityResolver: AliasResolver = { action: Any ->
    when (action) {
        is String -> listOf(action)
        is List<*> -> {
            @Suppress("UNCHECKED_CAST")
            action as List<String>
        }
        else -> listOf()
    }
}
