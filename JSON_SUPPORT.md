# JSONObject Support in CASL Android

CASL Android v2.2.0+ includes native support for Android's `JSONObject` and `JSONArray`, making it seamless to check permissions against JSON data from APIs, databases, or other sources.

## Quick Start

```kotlin
import org.json.JSONObject

// Your ability rules
val ability = Ability.builder()
    .can("update", "Post", conditions = mapOf("author.id" to currentUserId))
    .build()

// JSON from your API
val postJson = JSONObject("""
    {
        "id": 1,
        "author": {"id": 123}
    }
""")

// Direct permission check - no conversion needed!
val canUpdate = ability.can("update", "Post", postJson)  // true if author.id matches
```

## Features

### Automatic Conversion
CASL automatically converts JSONObject to the internal Map structure used for condition matching. You don't need to manually convert your JSON data.

### Deep Nesting Support
CASL handles deeply nested JSON structures using dot notation:

```kotlin
val ability = Ability.builder()
    .can("read", "Post", conditions = mapOf(
        "metadata.author.permissions.canEdit" to true
    ))
    .build()

val postJson = JSONObject("""
    {
        "metadata": {
            "author": {
                "permissions": {
                    "canEdit": true
                }
            }
        }
    }
""")

ability.can("read", "Post", postJson)  // true
```

### Array Support
Access array elements using numeric indices:

```kotlin
val ability = Ability.builder()
    .can("read", "Post", conditions = mapOf("tags.0" to "featured"))
    .build()

val postJson = JSONObject("""
    {
        "tags": ["featured", "tech", "news"]
    }
""")

ability.can("read", "Post", postJson)  // true - first tag is "featured"
```

### MongoDB Operators
Use MongoDB-style operators in conditions:

```kotlin
val ability = Ability.builder()
    .can("read", "Post", conditions = mapOf(
        "author.role" to mapOf("\$in" to listOf("admin", "editor"))
    ))
    .build()

val postJson = JSONObject("""{"author": {"role": "admin"}}""")

ability.can("read", "Post", postJson)  // true - admin is in the list
```

### Field-Level Permissions
Check permissions for specific fields:

```kotlin
val ability = Ability.builder()
    .can("update", "Post",
        conditions = mapOf("author.id" to 123),
        fields = listOf("title", "content")
    )
    .build()

val postJson = JSONObject("""{"author": {"id": 123}}""")

ability.can("update", "Post", postJson, "title")    // true
ability.can("update", "Post", postJson, "status")   // false - not in fields list
```

## API Reference

### Ability.can() with JSONObject

```kotlin
fun can(
    action: String,
    subjectType: String,
    json: JSONObject,
    field: String? = null
): Boolean
```

**Parameters:**
- `action`: The action to check (e.g., "read", "update", "delete")
- `subjectType`: The subject type name (e.g., "Post", "User", "Comment")
- `json`: The JSONObject containing the subject's attributes
- `field`: (Optional) Specific field name for field-level permission checks

**Returns:** `true` if permitted, `false` otherwise

**Example:**
```kotlin
val canUpdate = ability.can("update", "Post", postJson)
val canUpdateTitle = ability.can("update", "Post", postJson, "title")
```

### Ability.cannot() with JSONObject

```kotlin
fun cannot(
    action: String,
    subjectType: String,
    json: JSONObject,
    field: String? = null
): Boolean
```

**Parameters:** Same as `can()`

**Returns:** `true` if prohibited, `false` otherwise (opposite of `can()`)

**Example:**
```kotlin
val cannotDelete = ability.cannot("delete", "Post", postJson)
```

## Utility Functions

### jsonObjectToMap()

Convert a JSONObject to a nested Map structure:

```kotlin
fun jsonObjectToMap(json: JSONObject): Map<String, Any?>
```

**Example:**
```kotlin
val json = JSONObject("""
    {
        "id": 123,
        "author": {"name": "John"}
    }
""")

val map = jsonObjectToMap(json)
// Result: mapOf("id" to 123, "author" to mapOf("name" to "John"))
```

