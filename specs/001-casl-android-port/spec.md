# Feature Specification: CASL Android Authorization Library

**Feature Branch**: `001-casl-android-port`
**Created**: 2025-10-23
**Status**: Draft
**Input**: User description: "implement the android java version (with kotlin compatibility) of the casl-ios library in the ../casl-ios folder"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Define Authorization Rules for Application Resources (Priority: P1)

Android developers need to define authorization rules that control what actions users can perform on different types of resources in their application (e.g., read posts, update profiles, delete comments).

**Why this priority**: This is the core functionality of the library - without the ability to define rules, no authorization can occur. This forms the foundational API that all other features depend on.

**Independent Test**: Can be fully tested by creating an authorization rule set programmatically and verifying rules are stored correctly without needing to perform any permission checks. Delivers the essential API for defining access control policies.

**Acceptance Scenarios**:

1. **Given** a developer has imported the CASL library, **When** they create rules using a builder pattern (e.g., "users can read blog posts"), **Then** the rules are successfully stored in an authorization manager object
2. **Given** a developer defines multiple rules, **When** they add both positive permissions (can) and negative permissions (cannot), **Then** both rule types are stored with proper precedence ordering
3. **Given** a developer defines rules with conditions (e.g., "can update if authorId matches"), **When** they specify attribute-based conditions, **Then** the conditions are captured and associated with the rule
4. **Given** a developer defines field-level permissions (e.g., "can read post.title but not post.drafts"), **When** they specify field restrictions, **Then** field-level rules are stored separately from resource-level rules

---

### User Story 2 - Check User Permissions at Runtime (Priority: P1)

Application code needs to check whether a user has permission to perform a specific action on a resource before allowing the operation (e.g., checking if a user can delete a comment before showing the delete button).

**Why this priority**: Permission checking is the primary value delivery - without it, defined rules have no practical use. This enables developers to enforce authorization policies in their applications.

**Independent Test**: Can be fully tested by defining a simple rule set, creating test subjects (resource instances), and verifying that permission checks return correct true/false results. Delivers immediate authorization enforcement capability.

**Acceptance Scenarios**:

1. **Given** rules allow "read" on "BlogPost" resources, **When** the application checks if a user can read a blog post instance, **Then** the check returns true
2. **Given** rules prohibit "delete" on "BlogPost" resources, **When** the application checks if a user can delete a blog post instance, **Then** the check returns false
3. **Given** rules include conditions (e.g., "can update if authorId equals userId"), **When** the application checks permission on a resource matching the condition, **Then** the check evaluates the condition and returns the correct result
4. **Given** rules define field-level permissions, **When** the application checks if a user can access a specific field of a resource, **Then** the check returns true only if that specific field is permitted
5. **Given** multiple threads check permissions simultaneously, **When** concurrent permission checks occur, **Then** all checks complete correctly without race conditions or data corruption

---

### User Story 3 - Serialize and Deserialize Authorization Rules (Priority: P2)

Applications need to serialize authorization rules to JSON format for storage or transmission to clients, and deserialize rules from JSON to reconstruct authorization policies (enabling isomorphic authorization across client and server).

**Why this priority**: This enables dynamic rule updates and sharing authorization policies between backend and frontend. While important for production use, applications can function with hardcoded rules initially.

**Independent Test**: Can be fully tested by defining a rule set, exporting to JSON, creating a new authorization manager from the JSON, and verifying both managers produce identical permission check results. Delivers capability for dynamic authorization policies.

**Acceptance Scenarios**:

1. **Given** an authorization manager with defined rules, **When** the application exports rules to JSON, **Then** the JSON output contains all rules in a serializable format
2. **Given** JSON-formatted rules, **When** the application creates a new authorization manager from the JSON, **Then** the recreated manager behaves identically to the original
3. **Given** rules with complex conditions (nested objects, arrays), **When** the application serializes and deserializes these rules, **Then** all condition data is preserved accurately
4. **Given** rules with field-level permissions, **When** exported and imported, **Then** field restrictions are maintained correctly

---

### User Story 4 - Update Authorization Rules at Runtime (Priority: P2)

