# Implementation Plan: CASL Android Authorization Library

**Branch**: `001-casl-android-port` | **Date**: 2025-10-23 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-casl-android-port/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Port the CASL iOS authorization library to Android, providing an attribute-based access control (ABAC) library that supports role-based authorization, field-level permissions, and isomorphic rule serialization. The library will offer a fluent builder API for defining authorization rules, thread-safe permission checking, and full compatibility with both Java and Kotlin codebases. Core capabilities include synchronous and asynchronous permission checks, JSON serialization for dynamic rule updates, and zero external dependencies beyond the Android SDK and Kotlin standard library.

## Technical Context

**Language/Version**: Kotlin 1.9+ (primary), Java 11+ compatibility (secondary)
**Primary Dependencies**: None (zero dependencies beyond Android SDK and Kotlin standard library per FR-016)
**Storage**: N/A (in-memory rule storage only)
**Testing**: JUnit 5 for unit tests, Robolectric for Android-specific tests
**Target Platform**: Android API 21+ (Android 5.0 Lollipop and above, per FR-019)
**Project Type**: Android Library (single library module, published as AAR)
**Performance Goals**: <1ms permission checks for 100 rules (SC-002), 1000+ concurrent checks (SC-003), <10ms serialization for 100 rules (SC-004)
**Constraints**: Package size <100KB (SC-008), zero crashes under 10k concurrent operations (SC-009), feature parity with iOS CASL library (SC-007)
**Scale/Scope**: Library for integration into Android applications, supports complex rule sets with nested conditions, field-level permissions, and attribute-based access control

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Note**: Constitution file contains template placeholders only. Applying standard library development principles:

| Principle | Status | Notes |
|-----------|--------|-------|
| Library-First Architecture | ✅ PASS | Single standalone library with clear purpose: authorization/access control |
| Zero Dependencies | ✅ PASS | FR-016 requires no external dependencies beyond Android SDK and Kotlin stdlib |
| Test Coverage | ⚠️ PENDING | TDD approach required, tests must be written before implementation (Phase 2) |
| API Clarity | ✅ PASS | Fluent builder pattern, clear can/cannot methods, Java and Kotlin compatibility |
| Performance Standards | ✅ PASS | Explicit performance goals defined in SC-002, SC-003, SC-004 |
| Platform Compatibility | ✅ PASS | Android API 21+ ensures 99%+ device coverage |
| Package Size | ✅ PASS | <100KB constraint prevents bloat |
| Thread Safety | ✅ PASS | FR-007 mandates thread-safe concurrent operations |

**Initial Assessment**: All gates PASS or PENDING. No constitutional violations requiring justification.

**Post-Design Re-evaluation** (after Phase 1):

| Principle | Status | Post-Design Notes |
|-----------|--------|-------------------|
| Library-First Architecture | ✅ PASS | Confirmed: Single library module (casl/) with sample app for demos |
| Zero Dependencies | ✅ PASS | Confirmed: Using only org.json from Android SDK, no external dependencies |
| Test Coverage | ✅ READY | Test structure defined: unit/, integration/, contract/ test packages |
| API Clarity | ✅ PASS | API contracts documented, both synchronous and async APIs defined |
| Performance Standards | ✅ PASS | Research confirms <1ms checks, <10ms serialization achievable |
| Platform Compatibility | ✅ PASS | API 21+ requirement confirmed, covers 99%+ devices |
| Package Size | ✅ PASS | Estimated 20KB for library, well under 100KB limit |
| Thread Safety | ✅ PASS | Volatile snapshots + synchronized updates pattern documented |

**Final Assessment**: All constitutional gates PASS. Design phase complete. Ready for Phase 2 (task generation via `/speckit.tasks`).

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
casl-android/                           # Repository root
├── casl/                               # Library module
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/
│   │   │   │   └── com/
│   │   │   │       └── casl/
│   │   │   │           ├── Ability.kt            # Main authorization manager
│   │   │   │           ├── AbilityBuilder.kt     # Fluent rule builder
│   │   │   │           ├── Rule.kt               # Internal rule representation
│   │   │   │           ├── RawRule.kt            # Serializable rule format
│   │   │   │           ├── RuleIndex.kt          # Rule storage and lookup
│   │   │   │           ├── ConditionMatcher.kt   # Condition evaluation logic
│   │   │   │           ├── SubjectTypeDetector.kt # Type detection from instances
│   │   │   │           └── extensions/
│   │   │   │               └── KotlinDsl.kt      # Kotlin-specific DSL extensions
│   │   │   └── java/
│   │   │       └── com/
│   │   │           └── casl/
│   │   │               └── compat/
│   │   │                   └── JavaBuilder.kt    # Java-friendly builder wrappers
│   │   └── test/
│   │       ├── kotlin/
│   │       │   └── com/
│   │       │       └── casl/
│   │       │           ├── unit/                 # Unit tests (single class)
│   │       │           │   ├── AbilityTest.kt
│   │       │           │   ├── RuleIndexTest.kt
│   │       │           │   └── ConditionMatcherTest.kt
│   │       │           ├── integration/          # Integration tests (multiple classes)
│   │       │           │   ├── ConcurrencyTest.kt
│   │       │           │   ├── SerializationTest.kt
│   │       │           │   └── FeatureParityTest.kt
│   │       │           └── contract/             # Public API contract tests
│   │       │               ├── JavaCompatTest.java
│   │       │               └── KotlinDslTest.kt
│   │       └── resources/
│   │           └── test-fixtures/                # Sample rule JSON files
│   │               └── sample-rules.json
│   └── build.gradle.kts                          # Library module build config
├── sample/                                       # Sample app demonstrating usage
│   ├── src/
│   │   └── main/
│   │       ├── kotlin/
│   │       │   └── com/
│   │       │       └── casl/
│   │       │           └── sample/
│   │       │               └── MainActivity.kt
│   │       └── res/
│   │           └── layout/
│   │               └── activity_main.xml
│   └── build.gradle.kts                          # Sample app build config
├── settings.gradle.kts                           # Multi-module project settings
├── build.gradle.kts                              # Root build config
├── gradle.properties                             # Gradle properties
└── README.md                                     # Library documentation
```

**Structure Decision**: Android Library module structure following Gradle multi-module conventions. The `casl/` module is the publishable library (AAR), while `sample/` demonstrates integration patterns. Kotlin source is primary with Java compatibility layer. Test structure separates unit (single class), integration (multiple classes), and contract (API stability) tests.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No constitutional violations detected. Complexity is justified by requirements:
- Thread-safe concurrent access (FR-007) → Requires synchronization mechanisms
- Deep equality matching (FR-013) → Requires recursive condition evaluation
- Type detection (FR-012) → Requires reflection or code generation
- Zero dependencies (FR-016) → Must implement JSON serialization manually
