package com.michaelsiddi.casl

/**
 * Exception thrown when a permission check fails.
 *
 * Provides detailed context about what action was attempted on which subject,
 * making it easier to handle and display authorization errors to users.
 *
 * Example usage:
 * ```kotlin
 * val ability = Ability.builder().can("read", "Post").build()
 * val error = ForbiddenError(ability)
 *
 * // Throws exception if not allowed
 * error.throwUnlessCan("delete", post)
 *
 * // Or check without throwing
 * val errorOrNull = error.unlessCan("delete", post)
 * if (errorOrNull != null) {
 *     // Handle forbidden action
 *     println(errorOrNull.message)
 * }
 * ```
 *
 * @property ability The ability instance used for permission checks
 * @property action The action that was denied (set after unlessCan call)
 * @property subject The subject the action was attempted on (set after unlessCan call)
 * @property subjectType The detected type of the subject (set after unlessCan call)
 * @property field The field that was accessed, if any (set after unlessCan call)
 */
public class ForbiddenError(
    public val ability: Ability
) : RuntimeException() {

    public var action: String? = null
        private set

    public var subject: Any? = null
        private set

    public var subjectType: String? = null
        private set

    public var field: String? = null
        private set

    /**
     * Check if action is allowed, and return this error if not.
     * Does not throw - use throwUnlessCan() to throw immediately.
     *
     * @param action The action to check
     * @param subject The subject to check against
     * @param field Optional field name
     * @return This error if not allowed, null if allowed
     */
    public fun unlessCan(action: String, subject: Any?, field: String? = null): ForbiddenError? {
        // Check if action is allowed
        if (ability.can(action, subject, field)) {
            return null
        }

        // Permission denied - populate error details
        this.action = action
        this.subject = subject
        this.subjectType = SubjectTypeDetector.detectType(subject)
        this.field = field

        return this
    }

    /**
     * Check if action is allowed, and throw this error if not.
     *
     * @param action The action to check
     * @param subject The subject to check against
     * @param field Optional field name
     * @throws ForbiddenError if action is not allowed
     */
    public fun throwUnlessCan(action: String, subject: Any?, field: String? = null) {
        val error = unlessCan(action, subject, field)
        if (error != null) {
            throw error
        }
    }

    private var customMessage: String? = null

    /**
     * Set a custom error message.
     *
     * @param message The custom message
     * @return This error for chaining
     */
    public fun setMessage(message: String): ForbiddenError {
        customMessage = message
        return this
    }

    override val message: String
        get() = customMessage ?: getDefaultErrorMessage()

    private fun getDefaultErrorMessage(): String {
        val actionStr = action ?: "unknown"
        val subjectTypeStr = subjectType ?: "unknown"
        val fieldStr = field?.let { " field \"$it\"" } ?: ""

        return "Cannot execute \"$actionStr\" on \"$subjectTypeStr\"$fieldStr"
    }

    public companion object {
        /**
         * Factory method to create a ForbiddenError from an ability.
         *
         * @param ability The ability instance
         * @return New ForbiddenError instance
         */
        @JvmStatic
        public fun from(ability: Ability): ForbiddenError {
            return ForbiddenError(ability)
        }

        /**
         * Global default error message function.
         * Can be set to customize error messages application-wide.
         */
        @JvmStatic
        public var defaultMessageGenerator: ((ForbiddenError) -> String)? = null

        /**
         * Set a global default error message.
         *
         * @param message Static message string, or null to reset to default
         */
        @JvmStatic
        public fun setDefaultMessage(message: String?) {
            defaultMessageGenerator = message?.let { msg -> { _: ForbiddenError -> msg } }
        }

        /**
         * Set a global default error message generator function.
         *
         * @param generator Function that takes a ForbiddenError and returns a message
         */
        @JvmStatic
        public fun setDefaultMessage(generator: (ForbiddenError) -> String) {
            defaultMessageGenerator = generator
        }
    }

    init {
        // Use global default message generator if set
        if (defaultMessageGenerator != null) {
            customMessage = defaultMessageGenerator!!(this)
        }
    }
}