Running applications need to update authorization rules dynamically without restarting (e.g., when a user's role changes or new permissions are granted by an administrator).

**Why this priority**: This enables responsive authorization systems that reflect real-time permission changes. Applications can work with static rules initially, making this an enhancement rather than core functionality.

**Independent Test**: Can be fully tested by creating an authorization manager with initial rules, updating with new rules, and verifying that subsequent permission checks use the new rules exclusively. Delivers dynamic authorization capability.

**Acceptance Scenarios**:

1. **Given** an authorization manager with existing rules, **When** the application updates with a new rule set, **Then** subsequent permission checks use only the new rules
2. **Given** multiple threads checking permissions during a rule update, **When** rules are updated concurrently with checks, **Then** all operations complete safely without inconsistent results
3. **Given** rules in JSON format received from a remote source, **When** the application updates rules from the JSON, **Then** the new rules take effect immediately

---

### User Story 5 - Use Library from Both Java and Kotlin Code (Priority: P3)

Android developers using either Java or Kotlin need to integrate the authorization library with idiomatic API patterns for their chosen language (Java builders and Kotlin DSLs).

**Why this priority**: While important for developer experience, the library can function correctly with Java-only or Kotlin-only APIs initially. This priority focuses on broad compatibility and ergonomics after core functionality is proven.

**Independent Test**: Can be fully tested by writing identical authorization logic in both Java and Kotlin, verifying both compile successfully and produce equivalent runtime behavior. Delivers cross-language compatibility.

**Acceptance Scenarios**:

1. **Given** a Java developer, **When** they use the library with Java syntax (method chaining, builder pattern), **Then** they can define rules and check permissions without Kotlin-specific features
2. **Given** a Kotlin developer, **When** they use the library with Kotlin syntax (nullable types, extension functions, coroutines), **Then** they get idiomatic Kotlin API patterns
3. **Given** a mixed Java/Kotlin project, **When** both languages interact with the same authorization manager instance, **Then** all operations work correctly across language boundaries

---

### Edge Cases

- What happens when a rule set contains conflicting rules (e.g., "can read" and "cannot read" for the same resource)? The last rule defined takes precedence.
- How does the system handle permission checks on null or undefined resources? Permission checks return false for null subjects.
- What occurs when a condition references an attribute that doesn't exist on the subject? The condition fails (evaluates to false) and permission is denied.
- How are field-level permissions resolved when no field is specified in the check? The permission check applies to the entire resource, not specific fields.
- What happens when rules are updated while permission checks are in progress? Ongoing checks complete with the rule set they started with; new checks use the updated rules.
- How does the system behave when condition values contain special characters or complex nested structures? Conditions support standard JSON-serializable data types (strings, numbers, booleans, arrays, objects) with deep equality matching.
- What occurs if multiple threads attempt to update rules simultaneously? Updates are serialized internally to prevent corruption; the last complete update wins.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The library MUST provide a fluent builder API for defining authorization rules with positive permissions (can) and negative permissions (cannot)
- **FR-002**: The library MUST support defining rules for actions (verbs like "read", "create", "update", "delete") on subject types (resource types like "BlogPost", "Comment")
- **FR-003**: The library MUST support attribute-based conditions that match resource attributes (e.g., "authorId equals current user")
- **FR-004**: The library MUST support field-level permissions that restrict access to specific attributes of resources (e.g., "can read post.title but not post.salary")
- **FR-005**: The library MUST provide synchronous permission checking methods that return boolean results (can/cannot)
- **FR-006**: The library MUST provide asynchronous permission checking methods for use with coroutines
- **FR-007**: The library MUST implement thread-safe permission checking supporting concurrent operations from multiple threads
- **FR-008**: The library MUST enforce rule precedence where later-defined rules override earlier conflicting rules
- **FR-009**: The library MUST serialize authorization rules to JSON format preserving all rule data (actions, subjects, conditions, fields)
- **FR-010**: The library MUST deserialize authorization rules from JSON format, reconstructing equivalent authorization behavior
- **FR-011**: The library MUST support runtime rule updates that replace the entire rule set atomically
- **FR-012**: The library MUST detect subject types automatically from resource instances (class-based type detection)
- **FR-013**: The library MUST evaluate conditions using deep equality matching on nested object structures
- **FR-014**: The library MUST provide clear, type-safe APIs usable from Java code (no Kotlin-only language features in core API)
- **FR-015**: The library MUST provide idiomatic Kotlin extensions and DSLs for Kotlin developers
- **FR-016**: The library MUST operate without external dependencies (zero dependencies beyond Android SDK and Kotlin standard library)
- **FR-017**: The library MUST return false for permission checks on null subjects
- **FR-018**: The library MUST return false for permission checks when condition attributes don't exist on the subject
- **FR-019**: The library MUST maintain backward compatibility with Android API level 21 (Android 5.0) and above
- **FR-020**: The library MUST provide clear error messages when invalid rules are defined (e.g., empty action strings, null subjects)

### Key Entities

- **Authorization Manager (Ability)**: Represents the authorization engine that stores rules and evaluates permissions. Contains methods for checking permissions (can/cannot), updating rules, and exporting rules. Operates as a thread-safe actor/manager handling concurrent permission checks.

- **Authorization Rule**: Represents a single permission rule with an action (string), subject type (string or class reference), optional conditions (key-value attribute matches), optional field restrictions (list of field names), and rule type (allow/deny). Rules are immutable once created.

- **Rule Builder**: Provides fluent API for accumulating rules before creating an Authorization Manager. Supports method chaining for defining can/cannot rules with conditions and field specifications. Maintains a mutable list of rules during construction.

- **Raw Rule**: Serializable representation of a rule suitable for JSON encoding/decoding. Contains the same data as Authorization Rule but in primitive/collection types (strings, maps, lists) rather than typed objects.

- **Subject**: Any resource or entity being authorized (e.g., a BlogPost instance, User object, Document). Subjects have a type (detected from class) and attributes (fields/properties) that can be matched by conditions.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Developers can define authorization rules and perform permission checks within 10 lines of code for basic scenarios
- **SC-002**: Permission checks complete in under 1 millisecond for rule sets containing up to 100 rules
- **SC-003**: The library supports at least 1000 concurrent permission checks without performance degradation
- **SC-004**: Rule serialization and deserialization completes in under 10 milliseconds for rule sets containing 100 rules
- **SC-005**: The library works correctly on Android devices running API level 21 and above (covering 99%+ of active Android devices)
- **SC-006**: Developers can successfully integrate the library into both pure Java projects and pure Kotlin projects without compilation errors
- **SC-007**: Authorization logic written with this library matches the behavior of the iOS CASL library for equivalent rule sets (feature parity)
- **SC-008**: The library package size is under 100 KB (compiled AAR)
- **SC-009**: Zero crashes or data corruption occur during concurrent rule updates and permission checks under stress testing (10,000 operations)
- **SC-010**: API documentation coverage reaches 100% of public methods and classes
