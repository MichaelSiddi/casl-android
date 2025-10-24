---
description: "Implementation tasks for CASL Android Authorization Library"
---

# Tasks: CASL Android Authorization Library

**Input**: Design documents from `/specs/001-casl-android-port/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Not explicitly requested in feature specification. Test tasks are excluded per project guidelines.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Android Library project structure:
- **Library module**: `casl/src/main/kotlin/com/casl/`
- **Test module**: `casl/src/test/kotlin/com/casl/`
- **Sample app**: `sample/src/main/kotlin/com/casl/sample/`
- **Build configs**: `build.gradle.kts`, `settings.gradle.kts`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic Gradle structure

- [X] T001 Create root Gradle project structure with settings.gradle.kts and root build.gradle.kts
- [X] T002 Create library module directory structure at casl/src/main/kotlin/com/casl/
- [X] T003 Create test directory structure at casl/src/test/kotlin/com/casl/
- [X] T004 Create sample app module directory structure at sample/src/main/kotlin/com/casl/sample/
- [X] T005 Configure library module build.gradle.kts with Kotlin 1.9+, Android API 21+ target
- [X] T006 [P] Configure root gradle.properties with Kotlin and Android SDK versions
- [X] T007 [P] Create .gitignore for Android/Gradle projects
- [X] T008 [P] Create README.md with installation and quick start instructions

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core utility classes that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T009 Implement SubjectTypeDetector utility in casl/src/main/kotlin/com/casl/SubjectTypeDetector.kt
- [X] T010 [P] Implement ConditionMatcher utility in casl/src/main/kotlin/com/casl/ConditionMatcher.kt
- [X] T011 [P] Create RawRule data class with JSON serialization in casl/src/main/kotlin/com/casl/RawRule.kt
- [X] T012 Create internal Rule data class in casl/src/main/kotlin/com/casl/Rule.kt
- [X] T013 Add Rule.toRawRule() and RawRule.toRule() conversion methods
- [X] T014 Implement JSON serialization helpers using org.json.JSONObject in RawRule.kt
- [X] T015 Add RawRule.fromJson() and RawRule.toJson() methods
- [X] T016 Add RawRule.listFromJson() and RawRule.listToJson() companion methods for array handling

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Define Authorization Rules (Priority: P1) 🎯 MVP

**Goal**: Enable developers to define authorization rules using a fluent builder API

**Independent Test**: Create an AbilityBuilder, add can/cannot rules, build an Ability instance, and verify rules are stored without performing permission checks

### Implementation for User Story 1

- [X] T017 [P] [US1] Create AbilityBuilder class with mutable rule accumulation in casl/src/main/kotlin/com/casl/AbilityBuilder.kt
- [X] T018 [P] [US1] Create empty Ability class skeleton in casl/src/main/kotlin/com/casl/Ability.kt
- [X] T019 [US1] Implement AbilityBuilder.can() method with action, subject, conditions, fields parameters
- [X] T020 [US1] Implement AbilityBuilder.cannot() method (inverted rules)
- [X] T021 [US1] Implement AbilityBuilder.clear() method to reset accumulated rules
- [X] T022 [US1] Implement AbilityBuilder.build() method that creates Ability instance
- [X] T023 [US1] Add @JvmOverloads annotations to can/cannot methods for Java compatibility
- [X] T024 [US1] Add input validation to can/cannot methods (non-blank action/subject, non-empty fields)
- [X] T025 [US1] Add Ability.builder() companion method marked @JvmStatic
- [X] T026 [US1] Add Ability.fromRules() companion method for creating Ability from RawRule list
- [X] T027 [US1] Implement Ability constructor that accepts List<Rule> and initializes RuleIndex

**Checkpoint**: At this point, developers can define rules programmatically - User Story 1 is complete

---

## Phase 4: User Story 2 - Check User Permissions at Runtime (Priority: P1)

**Goal**: Enable applications to check permissions synchronously and asynchronously

**Independent Test**: Define a simple rule set, create test subject instances, call can/cannot methods, verify correct true/false results

### Implementation for User Story 2

- [X] T028 [US2] Create RuleIndex class with resourceRules and fieldRules maps in casl/src/main/kotlin/com/casl/RuleIndex.kt
- [X] T029 [US2] Implement RuleIndex.fromRawRules() companion method to build indexes from rule list
- [X] T030 [US2] Implement RuleIndex.empty() companion method for initial state
- [X] T031 [US2] Implement RuleIndex.findMatchingRule() method with action, subject, field parameters
- [X] T032 [US2] Add rule indexing logic: map rules by "action:subjectType" key for fast lookup
- [X] T033 [US2] Add field-specific rule indexing: separate index for field-restricted rules
- [X] T034 [US2] Implement last-match-wins precedence using lastOrNull() evaluation order
- [X] T035 [US2] Integrate SubjectTypeDetector for automatic type detection in findMatchingRule()
- [X] T036 [US2] Integrate ConditionMatcher for evaluating rule conditions against subjects
- [X] T037 [US2] Add field matching logic: check field restrictions when field parameter provided
- [X] T038 [US2] Implement Ability.can() method using RuleIndex.findMatchingRule()
- [X] T039 [US2] Implement Ability.cannot() method as inverse of can()
- [X] T040 [US2] Add @JvmOverloads annotations to can/cannot for optional field parameter
- [X] T041 [US2] Add null subject handling: return false immediately for null subjects
- [X] T042 [US2] Implement Ability.canAsync() suspend function delegating to can()
- [X] T043 [US2] Implement Ability.cannotAsync() suspend function delegating to cannot()
- [X] T044 [US2] Add @Volatile annotation to Ability.ruleIndex field for thread visibility
- [X] T045 [US2] Add inline documentation for thread-safety guarantees

**Checkpoint**: At this point, permission checking works with thread-safe concurrent access - User Story 2 is complete

---

## Phase 5: User Story 3 - Serialize and Deserialize Rules (Priority: P2)

**Goal**: Enable JSON export/import of rules for isomorphic authorization

**Independent Test**: Define a rule set, export to JSON string, create new Ability from JSON, verify both produce identical permission check results

### Implementation for User Story 3

- [X] T046 [US3] Implement Ability.exportRules() method returning List<RawRule>
- [X] T047 [US3] Add internal Rule.toRawRule() conversion in exportRules() implementation
- [X] T048 [US3] Enhance RawRule.toJson() to handle nested conditions (maps, lists, objects)
- [X] T049 [US3] Enhance RawRule.fromJson() to handle nested conditions with type coercion
- [X] T050 [US3] Add JSONObject.toMap() extension helper for condition deserialization
- [X] T051 [US3] Add JSONArray.toList() extension helper for fields deserialization
- [X] T052 [US3] Implement numeric type coercion in JSON parsing (Int/Long/Double normalization)
- [X] T053 [US3] Add error handling for malformed JSON (throw JSONException with clear messages)
- [X] T054 [US3] Add validation in RawRule init block (non-blank action/subject, non-empty fields)

**Checkpoint**: At this point, rules can be serialized to/from JSON for dynamic loading - User Story 3 is complete

---

## Phase 6: User Story 4 - Update Rules at Runtime (Priority: P2)

**Goal**: Enable dynamic rule updates without application restart

**Independent Test**: Create Ability with initial rules, call update() with new rules, verify subsequent permission checks use only new rules

### Implementation for User Story 4

- [X] T055 [US4] Implement Ability.update() method with synchronized block in casl/src/main/kotlin/com/casl/Ability.kt
- [X] T056 [US4] Add RawRule list parameter to update() method
- [X] T057 [US4] Convert RawRule list to internal Rule list inside update()
- [X] T058 [US4] Build new RuleIndex from converted rules inside synchronized block
- [X] T059 [US4] Atomically replace volatile ruleIndex field with new index
- [X] T060 [US4] Add error handling for invalid rules (throw IllegalArgumentException)
- [X] T061 [US4] Add documentation explaining thread-safety: ongoing checks use old rules, new checks use new rules

**Checkpoint**: At this point, rules can be updated dynamically at runtime - User Story 4 is complete

---

## Phase 7: User Story 5 - Java and Kotlin Compatibility (Priority: P3)

**Goal**: Provide idiomatic API patterns for both Java and Kotlin developers

**Independent Test**: Write identical authorization logic in pure Java and pure Kotlin, verify both compile and produce equivalent runtime behavior

### Implementation for User Story 5

- [X] T062 [P] [US5] Create Kotlin DSL extensions file at casl/src/main/kotlin/com/casl/extensions/KotlinDsl.kt
- [X] T063 [P] [US5] Create Java compatibility layer at casl/src/main/java/com/casl/compat/JavaBuilder.kt
- [X] T064 [US5] Implement buildAbility() DSL function with lambda syntax
- [X] T065 [US5] Add enhanced can() extension with ConditionsBuilder for inline condition DSL
- [X] T066 [US5] Add enhanced cannot() extension with ConditionsBuilder
- [X] T067 [US5] Add List<RawRule> extension methods (filterByAction, filterBySubject, onlyPermissions, onlyProhibitions)
- [X] T068 [US5] Add Any.subjectTypeName extension property for type name extraction
- [X] T069 [US5] Review all public API methods for @JvmStatic and @JvmOverloads annotations
- [X] T070 [US5] Add @file:JvmName annotations to Kotlin files for clean Java imports
- [X] T071 [US5] Ensure nullable types have proper @Nullable/@NonNull JSR-305 annotations

**Checkpoint**: At this point, library works idiomatically from both Java and Kotlin - User Story 5 is complete

---

## Phase 8: Sample Application & Documentation

**Purpose**: Demonstrate library usage and provide developer onboarding

- [X] T072 [P] Create sample app MainActivity.kt with basic authorization examples at sample/src/main/kotlin/com/casl/sample/MainActivity.kt
- [X] T073 [P] Add sample app activity_main.xml layout at sample/src/main/res/layout/activity_main.xml
- [X] T074 [P] Implement role-based authorization example in sample app (admin, editor, viewer roles)
- [X] T075 [P] Implement field-level permission example in sample app
- [X] T076 [P] Implement JSON serialization/deserialization example in sample app
- [X] T077 [P] Add sample app build.gradle.kts with dependency on casl library module
- [X] T078 [P] Add sample domain models (BlogPost, User) in sample app
- [X] T079 [P] Generate KDoc documentation with Dokka for all public APIs
- [X] T080 [P] Create API reference documentation in docs/ directory
- [X] T081 [P] Add code examples to README.md from quickstart.md
- [X] T082 [P] Create MIGRATION.md guide mapping iOS CASL API to Android equivalents

---

## Phase 9: Build Configuration & Distribution

**Purpose**: Configure library publishing and distribution

- [X] T083 Configure Maven publishing in library build.gradle.kts
- [X] T084 [P] Add library metadata (group ID, artifact ID, version) to gradle.properties
- [X] T085 [P] Create consumer ProGuard rules in casl/proguard-rules.pro
- [X] T086 [P] Configure AAR generation and size validation (<100KB requirement)
- [X] T087 [P] Add Gradle task to measure and report library size
- [X] T088 [P] Configure Kotlin compiler options (JVM target 11, explicit API mode)
- [X] T089 [P] Add manifest file for library module at casl/src/main/AndroidManifest.xml

---

## Phase 10: Quality & Performance

**Purpose**: Ensure library meets performance and quality standards

- [X] T090 [P] Add performance benchmark setup with JMH in casl/src/test/kotlin/com/casl/benchmarks/
- [X] T091 [P] Create permission check latency benchmark (target: <1ms for 100 rules)
- [X] T092 [P] Create concurrent access benchmark (target: 1000+ concurrent checks)
- [X] T093 [P] Create serialization performance benchmark (target: <10ms for 100 rules)
- [X] T094 [P] Add memory profiling test for rule updates (verify no leaks)
- [X] T095 [P] Create stress test: 10,000 concurrent operations across threads
- [X] T096 [P] Add edge case validation: null subjects, missing attributes, conflicting rules
- [X] T097 [P] Verify last-match-wins precedence with explicit test cases
- [X] T098 [P] Test deep equality matching for nested conditions
- [X] T099 [P] Test numeric type coercion across Int/Long/Double types
- [X] T100 [P] Run quickstart.md validation to ensure examples compile and run

---

## Phase 11: Polish & Cross-Cutting Concerns

**Purpose**: Final touches and quality improvements

- [X] T101 [P] Code review: Check all public APIs have KDoc comments
- [X] T102 [P] Code review: Verify 100% API documentation coverage
- [X] T103 [P] Add inline code examples to KDoc for common use cases
- [X] T104 [P] Review error messages for clarity (IllegalArgumentException, IllegalStateException)
- [X] T105 [P] Add helpful error messages when rules are invalid
- [X] T106 [P] Verify thread-safety documentation is clear and accurate
- [X] T107 [P] Run Kotlin linter and fix any warnings
- [X] T108 [P] Ensure consistent code formatting across all files
- [X] T109 [P] Add LICENSE file (if not already present)
- [X] T110 [P] Add CHANGELOG.md for version tracking
- [X] T111 [P] Final build verification: clean build, run all benchmarks, verify AAR size
- [X] T112 Prepare release notes highlighting feature parity with iOS CASL library

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 completion - BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Phase 2 completion
  - US1 (Phase 3): Can start after Foundational - No dependencies on other stories
  - US2 (Phase 4): Depends on US1 (needs AbilityBuilder and Ability classes)
  - US3 (Phase 5): Depends on US1 and US2 (needs Ability.exportRules and full permission checking)
  - US4 (Phase 6): Depends on US1 and US2 (needs Ability.update with RuleIndex)
  - US5 (Phase 7): Depends on US1 and US2 (enhances existing APIs) - can run in parallel with US3/US4
- **Sample App (Phase 8)**: Depends on US1, US2, US3 completion (demonstrates core features)
- **Build Config (Phase 9)**: Can run in parallel with Phase 8
- **Quality (Phase 10)**: Depends on all user stories being complete
- **Polish (Phase 11)**: Depends on all other phases

### User Story Dependencies

```text
Phase 2 (Foundation) ──┬──→ Phase 3 (US1: Define Rules) ──┬──→ Phase 4 (US2: Check Permissions)
                       │                                   │
                       │                                   ├──→ Phase 5 (US3: Serialize Rules)
                       │                                   │
                       │                                   ├──→ Phase 6 (US4: Update Rules)
                       │                                   │
                       └───────────────────────────────────┴──→ Phase 7 (US5: Java/Kotlin Compat)