### jsonArrayToList()

Convert a JSONArray to a List:

```kotlin
fun jsonArrayToList(jsonArray: JSONArray): List<Any?>
```

**Example:**
```kotlin
val jsonArray = JSONArray("""[1, 2, 3]""")
val list = jsonArrayToList(jsonArray)
// Result: listOf(1, 2, 3)
```

### subjectFromJson()

Create a ForcedSubject from a JSONObject:

```kotlin
fun subjectFromJson(type: String, json: JSONObject): ForcedSubject
```

**Example:**
```kotlin
val postJson = JSONObject("""{"id": 1, "author": {"id": 123}}""")
val post = subjectFromJson("Post", postJson)

ability.can("update", post)  // Works with ForcedSubject
```

## Real-World Examples

### Example 1: API Response

```kotlin
class PostRepository(private val ability: Ability) {

    suspend fun getPost(id: Int): JSONObject {
        // Fetch from API
        val response = apiClient.get("/posts/$id")
        return JSONObject(response.body)
    }

    suspend fun canUserUpdatePost(postId: Int): Boolean {
        val postJson = getPost(postId)
        return ability.can("update", "Post", postJson)
    }
}
```

### Example 2: User-Based Permissions

```kotlin
// Build ability based on current user
fun buildUserAbility(currentUser: JSONObject): Ability {
    val userId = currentUser.getInt("id")
    val role = currentUser.getString("role")

    return Ability.builder()
        // Own posts
        .can("update", "Post", conditions = mapOf("author.id" to userId))
        .can("delete", "Post", conditions = mapOf("author.id" to userId))

        // Admin can do anything
        .apply {
            if (role == "admin") {
                can("manage", "all")
            }
        }
        .build()
}

// Usage
val currentUser = JSONObject("""{"id": 123, "role": "user"}""")
val ability = buildUserAbility(currentUser)

val postJson = JSONObject("""{"author": {"id": 123}}""")
ability.can("update", "Post", postJson)  // true - user owns the post
```

### Example 3: Complex Conditions

```kotlin
val ability = Ability.builder()
    .can("update", "Post", conditions = mapOf(
        "author.id" to currentUserId,
        "status" to "draft",
        "metadata.locked" to false
    ))
    .build()

// API response with complex structure
val postJson = JSONObject(apiResponse)

if (ability.can("update", "Post", postJson)) {
    // Allow editing
} else {
    // Show error or read-only view
}
```

### Example 4: Android Activity

```kotlin
class PostDetailActivity : AppCompatActivity() {

    private lateinit var ability: Ability
    private var postJson: JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ability from user session
        val currentUserId = SessionManager.getCurrentUserId()
        ability = Ability.builder()
            .can("read", "Post")
            .can("update", "Post", conditions = mapOf("author.id" to currentUserId))
            .can("delete", "Post", conditions = mapOf("author.id" to currentUserId))
            .build()

        // Load post from API
        loadPost()
    }

    private fun loadPost() {
        lifecycleScope.launch {
            postJson = apiClient.getPost(postId)
            updateUI()
        }
    }

    private fun updateUI() {
        postJson?.let { json ->
            // Show/hide UI elements based on permissions
            btnEdit.isVisible = ability.can("update", "Post", json)
            btnDelete.isVisible = ability.can("delete", "Post", json)

            // Enable/disable field editing
            etTitle.isEnabled = ability.can("update", "Post", json, "title")
            etContent.isEnabled = ability.can("update", "Post", json, "content")
        }
    }
}
```

## Java Support

JSONObject support works seamlessly with Java:

```java
// Build ability
Ability ability = Ability.builder()
    .can("update", "Post", Map.of("author.id", currentUserId))
    .build();

// JSON from API
JSONObject postJson = new JSONObject(apiResponse);

// Check permission
boolean canUpdate = ability.can("update", "Post", postJson);

if (canUpdate) {
    // Allow editing
}
```

## Best Practices

### 1. Cache Ability Instances

Build ability once per user session and reuse it:

