package com.michaelsiddi.casl

/**
 * Options for configuring an Ability instance.
 *
 * @property resolveAction Function that expands action aliases (default: identity, no expansion)
 * @property anyAction Reserved action name that matches all actions (default: "manage")
 * @property anySubjectType Reserved subject type that matches all subjects (default: "all")
 */
public data class AbilityOptions(
    val resolveAction: AliasResolver = identityResolver,
    val anyAction: String = "manage",
    val anySubjectType: String = "all"
)
