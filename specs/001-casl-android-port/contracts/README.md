# API Contracts

This directory contains the public API contracts for the CASL Android Authorization Library.

## Files

### ability-api.kt
Core API surface that all consumers depend on:
- `Ability` - Main authorization manager
- `AbilityBuilder` - Fluent rule builder
- `RawRule` - Serializable rule representation

**Stability**: This API is covered by semantic versioning. Changes require major version bump.

### kotlin-extensions.kt
Optional Kotlin DSL extensions for enhanced developer experience:
- `buildAbility {}` - DSL for building abilities
- Infix operators for readable permission checks
- Extension functions for rule manipulation

**Stability**: These extensions are additive - new extensions can be added in minor versions. Existing extensions follow semver.

## API Guarantees

### Thread Safety
- All `Ability` methods are thread-safe and can be called from any thread
- `AbilityBuilder` is NOT thread-safe - use from single thread only
- `RawRule` is immutable and inherently thread-safe

### Performance Commitments
- Permission checks complete in <1ms for rule sets up to 100 rules (SC-002)
- Concurrent permission checks scale to 1000+ simultaneous operations (SC-003)
- Rule serialization/deserialization completes in <10ms for 100 rules (SC-004)

### Null Safety
- Permission checks on `null` subjects always return `false`
- Methods document nullable parameters with `?` syntax
- Kotlin null-safety enforced at compile time

### Exception Behavior
- `IllegalArgumentException` for invalid parameters (blank strings, empty collections)
- `IllegalStateException` for invalid builder state
- `org.json.JSONException` for JSON parsing errors
- No checked exceptions (Kotlin style)

## Breaking vs Non-Breaking Changes

### Breaking Changes (Major Version)
- Removing public methods or classes
- Changing method signatures (parameters, return types)
- Changing exception behavior
- Changing thread-safety guarantees
- Changing performance characteristics significantly

### Non-Breaking Changes (Minor Version)
- Adding new public methods or classes
- Adding new optional parameters with defaults
- Adding new Kotlin extensions
- Improving performance
- Adding overloads

### Patch Changes
- Bug fixes maintaining API compatibility
- Documentation improvements
- Internal implementation changes

## Java Compatibility

All APIs marked with `@JvmStatic`, `@JvmOverloads`, etc. maintain Java compatibility:

```java
// Java example
Ability ability = Ability.builder()
    .can("read", "BlogPost")
    .cannot("delete", "BlogPost")
    .build();

if (ability.can("read", blogPost)) {
    // Allow access
}
```

## Testing Contracts

Public API contracts are tested via:
- **Unit tests**: Verify individual method behavior
- **Contract tests**: Ensure API stability across versions
- **Java compatibility tests**: Pure Java test classes
- **Integration tests**: Multi-threaded scenarios

See `casl/src/test/kotlin/com/casl/contract/` for contract test implementations.