```

**Critical Path**: Phase 1 → Phase 2 → Phase 3 (US1) → Phase 4 (US2) → Remaining Phases

### Within Each User Story

- User Story 1: All tasks can run sequentially, T017-T018 can start in parallel
- User Story 2: RuleIndex implementation (T028-T037) must complete before Ability methods (T038-T045)
- User Story 3: All tasks are enhancements to existing classes, can mostly run in parallel
- User Story 4: Single method implementation, sequential execution
- User Story 5: All extension files [P] parallelizable, API review sequential

### Parallel Opportunities

- Phase 1: Tasks T006, T007, T008 marked [P] can run in parallel
- Phase 2: Tasks T010, T011 marked [P] can run in parallel with T009
- Phase 5: Tasks T062, T063 marked [P] can start in parallel
- Phase 8: All tasks marked [P] are independent
- Phase 9: All tasks marked [P] are independent
- Phase 10: All tasks marked [P] are independent
- Phase 11: Most tasks marked [P] are independent

---

## Parallel Example: User Story 1 (Define Rules)

```bash
# Start in parallel:
Task T017: "Create AbilityBuilder class skeleton"
Task T018: "Create Ability class skeleton"

# Then sequentially:
Task T019-T027: "Implement builder methods and companion functions"
```

---

## Parallel Example: User Story 2 (Check Permissions)

```bash
# RuleIndex implementation (sequential):
Task T028-T037: "Implement RuleIndex with indexing and lookup logic"

