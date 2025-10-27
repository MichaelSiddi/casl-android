package com.michaelsiddi.casl

/**
 * Function type for custom subject type detection.
 * Receives a subject and returns its type as a string.
 */
public typealias DetectSubjectType = (Any?) -> String

/**
 * Options for configuring an Ability instance.
 *
 * @property resolveAction Function that expands action aliases (default: identity, no expansion)
 * @property anyAction Reserved action name that matches all actions (default: "manage")
 * @property anySubjectType Reserved subject type that matches all subjects (default: "all")
 * @property detectSubjectType Custom function to detect subject types (default: uses SubjectTypeDetector.detectType)
 */
public data class AbilityOptions(
    val resolveAction: AliasResolver = identityResolver,
    val anyAction: String = "manage",
    val anySubjectType: String = "all",
    val detectSubjectType: DetectSubjectType? = null
)
