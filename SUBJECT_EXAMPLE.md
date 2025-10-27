# Using the `subject()` Helper Function

The `subject()` helper function allows you to create permission subjects with explicit types and attributes, just like in casl-js.

## Basic Usage

### Kotlin

```kotlin
import com.michaelsiddi.casl.subject
import com.michaelsiddi.casl.Ability
import com.michaelsiddi.casl.RawRule

// Create a subject with type and attributes
val user = subject("User",
    "id" to 123,
    "role" to "admin"
)

// Check permissions
val ability = Ability.builder()
    .can("read", "User")
    .build()

if (ability.can("read", user)) {
    println("Can read user!")
}
```

### Java

```java
import com.michaelsiddi.casl.SubjectKt;
import com.michaelsiddi.casl.Ability;
import com.michaelsiddi.casl.ForcedSubject;

// Create attributes map
Map<String, Object> attrs = new HashMap<>();
attrs.put("id", 123);
attrs.put("role", "admin");

// Create subject
ForcedSubject user = SubjectKt.subject("User", attrs);

// Check permissions
Ability ability = Ability.builder()
    .can("read", "User")
    .build();

if (ability.can("read", user)) {
    System.out.println("Can read user!");
}
```

## Your Use Case - ChannelOptionField

Based on your example:

```kotlin
// In JavaScript you had:
// return this.can(option, subject('ChannelOptionField', { ...this.me.user }))

// In Kotlin, you can do:
fun canPerformOption(option: String, user: User): Boolean {
    val channelOptionField = subject("ChannelOptionField",
        "id" to user.id,
        "name" to user.name,
        "role" to user.role
        // ... spread all user properties you need
    )

    return ability.can(option, channelOptionField)
}

// Or more concisely with a helper:
fun canPerformOption(option: String, user: User): Boolean {
    return ability.can(option, subject("ChannelOptionField",
        "userId" to user.id,
        "role" to user.role
    ))
}
```

## Conditional Permissions

You can check permissions with conditions on subject attributes:

```kotlin
// Define rules with conditions
val rules = listOf(
    RawRule(
        action = listOf("read", "update"),
        subject = "ChannelOptionField",
        conditions = mapOf("userId" to currentUserId)
    )
)

val ability = Ability.fromRules(rules)

// Check permission on a specific channel option
val channelOption = subject("ChannelOptionField",
    "id" to 1,
    "userId" to 123,  // This must match the condition
    "value" to "some value"
)

// Will return true if userId matches
ability.can("read", channelOption)  // true if userId == 123
ability.can("update", channelOption) // true if userId == 123
```

## Complex Nested Conditions

```kotlin
// Rules with nested conditions
val rules = listOf(
    RawRule(
        action = "delete",
        subject = "Post",
        conditions = mapOf(
            "author" to mapOf("id" to currentUserId)
        )
    )
)

val ability = Ability.fromRules(rules)

// Check if user can delete their own post
val post = subject("Post",
    "id" to 1,
    "title" to "My Post",
    "author" to mapOf(
        "id" to currentUserId,
        "name" to "John"
    )
)

ability.can("delete", post)  // true - user can delete their own post
```

## API Reference

### `subject(type: String, attributes: Map<String, Any?>): ForcedSubject`
Creates a subject with explicit type and attributes.

### `subject(type: String, vararg attributes: Pair<String, Any?>): ForcedSubject`
Kotlin convenience function using vararg pairs.

### `ForcedSubject` Methods
- `get(key: String): Any?` - Get an attribute value
- `has(key: String): Boolean` - Check if attribute exists
- `toMap(): Map<String, Any?>` - Get all attributes as a map

## Key Differences from casl-js

1. **Return Type**: Returns `ForcedSubject` instead of mutating the object
2. **Kotlin Convenience**: Supports vararg pairs for cleaner syntax
3. **Type Safety**: Provides better IDE support and compile-time checking

## When to Use `subject()`

Use `subject()` when:
- You don't have a corresponding Kotlin/Java class for the subject
- You want to override the detected type
- You need to pass dynamic attributes for permission checks
- You're working with data that comes from external sources (API, database)

Use regular objects when:
- You have a defined Kotlin data class or Java POJO
- The class name matches the subject type you want to use
- You don't need to override type detection
