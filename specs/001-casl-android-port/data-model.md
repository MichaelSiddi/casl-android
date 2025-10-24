# Data Model: CASL Android Authorization Library

**Feature**: 001-casl-android-port
**Date**: 2025-10-23
**Purpose**: Define entities, relationships, and state transitions for the authorization library

## Entity Overview

This is an authorization library with in-memory data structures only (no persistence). Entities represent authorization concepts rather than database models.

```text
┌─────────────┐
│   Ability   │ (Authorization Manager)
└──────┬──────┘
       │ owns
       ↓
┌─────────────┐
│  RuleIndex  │ (Rule Storage & Lookup)
└──────┬──────┘
       │ contains
       ↓
┌─────────────┐         ┌─────────────┐
│    Rule     │────────→│  RawRule    │ (Serializable)
└──────┬──────┘ converts└─────────────┘
       │ evaluates
       ↓
┌─────────────┐
│   Subject   │ (Any user-defined type)
└─────────────┘
```

---

## Core Entities

### 1. Ability (Authorization Manager)

**Purpose**: Main entry point for authorization operations. Manages rule storage and evaluates permissions.

**Attributes**:
- `ruleIndex: RuleIndex` - Immutable snapshot of current rules (volatile for thread visibility)

**Behavior**:
- `can(action, subject, field?): Boolean` - Check if action is permitted (synchronous)
- `cannot(action, subject, field?): Boolean` - Check if action is prohibited (synchronous)
- `canAsync(action, subject, field?): Boolean` - Async permission check (suspend function)
- `cannotAsync(action, subject, field?): Boolean` - Async prohibition check (suspend function)
- `update(rawRules: List<RawRule>)` - Atomically replace all rules (synchronized)
- `exportRules(): List<RawRule>` - Export current rules for serialization

**Invariants**:
- `ruleIndex` is never null (uses empty index initially)
- Permission checks on null subjects always return false
- Updates are atomic (no partial rule sets visible)

**Thread Safety**: Reads use volatile snapshot, writes use synchronized block

---

### 2. Rule (Internal Authorization Rule)

**Purpose**: Internal representation of a single authorization rule. Immutable once created.

**Attributes**:
- `action: String` - The action being authorized (e.g., "read", "update", "delete")
- `subjectType: String` - Type of resource (e.g., "BlogPost", "Comment")
- `conditions: Map<String, Any?>?` - Optional attribute matchers for conditional rules
- `fields: Set<String>?` - Optional field restrictions for field-level permissions
- `inverted: Boolean` - True for "cannot" rules, false for "can" rules

**Behavior**:
- `matches(subject: Any?): Boolean` - Check if this rule applies to the given subject
- `matchesConditions(subject: Any?): Boolean` - Evaluate condition predicates against subject
- `matchesField(field: String?): Boolean` - Check if field is covered by this rule
- `toRawRule(): RawRule` - Convert to serializable format

**Validation Rules**:
- `action` must not be empty (enforced at construction)
- `subjectType` must not be empty (enforced at construction)
- `conditions` keys must correspond to subject attributes
- `fields` must contain valid field names (no validation of subject schema)

**Examples**:
```kotlin
// Simple rule: anyone can read blog posts
Rule(
    action = "read",
    subjectType = "BlogPost",
    conditions = null,
    fields = null,
    inverted = false
)

// Conditional rule: can update own posts
Rule(
    action = "update",
    subjectType = "BlogPost",
    conditions = mapOf("authorId" to currentUserId),
    fields = null,
    inverted = false
)

// Field-level rule: can read title but not salary
Rule(
    action = "read",
    subjectType = "Employee",
    conditions = null,
    fields = setOf("title"),
    inverted = false
)
```

---

### 3. RawRule (Serializable Rule)

**Purpose**: JSON-serializable representation of a rule for export/import and isomorphic authorization.

**Attributes**:
- `action: String` - Same as Rule
- `subject: String` - Same as Rule.subjectType (naming matches iOS CASL)
- `conditions: Map<String, Any?>?` - Same as Rule
- `fields: List<String>?` - List instead of Set for JSON compatibility
- `inverted: Boolean` - Same as Rule

**Behavior**:
- `toJson(): JSONObject` - Serialize to org.json.JSONObject
- `fromJson(json: JSONObject): RawRule` - Deserialize from JSONObject (companion function)
- `toRule(): Rule` - Convert to internal Rule representation

**JSON Schema**:
```json
{
  "action": "string (required)",
  "subject": "string (required)",
  "conditions": {
    "key1": "value1",
    "key2": { "nested": "value" }
  },
  "fields": ["field1", "field2"],
  "inverted": false
}
```

