# Technical Research: CASL Android Authorization Library

**Feature**: 001-casl-android-port
**Date**: 2025-10-23
**Purpose**: Document technical decisions and research findings for implementation

## Research Areas

### 1. Thread-Safety Strategy for Concurrent Permission Checks

**Decision**: Use Kotlin `synchronized` blocks with immutable rule snapshots

**Rationale**:
- iOS CASL uses Swift actors for thread safety, but Android lacks direct actor equivalent
- Kotlin coroutines with `Mutex` would force async-only API, breaking Java compatibility (FR-014)
- Immutable snapshots prevent read locks during permission checks (optimistic locking pattern)
- Write operations (rule updates) use `synchronized` block for atomic replacement
- Achieves <1ms permission check performance goal (SC-002)

**Alternatives Considered**:
- **ReentrantReadWriteLock**: Adds complexity, no significant performance gain for in-memory operations
- **Kotlin Coroutines with Mutex**: Forces async-first API, incompatible with synchronous API requirement (FR-005)
- **Java synchronized methods**: Considered but inline `synchronized` blocks offer better granularity
- **Copy-on-Write collections**: CopyOnWriteArrayList too slow for frequent reads, rule lookups dominate performance

**Implementation Pattern**:
```kotlin
class Ability {
    @Volatile
    private var ruleIndex: RuleIndex = RuleIndex.empty()

    fun can(action: String, subject: Any?, field: String? = null): Boolean {
        val snapshot = ruleIndex // Volatile read gets consistent snapshot
        return snapshot.findMatchingRule(action, subject, field)
    }

    fun update(rawRules: List<RawRule>) {
        synchronized(this) {
            ruleIndex = RuleIndex.fromRawRules(rawRules)
        }
    }
}
```

---

### 2. JSON Serialization Without External Dependencies

**Decision**: Manual JSON serialization using `org.json.JSONObject` and `org.json.JSONArray` from Android SDK

**Rationale**:
- FR-016 prohibits external dependencies (no Gson, Moshi, kotlinx.serialization)
- Android SDK includes `org.json` package by default (API 1+, well before API 21 target)
- Simple data model (RawRule with strings, maps, lists) doesn't require advanced serialization features
- Meets <10ms serialization performance goal for 100 rules (SC-004)

**Alternatives Considered**:
- **kotlinx.serialization**: Adds external dependency, violates FR-016
- **Gson**: Adds external dependency, violates FR-016
- **Moshi**: Adds external dependency, violates FR-016
- **Manual String building**: Error-prone, no parsing support, doesn't handle escaping correctly

**Implementation Pattern**:
```kotlin
data class RawRule(
    val action: String,
    val subject: String,
    val conditions: Map<String, Any?>? = null,
    val fields: List<String>? = null,
    val inverted: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("action", action)
        put("subject", subject)
        conditions?.let { put("conditions", JSONObject(it)) }
        fields?.let { put("fields", JSONArray(it)) }
        put("inverted", inverted)
    }

    companion object {
        fun fromJson(json: JSONObject): RawRule = RawRule(
            action = json.getString("action"),
            subject = json.getString("subject"),
            conditions = json.optJSONObject("conditions")?.toMap(),
            fields = json.optJSONArray("fields")?.toList(),
            inverted = json.optBoolean("inverted", false)
        )
    }
}
```

---

### 3. Subject Type Detection Strategy

**Decision**: Kotlin reflection (`KClass`) with Java `Class` fallback

**Rationale**:
- iOS CASL uses Swift's native type system for automatic type detection
- Kotlin reflection (`::class`) is part of Kotlin standard library (kotlin-reflect not needed for basic KClass operations)
- Simple class name extraction doesn't require full reflection capabilities
- Java interop works naturally with `Class<*>` references
- No code generation needed, keeps build simple

**Alternatives Considered**:
- **Full kotlin-reflect library**: Adds 2.5MB dependency, violates <100KB package size (SC-008)
- **Annotation processing**: Unnecessary complexity for simple class name extraction
- **Manual type registration**: Requires explicit type mapping, reduces developer ergonomics
- **String-based types only**: Loses type safety, increases error potential

**Implementation Pattern**:
```kotlin
object SubjectTypeDetector {
    fun detectType(subject: Any): String = when (subject) {
        is String -> subject // Treat strings as type literals
        else -> subject::class.simpleName ?: subject.javaClass.simpleName
    }
}

// Usage examples
ability.can("read", "BlogPost") // String literal type
ability.can("read", blogPostInstance) // Auto-detected as "BlogPost"
```

