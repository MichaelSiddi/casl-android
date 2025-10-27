package com.michaelsiddi.casl

/**
 * Field pattern matcher that supports wildcard patterns.
 *
 * Supports the following patterns:
 * - `author.*` - Matches any 1st level field under author (e.g., author, author.name, author.age)
 * - `author.**` - Matches any field at any depth under author (e.g., author, author.name, author.publication.name)
 * - `*.name` - Matches name field in any 1st level object (e.g., author.name, NOT author.publication.name)
 * - `**.name` - Matches name field at any depth (e.g., author.name, author.publication.name)
 * - `street*` - Matches fields starting with "street" (e.g., street, street1, street2)
 *
 * Implementation based on casl-js packages/casl-ability/src/matchers/field.ts
 */
internal object FieldMatcher {

    /**
     * Creates a field matcher function for the given field patterns.
     */
    fun createMatcher(fields: List<String>): (String) -> Boolean {
        // Check if any field contains wildcards
        val hasWildcards = fields.any { '*' in it }

        if (!hasWildcards) {
            // Fast path: exact matching only
            val fieldSet = fields.toSet()
            return { field -> field in fieldSet }
        } else {
            // Create regex pattern for wildcard matching
            val pattern = createPattern(fields)
            return { field -> pattern.matches(field) }
        }
    }

    /**
     * Creates a regex pattern from field patterns.
     */
    private fun createPattern(fields: List<String>): Regex {
        val patterns = fields.map { field ->
            convertToRegexPattern(field)
        }

        val combinedPattern = if (patterns.size > 1) {
            "(?:${patterns.joinToString("|")})"
        } else {
            patterns[0]
        }

        return Regex("^$combinedPattern$")
    }

    /**
     * Converts a field pattern to a regex pattern.
     *
     * Examples based on casl-js:
     * - author.* -> author(?:\.[^.]+)?
     * - author.** -> author(?:\.(.+))?
     * - *.name -> [^.]+\.name
     * - **.name -> .+\.name
     * - street* -> street.*
     * - author.*.name -> author\.[^.]+\.name
     */
    private fun convertToRegexPattern(field: String): String {
        val result = StringBuilder()
        var i = 0

        while (i < field.length) {
            when {
                // Handle ** (match any depth)
                i + 1 < field.length && field[i] == '*' && field[i + 1] == '*' -> {
                    val atEnd = i + 2 >= field.length
                    val hasDotBefore = i > 0 && field[i - 1] == '.'

                    if (atEnd && hasDotBefore) {
                        // author.** at end - make optional including the dot
                        // Remove the \. we just added and make it optional with what follows
                        result.setLength(result.length - 2) // Remove "\."
                        result.append("(?:\\.(.+))?")
                    } else if (atEnd) {
                        // ** at very end with no dot before
                        result.append("(.+)?")
                    } else {
                        // ** in middle
                        result.append(".+")
                    }

                    i += 2
                    // Skip the dot after ** if present (we'll add it ourselves)
                    if (i < field.length && field[i] == '.') {
                        result.append("\\.")
                        i++
                    }
                }
                // Handle * (match single level or prefix)
                field[i] == '*' -> {
                    val hasDotBefore = i > 0 && field[i - 1] == '.'
                    val hasDotAfter = i + 1 < field.length && field[i + 1] == '.'
                    val atEnd = i + 1 >= field.length

                    when {
                        hasDotBefore && hasDotAfter -> {
                            // .*.  (single level in middle)
                            result.append("[^.]+")
                        }
                        hasDotAfter -> {
                            // *. at start (single level)
                            result.append("[^.]+")
                        }
                        hasDotBefore && atEnd -> {
                            // .* at end - make optional including the dot
                            result.setLength(result.length - 2) // Remove "\."
                            result.append("(?:\\.[^.]+)?")
                        }
                        atEnd -> {
                            // * at end (prefix match)
                            result.append(".*")
                        }
                        else -> {
                            // * in middle without dots
                            result.append(".*")
                        }
                    }

                    i++
                    // Skip the dot after * if we're handling .*.
                    if (hasDotAfter && i < field.length && field[i] == '.') {
                        result.append("\\.")
                        i++
                    }
                }
                // Handle . (escape it)
                field[i] == '.' -> {
                    result.append("\\.")
                    i++
                }
                // Handle special regex characters
                field[i] in "-/\\^$+?|()[]{}".toCharArray() -> {
                    result.append("\\").append(field[i])
                    i++
                }
                // Regular character
                else -> {
                    result.append(field[i])
                    i++
                }
            }
        }

        return result.toString()
    }
}
