package com.michaelsiddi.casl

/**
 * Utility for detecting subject type from instances.
 *
 * Supports three modes:
 * - String subjects are treated as type literals (e.g., "BlogPost")
 * - ForcedSubject instances use their specified type
 * - Object subjects use their class simple name for type detection
 *
 * Thread-safe: This object is stateless and can be called from multiple threads.
 */
public object SubjectTypeDetector {

    /**
     * Detect the type name from a subject.
     *
     * @param subject The subject to detect type from (can be String, ForcedSubject, or any object)
     * @return Type name as string, or "Unknown" if unable to determine
     *
     * Examples:
     * - detectType("BlogPost") returns "BlogPost" (string literal)
     * - detectType(subject("User", ...)) returns "User" (from ForcedSubject)
     * - detectType(BlogPost(...)) returns "BlogPost" (from class name)
     * - detectType(null) returns "Unknown"
     */
    public fun detectType(subject: Any?): String = when (subject) {
        null -> "Unknown"
        is String -> subject // Treat strings as type literals
        is ForcedSubject -> subject.getSubjectType() // Use forced type
        else -> {
            // Try Kotlin reflection first
            subject::class.simpleName
                // Fallback to Java reflection
                ?: subject.javaClass.simpleName
                // Final fallback
                ?: "Unknown"
        }
    }
}