---

### 4. Condition Matching with Deep Equality

**Decision**: Recursive equality checking with type coercion for JSON compatibility

**Rationale**:
- FR-013 requires deep equality matching on nested structures
- JSON deserialization may produce different numeric types (Int vs Long vs Double)
- iOS CASL handles similar type coercion for NSNumber compatibility
- Performance impact minimal for typical condition depth (1-3 levels)

**Alternatives Considered**:
- **Strict equality (===)**: Fails for JSON round-trips (Int vs Long mismatch)
- **Custom DSL for conditions**: Over-engineered, reduces flexibility
- **JSONObject comparison**: Requires serializing subjects to JSON (too slow)

**Implementation Pattern**:
```kotlin
object ConditionMatcher {
    fun matches(conditions: Map<String, Any?>, subject: Any): Boolean {
        val subjectFields = extractFields(subject)
        return conditions.all { (key, expectedValue) ->
            val actualValue = subjectFields[key]
            deepEquals(expectedValue, actualValue)
        }
    }

    private fun deepEquals(expected: Any?, actual: Any?): Boolean = when {
        expected == null && actual == null -> true
        expected == null || actual == null -> false
        expected::class == actual::class -> expected == actual
        isNumeric(expected) && isNumeric(actual) ->
            toDouble(expected) == toDouble(actual) // Numeric coercion
        expected is Map<*, *> && actual is Map<*, *> ->
            compareMapsFurther (expected, actual) // Recursive
        expected is List<*> && actual is List<*> ->
            compareListsDeep(expected, actual) // Recursive
        else -> expected == actual
    }
}
```

---

### 5. Field-Level Permission Indexing

**Decision**: Separate index for field-restricted rules with field-then-action lookup

**Rationale**:
- Field-level permissions are less common than resource-level permissions
- Separate index avoids polluting main rule lookup with field checks
- Field checks only execute when explicit field parameter provided
- Lookup order: field-specific rules first, then resource-level rules (most specific wins)

**Alternatives Considered**:
- **Single unified index**: Every permission check must filter field rules, adds overhead
- **Field-first lookup always**: Adds overhead when no field specified (common case)
- **Separate field validation pass**: Requires two lookups, slower

**Implementation Pattern**:
```kotlin
class RuleIndex(
    private val resourceRules: Map<String, List<Rule>>, // action+subject -> rules
    private val fieldRules: Map<String, Map<String, List<Rule>>> // field -> action+subject -> rules
) {
    fun findMatchingRule(action: String, subject: Any?, field: String?): Rule? {
        val subjectType = SubjectTypeDetector.detectType(subject)
        val key = "$action:$subjectType"

        // Check field-specific rules first if field provided
        if (field != null) {
            fieldRules[field]?.get(key)?.lastOrNull { it.matches(subject) }
                ?.let { return it }
        }

        // Fall back to resource-level rules
        return resourceRules[key]?.lastOrNull { it.matches(subject) }
    }
}
```

---

### 6. Java Compatibility Layer Design

**Decision**: Kotlin implementation with Java-friendly method overloads and builder pattern

**Rationale**:
- Kotlin compiles to JVM bytecode, naturally accessible from Java
- `@JvmOverloads` annotation generates Java-friendly method overloads for default parameters
- `@JvmStatic` for companion object methods exposes static methods to Java
- Builder pattern familiar to Java developers, no need for separate Java implementations
- Kotlin nullable types map to `@Nullable`/`@NonNull` annotations via JSR-305

**Alternatives Considered**:
- **Separate Java implementation**: Code duplication, maintenance burden, violates DRY
- **Pure Java implementation**: Loses Kotlin idioms (data classes, sealed classes, coroutines)
- **Java-only API with Kotlin extensions**: Forces lowest common denominator API

**Implementation Pattern**:
```kotlin
class Ability private constructor(rules: List<Rule>) {
    @JvmOverloads
    fun can(action: String, subject: Any?, field: String? = null): Boolean {
        // Implementation
    }

    companion object {
        @JvmStatic
        fun builder(): AbilityBuilder = AbilityBuilder()
    }
}

// Java usage:
// Ability ability = Ability.builder()
//     .can("read", "BlogPost")
//     .cannot("delete", "BlogPost")
//     .build();
```

---

### 7. Asynchronous API Design for Kotlin Coroutines

**Decision**: Suspend functions wrapping synchronous implementation, no actual async work