**Validation Rules**:
- Same validation as Rule (enforced during conversion to Rule)
- JSON deserialization handles type coercion (Int/Long/Double normalized)

---

### 4. RuleIndex (Rule Storage & Lookup)

**Purpose**: Efficient storage and retrieval of rules optimized for permission checks.

**Attributes**:
- `resourceRules: Map<String, List<Rule>>` - Index by "action:subjectType" key
- `fieldRules: Map<String, Map<String, List<Rule>>>` - Index by field then "action:subjectType"

**Behavior**:
- `findMatchingRule(action, subject, field?): Rule?` - Find last matching rule
- `fromRawRules(rawRules: List<RawRule>): RuleIndex` - Build index from raw rules (companion function)
- `empty(): RuleIndex` - Create empty index (companion function)

**Indexing Strategy**:
- Rules without field restrictions go into `resourceRules`
- Rules with field restrictions go into `fieldRules`
- Lookup order: field-specific rules first (if field provided), then resource rules
- Within each index, rules stored in insertion order for precedence evaluation

**Performance Characteristics**:
- Lookup: O(1) hash map access + O(n) condition evaluation where n = matching rules (typically <10)
- Update: O(m) where m = total rules (rebuild entire index)
- Memory: O(m) space proportional to rule count

---

### 5. Subject (User-Defined Type)

**Purpose**: Any application domain object being authorized (e.g., BlogPost, User, Document).

**Attributes**: Varies by application domain (library does not define)

**Requirements**:
- Must be a Kotlin/Java object with accessible properties
- Properties used in conditions must be readable via reflection or public getters
- Type detection uses class simple name (e.g., BlogPost, User)

**Type Detection Strategy**:
- String subjects treated as type literals: `ability.can("read", "BlogPost")`
- Object subjects use class name: `ability.can("read", blogPostInstance)` → type = "BlogPost"

**Field Extraction Strategy**:
- Kotlin data classes: Use reflection to access properties
- Java POJOs: Use reflection to access getters
- Maps: Treat keys as field names
- Fallback: Reflection on public fields

**Example Subjects**:
```kotlin
data class BlogPost(
    val id: String,
    val title: String,
    val authorId: String,
    val published: Boolean
)

data class User(
    val id: String,
    val role: String,
    val organizationId: String
)
```

---

## Relationships

### Ability → RuleIndex (1:1 ownership)
- Ability owns a single RuleIndex instance at any time
- RuleIndex is immutable (replaced on update, not modified)
- Volatile reference ensures visibility across threads

### RuleIndex → Rule (1:N containment)
- RuleIndex contains multiple Rules organized by indexes
- Rules never modified after index construction
- Index built during Ability.update() operation

### Rule ↔ RawRule (bidirectional conversion)
- Rule.toRawRule() for serialization
- RawRule.toRule() for deserialization
- Lossless conversion (round-trip preserves semantics)

### Rule → Subject (N:1 evaluation)
- Rules evaluate against Subjects during permission checks
- Subject provides data for condition matching
- Multiple rules may match same subject (precedence determines outcome)

---

## State Transitions

### Ability Lifecycle

```text
┌─────────────┐
│   Created   │ (empty rule index)
└──────┬──────┘
       │ builder.can/cannot calls
       ↓
┌─────────────┐
│   Building  │ (accumulating rules)
└──────┬──────┘
       │ builder.build()
       ↓
┌─────────────┐
│   Active    │ (can check permissions)
└──────┬──────┘
       │ update(rules)
       ↓
┌─────────────┐
│   Updated   │ (new rule index)
└─────────────┘
       ↓
  (cycles back to Active)
```

### Rule Evaluation Flow

```text
┌─────────────────────┐
│ Permission Check    │
│ can(action, subject)│
└──────────┬──────────┘
           ↓
     ┌─────────────┐
     │ Subject =   │ NO
     │ null?       ├──────→ Continue
     └──────┬──────┘
            │ YES
            ↓
      Return FALSE


     ┌─────────────┐
     │ Detect      │
     │ subject type│
     └──────┬──────┘
            ↓
     ┌─────────────┐
     │ Lookup rules│
     │ by action+  │
     │ type        │
     └──────┬──────┘
            ↓
     ┌─────────────┐
     │ Evaluate    │ NO match
     │ conditions  ├──────────→ Next rule
     │ (last→first)│
     └──────┬──────┘
            │ MATCH
            ↓
     ┌─────────────┐
     │ Check if    │ inverted=true
     │ inverted    ├──────────────→ Return FALSE
     └──────┬──────┘
            │ inverted=false
            ↓
       Return TRUE


     (If no rules match)
            ↓
       Return FALSE (default deny)
```

