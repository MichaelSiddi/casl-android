# Changelog

All notable changes to the CASL Android Authorization Library will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-10-23

### Added

#### Core Features
- **Rule Definition API**: Fluent builder API with `can()` and `cannot()` methods
- **Permission Checking**: Synchronous (`can`/`cannot`) and asynchronous (`canAsync`/`cannotAsync`) methods
- **Attribute-Based Conditions**: Match resource attributes with deep equality
- **Field-Level Permissions**: Restrict access to specific fields
- **JSON Serialization**: Export/import rules with `RawRule.toJson()` and `RawRule.fromJson()`
- **Runtime Updates**: Dynamic rule replacement with `Ability.update()`

#### Technical Features
- **Thread-Safe Concurrency**: Volatile snapshots + synchronized updates pattern
- **Zero Dependencies**: Only uses Android SDK (org.json) and Kotlin standard library
- **Last-Match-Wins Precedence**: Later-defined rules override earlier ones
- **Subject Type Detection**: Automatic type detection from instances
- **Deep Equality Matching**: Handles nested objects, lists, and numeric type coercion

#### Language Support
- **Java Compatibility**: Full support with `@JvmStatic`, `@JvmOverloads` annotations
- **Kotlin DSL**: Optional extensions with `buildAbility {}`, enhanced `can`/`cannot` syntax
- **List Extensions**: `filterByAction()`, `filterBySubject()`, `onlyPermissions()`, `onlyProhibitions()`

#### Project Assets
- **Sample Application**: Complete demo with 5 examples (RBAC, field-level, JSON, DSL)
- **Comprehensive Tests**: Performance tests, edge case validation, concurrency tests
- **ProGuard Rules**: Consumer rules for library integration
- **Build Configuration**: Gradle multi-module setup ready for AAR publishing

### Performance
- Permission checks: <1ms for 100 rules (measured)
- Concurrent operations: 1000+ simultaneous checks supported
- Serialization: <10ms for 100 rules (measured)
- Stress tested: 10,000 concurrent operations without failures

### Compatibility
- Minimum Android API: 21 (Android 5.0 Lollipop)
- Kotlin: 1.9.20+
- Java: 11+
- Target Android API: 34

### Documentation
- README.md with installation and quick start
- KDoc comments on all public APIs
- Sample app demonstrating all features
- API contracts specification

### Known Limitations
- No async I/O operations (permission checks are CPU-bound)
- Reflection-based field extraction (consider performance for hot paths)

## [2.2.0] - 2025-10-27

### Added
- **Native JSONObject Support**: Direct integration with Android's JSONObject and JSONArray
  - New `ability.can(action, subjectType, jsonObject)` overload for seamless JSON permission checks
  - New `ability.cannot(action, subjectType, jsonObject)` overload
  - No manual conversion needed - CASL automatically converts JSON to Map internally
  - Works with deeply nested JSON structures using dot notation
  - Full support for MongoDB-style operators in JSON conditions
  - Array element access using numeric indices (e.g., `"tags.0"`)

- **JSON Utility Functions**: Helper functions for JSON manipulation
  - `jsonObjectToMap()`: Convert JSONObject to Map recursively
  - `jsonArrayToList()`: Convert JSONArray to List
  - `subjectFromJson()`: Create ForcedSubject from JSONObject

### Enhanced
- **Ability Class**: Added JSONObject overloads for can() and cannot() methods
- **Documentation**: New comprehensive JSON_SUPPORT.md guide with real-world examples
- **Test Coverage**: 18 new tests for JSONObject support (249 total tests, up from 231)

### Use Cases
- Check permissions directly against API responses (no conversion needed)
- Work with JSON from databases, caches, or other sources
- Seamless integration with Retrofit, Room, or other Android data layers
- Type-safe permission checks without manual Map construction

### Example
```kotlin
// Before v2.2.0: Manual conversion required
val json = JSONObject(apiResponse)
val map = manuallyConvertToMap(json)
val subject = subject("Post", map)
val canUpdate = ability.can("update", subject)

// After v2.2.0: Direct JSON support
val json = JSONObject(apiResponse)
val canUpdate = ability.can("update", "Post", json)  // That's it!
```

### Performance
- JSON conversion: <1ms for simple objects, <5ms for complex nested structures
- Zero overhead for non-JSON subjects (existing Map-based API unchanged)
- Lazy conversion only when needed

### Breaking Changes
- None - All changes are backward compatible
- Existing Map-based API continues to work unchanged
- New JSON API is opt-in

## [2.1.0] - 2025-10-27

### Added
- **Field Pattern Matching with Wildcards**: Support for advanced field permission patterns
  - Single-level wildcards: `author.*` matches any immediate child field
  - Multi-level wildcards: `author.**` matches fields at any depth
  - Wildcards at start: `*.name` or `**.name` for matching across parent objects
  - Prefix matching: `street*` matches fields starting with "street"
  - New `FieldMatcher` utility for efficient pattern matching with regex generation
- **ForbiddenError Auto-Reason**: Automatic extraction of `reason` from inverted rules
  - Added `reason` parameter to `can()` and `cannot()` builder methods
  - ForbiddenError automatically uses rule reason when permission denied
  - Improved error messages for better user experience
- **rulesToFields Utility**: Extract default field values from authorization rules
  - Useful for form initialization based on user permissions
  - Supports dot notation for nested fields (`author.address.city`)
  - Automatically skips MongoDB query operators
  - New `RulesToFields.kt` in extra utilities package

### Fixed
- **ForbiddenError.setDefaultMessage**: Fixed global default message generator
  - Now properly called during message generation, not initialization
  - Correctly accesses `action`, `subject`, `subjectType`, and `field` properties
  - Both static string and function generators now work correctly

### Enhanced
- **Rule**: Added `reason` property for explanatory messages on prohibited actions
- **RawRule**: Added `reason` property with JSON serialization/deserialization support
- **AbilityBuilder**: All `can()` and `cannot()` methods now support optional `reason` parameter

### Test Coverage
- Added 39 new tests (217 total tests, up from 178)
  - FieldMatcher unit tests: 9 tests
  - FieldPattern integration tests: 14 tests
  - RulesToFields utility tests: 13 tests
  - ForbiddenError enhancements: 6 tests

### Performance
- Field pattern matching optimized with lazy initialization
- Non-wildcard patterns use fast set lookup (no regex overhead)
- Wildcard patterns compiled once and cached per rule

### Breaking Changes
- None - All changes are backward compatible
- Existing code continues to work without modifications
- New features are opt-in through additional parameters

## [2.0.0] - 2025-10-24

### Added
- Initial release with core features
- Thread-safe rule management
- Attribute-based conditions
- Field-level permissions
- JSON serialization

## [Unreleased]

### Planned
- Dokka-generated API documentation
- Migration guide from iOS CASL
- Additional performance optimizations
- Extended Kotlin DSL features

---

## Version History

- **2.2.0** (2025-10-27): Native JSONObject support, seamless API integration
- **2.1.0** (2025-10-27): Field pattern matching, auto-reason, rulesToFields utility, setDefaultMessage fix
- **2.0.0** (2025-10-24): Major release with enhanced features
- **1.0.0** (2025-10-23): Initial release with full feature parity to iOS CASL library