**Rationale**:
- FR-006 requires async API for coroutines, but FR-005 requires sync API
- Permission checks are CPU-bound with <1ms completion time (SC-002)
- No I/O operations warrant true async (in-memory only)
- Suspend functions satisfy coroutine API requirement without overhead
- `withContext(Dispatchers.Default)` available if CPU-intensive work identified later

**Alternatives Considered**:
- **Truly async with coroutines**: Unnecessary overhead for sub-millisecond operations
- **Callbacks**: Not idiomatic for Kotlin, more complex than coroutines
- **RxJava/Flow**: Adds complexity and dependencies for no benefit

**Implementation Pattern**:
```kotlin
class Ability {
    // Synchronous API (primary)
    fun can(action: String, subject: Any?, field: String? = null): Boolean {
        return ruleIndex.findMatchingRule(action, subject, field)?.inverted != true
    }

    // Asynchronous API (delegates to sync)
    suspend fun canAsync(action: String, subject: Any?, field: String? = null): Boolean {
        // Could use withContext(Dispatchers.Default) if needed
        return can(action, subject, field)
    }
}
```

---

### 8. Rule Precedence Implementation

**Decision**: List-based storage with last-match-wins evaluation

**Rationale**:
- FR-008 specifies later rules override earlier rules (matches iOS CASL behavior)
- `List<Rule>` maintains insertion order naturally
- `lastOrNull { predicate }` evaluates rules in reverse order
- Simple, predictable, matches developer mental model

**Alternatives Considered**:
- **Priority-based system**: Adds complexity, not required by spec
- **First-match-wins**: Contradicts iOS CASL behavior (feature parity requirement SC-007)
- **Explicit precedence values**: Over-engineered for simple "last wins" requirement

**Implementation Pattern**:
```kotlin
fun findMatchingRule(action: String, subject: Any?, field: String?): Rule? {
    val candidates = getRulesForActionAndSubject(action, subject)
    // Last matching rule wins (reverse evaluation)
    return candidates.lastOrNull { rule ->
        rule.matchesConditions(subject) && rule.matchesField(field)
    }
}
```

---

## Performance Validation Strategy

Based on success criteria (SC-002, SC-003, SC-004, SC-009):

1. **Micro-benchmarks**: JMH benchmarks for permission check latency
2. **Concurrency stress tests**: 10,000 concurrent operations across multiple threads
3. **Serialization benchmarks**: Measure JSON encoding/decoding for varying rule set sizes
4. **Memory profiling**: Ensure no memory leaks during rule updates
5. **AAR size validation**: Gradle task to fail build if package exceeds 100KB

---

## Integration Testing Strategy

Per FR-007 (thread safety) and SC-009 (zero crashes under load):

1. **Concurrency tests**: Multiple threads performing simultaneous can/cannot checks during rule updates
2. **Serialization round-trip tests**: Ensure JSON export → import produces identical behavior
3. **Feature parity tests**: Port iOS CASL test suite to validate equivalent behavior
4. **Java compatibility tests**: Pure Java test classes validating API usability without Kotlin
5. **Android instrumentation tests**: Verify behavior on actual devices with Robolectric fallback

---

## Build Configuration Decisions

**Gradle Configuration**:
- Kotlin 1.9.x (stable, good Java interop)
- Target Android API 34 (latest stable)
- Minimum Android API 21 (per FR-019)
- Java 11 bytecode compatibility (broad compatibility)
- ProGuard rules for library consumers
- Maven publishing for distribution

**Testing Dependencies**:
- JUnit 5 (modern, flexible)
- Mockito Kotlin (mocking when needed)
- Robolectric (Android testing without emulator)
- JMH (performance benchmarking)

---

## Documentation Requirements

Per SC-010 (100% API documentation):

1. **KDoc comments**: All public classes, functions, and parameters
2. **Usage examples**: Inline code samples in KDoc
3. **README.md**: Quick start guide, installation, basic usage
4. **MIGRATION.md**: Mapping from iOS CASL API to Android equivalents
5. **API reference**: Generated Dokka HTML documentation

---

## Summary

All technical unknowns resolved. Implementation approach:
- Kotlin primary with Java compatibility via annotations
- Volatile snapshots + synchronized updates for thread safety
- Android SDK's org.json for serialization (zero external deps)
- Kotlin reflection (basic) for type detection
- Suspend functions wrapping sync implementation for coroutine support
- List-based rule storage with last-match-wins precedence

No architectural blockers identified. Ready for Phase 1 design artifacts.