# Then Ability methods (sequential, depends on RuleIndex):
Task T038-T045: "Implement can/cannot methods using RuleIndex"
```

---

## Parallel Example: Phase 10 (Quality & Performance)

```bash
# All benchmarks and tests can run in parallel:
Task T091: "Permission check latency benchmark"
Task T092: "Concurrent access benchmark"
Task T093: "Serialization performance benchmark"
Task T094: "Memory profiling test"
Task T095: "Stress test: 10k concurrent operations"
Task T096-T099: "Edge case and correctness tests"
```

---

## Implementation Strategy

### MVP First (User Stories 1 + 2 Only)

1. Complete Phase 1: Setup → Project structure ready
2. Complete Phase 2: Foundational → Utilities ready (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 → Rules can be defined
4. Complete Phase 4: User Story 2 → Permissions can be checked
5. **STOP and VALIDATE**: Test rule definition + permission checking independently
6. Deploy/demo if ready → **This is a functional authorization library MVP**

### Incremental Delivery

1. **MVP (US1 + US2)**: Define rules + check permissions → Core authorization works
2. **+ US3**: Add JSON serialization → Dynamic rule loading from backend
3. **+ US4**: Add runtime updates → Real-time permission changes
4. **+ US5**: Add language compatibility → Full Java/Kotlin parity
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. **Team completes Phase 1 + 2 together** (Setup + Foundation)
2. Once Phase 2 done:
   - **Developer A**: Phase 3 (US1: Define Rules)
   - **Developer B**: Phase 8 (Sample App), Phase 9 (Build Config) - can start early
3. After Phase 3 (US1) completes:
   - **Developer A**: Phase 4 (US2: Check Permissions)
4. After Phase 4 (US2) completes:
   - **Developer A**: Phase 5 (US3: Serialize)
   - **Developer B**: Phase 7 (US5: Compatibility)
   - **Developer C**: Phase 6 (US4: Update Rules)
5. After all user stories:
   - **Team**: Phase 10 (Quality) in parallel, then Phase 11 (Polish)

---

## Notes

- **[P] tasks**: Different files, no dependencies, can run in parallel
- **[Story] label**: Maps task to specific user story for traceability
- **Test tasks excluded**: Not requested in feature specification
- **Each user story is independently testable**: US1 can be validated without US2, etc.
- **Thread-safety critical**: Volatile reads, synchronized writes pattern throughout
- **Zero dependencies**: Only org.json from Android SDK, no external libraries
- **Performance targets**: <1ms permission checks, 1000+ concurrent ops, <10ms serialization
- **Package size limit**: <100KB AAR (verified in Phase 9, Task T086)
- **Feature parity goal**: Match iOS CASL library behavior (verified in Phase 10)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently

---

## Task Count Summary

- **Phase 1 (Setup)**: 8 tasks
- **Phase 2 (Foundational)**: 8 tasks
- **Phase 3 (US1 - Define Rules)**: 11 tasks
- **Phase 4 (US2 - Check Permissions)**: 18 tasks
- **Phase 5 (US3 - Serialize Rules)**: 9 tasks
- **Phase 6 (US4 - Update Rules)**: 7 tasks
- **Phase 7 (US5 - Java/Kotlin Compat)**: 10 tasks
- **Phase 8 (Sample App & Docs)**: 11 tasks
- **Phase 9 (Build & Distribution)**: 7 tasks
- **Phase 10 (Quality & Performance)**: 11 tasks
- **Phase 11 (Polish)**: 12 tasks

**Total: 112 tasks**

**MVP Scope (Phases 1-4)**: 45 tasks
**Full Feature Set (Phases 1-7)**: 71 tasks
**Production Ready (All Phases)**: 112 tasks