### Rule Update Flow

```text
┌─────────────────────┐
│ Ability.update()    │
│ called              │
└──────────┬──────────┘
           ↓
     ┌─────────────┐
     │ Acquire     │
     │ lock        │
     │ (synchronized)
     └──────┬──────┘
            ↓
     ┌─────────────┐
     │ Convert     │
     │ RawRule →   │
     │ Rule        │
     └──────┬──────┘
            ↓
     ┌─────────────┐
     │ Build new   │
     │ RuleIndex   │
     └──────┬──────┘
            ↓
     ┌─────────────┐
     │ Atomically  │
     │ replace     │
     │ ruleIndex   │
     │ (volatile)  │
     └──────┬──────┘
            ↓
     ┌─────────────┐
     │ Release     │
     │ lock        │
     └──────┬──────┘
            ↓
  ┌──────────────────────┐
  │ Old RuleIndex        │
  │ remains visible to   │
  │ in-flight checks     │
  └──────────────────────┘
            ↓
  (garbage collected when
   all readers complete)
```

---

## Data Validation

### Rule Construction Validation
- **action**: Must not be blank (throw IllegalArgumentException)
- **subjectType**: Must not be blank (throw IllegalArgumentException)
- **conditions**: Keys must be valid identifiers (no runtime validation of subject schema)
- **fields**: Must not be empty set if provided (throw IllegalArgumentException)

### Condition Evaluation Validation
- Missing subject attributes → condition fails (returns false, no exception)
- Type mismatches → condition fails unless numeric coercion applies
- Null condition values → exact null match required

### Serialization Validation
- JSON parsing errors → throw JsonParseException
- Missing required fields (action, subject) → throw IllegalStateException
- Invalid condition types → throw IllegalArgumentException

---

## Memory Management

### Immutable Snapshots
- Each rule update creates new RuleIndex instance
- Old RuleIndex kept alive by in-flight permission checks
- Garbage collected when all references released
- No manual memory management required

### Memory Overhead Estimation
- Rule: ~200 bytes (object header + strings + collections)
- RuleIndex: ~100 bytes + (rules × 200 bytes)
- Ability: ~50 bytes + RuleIndex
- **Total for 100 rules**: ~20KB (well under 100KB package limit)

---

## Concurrency Model

### Read Operations (Permission Checks)
- Acquire volatile snapshot of ruleIndex
- No locking during evaluation
- Multiple readers operate on same immutable snapshot
- Lock-free reads enable high concurrency (SC-003: 1000+ concurrent checks)

### Write Operations (Rule Updates)
- Synchronized on Ability instance
- Serializes all concurrent update attempts
- Last complete update wins
- Updates complete in <10ms (SC-004) due to in-memory operations

### Happens-Before Guarantees
- Volatile read of ruleIndex happens-before any permission check
- Synchronized write happens-before volatile write
- Volatile write happens-before subsequent volatile reads
- Ensures all threads see consistent rule state post-update

---

## Feature Parity with iOS CASL

| iOS Entity | Android Equivalent | Notes |
|------------|-------------------|-------|
| `Ability` (actor) | `Ability` (synchronized) | Actor semantics via volatile + synchronized |
| `AbilityBuilder` | `AbilityBuilder` | Identical fluent API |
| `Rule` | `Rule` | Same attributes, immutable |
| `RawRule` | `RawRule` | JSON-compatible, lossless conversion |
| `RuleIndex` | `RuleIndex` | Similar indexing strategy |
| Subject type detection | `SubjectTypeDetector` | Uses Kotlin reflection vs Swift metatypes |
| Condition matching | `ConditionMatcher` | Deep equality with numeric coercion |

**Key Difference**: iOS uses Swift actors for concurrency, Android uses volatile + synchronized (JVM lacks first-class actors).

---

## Summary

Data model consists of five core entities:
1. **Ability**: Authorization manager (thread-safe, mutable via updates)
2. **Rule**: Internal rule representation (immutable)
3. **RawRule**: Serializable rule format (JSON-compatible)
4. **RuleIndex**: Optimized rule storage (immutable, rebuilt on update)
5. **Subject**: User-defined domain objects (library does not define)

All entities designed for:
- Thread safety via immutability + volatile/synchronized
- Performance via indexing and lock-free reads
- Compatibility via clear Java/Kotlin interop patterns
- Feature parity with iOS CASL library

No external data stores required - entire authorization state lives in memory.