```kotlin
class UserSession {
    private val ability: Ability by lazy {
        Ability.builder()
            .can("update", "Post", conditions = mapOf("author.id" to userId))
            // ... other rules
            .build()
    }

    fun canUpdate(postJson: JSONObject) = ability.can("update", "Post", postJson)
}
```

### 2. Handle JSON Parse Errors

```kotlin
fun checkPermission(jsonString: String): Boolean {
    return try {
        val json = JSONObject(jsonString)
        ability.can("update", "Post", json)
    } catch (e: JSONException) {
        // Invalid JSON - deny by default
        false
    }
}
```

### 3. Use Type-Safe Wrappers

```kotlin
data class Post(
    val id: Int,
    val authorId: Int,
    val status: String
) {
    companion object {
        fun fromJson(json: JSONObject) = Post(
            id = json.getInt("id"),
            authorId = json.getJSONObject("author").getInt("id"),
            status = json.getString("status")
        )
    }

    fun toJson() = JSONObject().apply {
        put("id", id)
        put("author", JSONObject().put("id", authorId))
        put("status", status)
    }
}

// Usage
val post = Post.fromJson(apiResponse)
val canUpdate = ability.can("update", "Post", post.toJson())
```

### 4. Test with Sample Data

```kotlin
@Test
fun `user can update own posts`() {
    val ability = buildUserAbility(userId = 123)

    val myPost = JSONObject("""
        {"author": {"id": 123}}
    """)

    val otherPost = JSONObject("""
        {"author": {"id": 456}}
    """)

    assertTrue(ability.can("update", "Post", myPost))
    assertFalse(ability.can("update", "Post", otherPost))
}
```

## Performance

- **Conversion Overhead**: Converting JSONObject to Map is a one-time cost per permission check
- **Typical Performance**: <1ms for simple JSON, <5ms for complex nested structures
- **Recommendation**: Cache parsed subjects if checking multiple permissions on the same object

```kotlin
// Good: Parse once, check multiple times
val subject = subjectFromJson("Post", postJson)
val canRead = ability.can("read", subject)
val canUpdate = ability.can("update", subject)
val canDelete = ability.can("delete", subject)

// Less efficient: Parse multiple times
val canRead = ability.can("read", "Post", postJson)
val canUpdate = ability.can("update", "Post", postJson)
val canDelete = ability.can("delete", "Post", postJson)
```

## Troubleshooting

### JSONObject Not Found

Make sure you're using Android's `org.json.JSONObject`:

```kotlin
import org.json.JSONObject  // ✅ Correct
import com.google.gson.JsonObject  // ❌ Wrong - use GSON's JsonObject differently
```

### Null Values

JSONObject.NULL is automatically converted to Kotlin null:

```kotlin
val json = JSONObject()
json.put("deletedAt", JSONObject.NULL)

val map = jsonObjectToMap(json)
map["deletedAt"]  // null (Kotlin null, not JSONObject.NULL)
```

### Type Coercion

CASL handles numeric type coercion automatically:

```kotlin
// Rule expects Int
val ability = Ability.builder()
    .can("read", "Post", conditions = mapOf("id" to 123))
    .build()

// JSON has Double
val json = JSONObject("""{"id": 123.0}""")

ability.can("read", "Post", json)  // true - 123.0 equals 123
```

## Migration from Map-Based Approach

### Before (Manual Conversion)

```kotlin
fun checkPermission(jsonString: String): Boolean {
    val json = JSONObject(jsonString)
    val map = manuallyConvertToMap(json)
    val subject = subject("Post", map)
    return ability.can("update", subject)
}
```

### After (Direct JSONObject)

```kotlin
fun checkPermission(jsonString: String): Boolean {
    val json = JSONObject(jsonString)
    return ability.can("update", "Post", json)
}
```

## See Also

- [Main README](README.md) - Getting started with CASL Android
- [CHANGELOG](CHANGELOG.md) - Version history and changes
- [Test Examples](casl/src/test/kotlin/com/michaelsiddi/casl/JsonSupportTest.kt) - Comprehensive test suite showing all features
