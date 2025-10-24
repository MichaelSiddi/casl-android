# Quick Start Guide: CASL Android Authorization Library

**Feature**: 001-casl-android-port
**Date**: 2025-10-23
**Target Audience**: Android developers integrating authorization into their apps

## Installation

### Gradle (Kotlin DSL)

Add to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.casl:casl-android:1.0.0")
}
```

### Gradle (Groovy)

Add to your app's `build.gradle`:

```groovy
dependencies {
    implementation 'com.casl:casl-android:1.0.0'
}
```

**Requirements**:
- Android API 21+ (Android 5.0 Lollipop)
- Kotlin 1.9+ (if using Kotlin)
- Java 11+ (if using Java)

---

## 5-Minute Tutorial

### Step 1: Define Your Rules

**Kotlin:**
```kotlin
import com.casl.Ability

val ability = Ability.builder()
    .can("read", "BlogPost")
    .can("create", "BlogPost")
    .can("update", "BlogPost", conditions = mapOf("authorId" to currentUserId))
    .cannot("delete", "BlogPost", conditions = mapOf("published" to true))
    .build()
```

**Java:**
```java
import com.casl.Ability;
import java.util.Map;

Ability ability = Ability.builder()
    .can("read", "BlogPost")
    .can("create", "BlogPost")
    .can("update", "BlogPost", Map.of("authorId", currentUserId), null)
    .cannot("delete", "BlogPost", Map.of("published", true), null)
    .build();
```

### Step 2: Check Permissions

**Kotlin:**
```kotlin
// Check permission on type
if (ability.can("read", "BlogPost")) {
    // User can read blog posts
}

// Check permission on instance with conditions
val blogPost = BlogPost(
    id = "123",
    title = "My Post",
    authorId = currentUserId,
    published = false
)

if (ability.can("update", blogPost)) {
    // User can update this specific post (because authorId matches)
}

if (ability.cannot("delete", blogPost)) {
    // Deletion is not allowed (but post is not published, so actually allowed)
}
```

**Java:**
```java
// Check permission on type
if (ability.can("read", "BlogPost")) {
    // User can read blog posts
}

// Check permission on instance
BlogPost blogPost = new BlogPost("123", "My Post", currentUserId, false);

if (ability.can("update", blogPost)) {
    // User can update this specific post
}
```

### Step 3: Use in Your App

**Example: Hide UI elements based on permissions**

```kotlin
class BlogPostActivity : AppCompatActivity() {
    private lateinit var ability: Ability

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ability = createAbilityForCurrentUser()

        val blogPost = loadBlogPost()

        // Show/hide buttons based on permissions
        binding.editButton.isVisible = ability.can("update", blogPost)
        binding.deleteButton.isVisible = ability.can("delete", blogPost)
    }
}
```

---

## Common Patterns

### Pattern 1: Role-Based Access Control (RBAC)

Define abilities based on user roles:

```kotlin
fun createAbility(user: User): Ability = when (user.role) {
    "admin" -> Ability.builder()
        .can("manage", "all") // Admin can do everything
        .build()

    "editor" -> Ability.builder()
        .can("read", "BlogPost")
        .can("create", "BlogPost")
        .can("update", "BlogPost")
        .can("delete", "BlogPost", conditions = mapOf("authorId" to user.id))
        .build()

    "viewer" -> Ability.builder()
        .can("read", "BlogPost")
        .build()

    else -> Ability.builder().build() // No permissions
}
```

### Pattern 2: Field-Level Permissions

Restrict access to specific fields:

```kotlin
val ability = Ability.builder()
    // Everyone can read basic fields
    .can("read", "User", fields = listOf("name", "email", "avatar"))
    // Only owner can read sensitive fields
    .can("read", "User",
        conditions = mapOf("id" to currentUserId),
        fields = listOf("phoneNumber", "address", "creditCard")
    )
    .build()

// Check field-level permission
if (ability.can("read", user, field = "creditCard")) {
    // Show credit card field
}
```

### Pattern 3: Dynamic Rules from Backend

Load rules from your backend API:

```kotlin
class AuthorizationRepository {
    suspend fun loadRulesForUser(userId: String): Ability {
        // Fetch rules from API
        val rulesJson = api.getUserPermissions(userId)
        val rawRules = RawRule.listFromJson(rulesJson)

        return Ability.fromRules(rawRules)
    }
}

// In your ViewModel
class MainViewModel : ViewModel() {
    private val repository = AuthorizationRepository()

    val ability: LiveData<Ability> = liveData {
        val rules = repository.loadRulesForUser(currentUserId)
        emit(rules)
    }
}
```

### Pattern 4: Isomorphic Authorization (Client + Server)

Share rules between Android app and backend:

**Backend (Node.js/TypeScript):**
```typescript
// Backend defines rules
const ability = defineAbility((can, cannot) => {
  can('read', 'BlogPost');
  can('update', 'BlogPost', { authorId: userId });
  cannot('delete', 'BlogPost', { published: true });
});

// Send rules to client
res.json(ability.rules);
```

**Android Client:**
```kotlin
// Client receives and uses same rules
suspend fun syncRules() {
    val rulesJson = api.getRules()
    val rawRules = RawRule.listFromJson(rulesJson)
    ability.update(rawRules)
}
```

### Pattern 5: Async Permission Checks with Coroutines

```kotlin
class BlogPostRepository {
    private val ability: Ability = ...

    suspend fun updatePost(post: BlogPost): Result<BlogPost> {
        // Check permission asynchronously
        if (!ability.canAsync("update", post)) {
            return Result.failure(UnauthorizedException())
        }

        // Perform update
        return withContext(Dispatchers.IO) {
            api.updateBlogPost(post)
        }
    }
}
```

---

## Advanced Usage

### Kotlin DSL (Optional)

For more idiomatic Kotlin, use the DSL extensions:

```kotlin
import com.casl.extensions.*

val ability = buildAbility {
    can("read", "BlogPost")

    can("update", "BlogPost") {
        "authorId" to currentUserId
    }

    cannot("delete", "BlogPost") {
        "published" to true
    }
}
```

### Custom Subject Types

Use your domain models directly:

```kotlin
data class BlogPost(
    val id: String,
    val title: String,
    val authorId: String,
    val published: Boolean
)

// CASL automatically detects type as "BlogPost"
val ability = Ability.builder()
    .can("read", "BlogPost")
    .build()

val post = BlogPost(...)
ability.can("read", post) // true
```

### Complex Conditions

Conditions support nested structures:

```kotlin
val ability = Ability.builder()
    .can("read", "Document", conditions = mapOf(
        "organization" to mapOf(
            "id" to currentOrgId
        ),
        "visibility" to "public"
    ))
    .build()
```

### Updating Rules at Runtime

```kotlin
class PermissionManager {
    private var ability: Ability = Ability.builder().build()

    suspend fun refreshPermissions() {
        val newRules = api.fetchLatestRules()
        ability.update(newRules) // Thread-safe update
    }
}
```

---

## Integration Examples

### Example 1: Retrofit API Client

```kotlin
class AuthorizedApiClient(private val ability: Ability) {
    private val api: BlogApi = ...

    suspend fun deletePost(postId: String): Result<Unit> {
        val post = api.getPost(postId)

        if (!ability.can("delete", post)) {
            return Result.failure(UnauthorizedException())
        }

        return api.deletePost(postId)
    }
}
```

### Example 2: Jetpack Compose

```kotlin
@Composable
fun BlogPostScreen(post: BlogPost, ability: Ability) {
    Column {
        Text(post.title)
        Text(post.content)

        if (ability.can("update", post)) {
            Button(onClick = { /* edit */ }) {
                Text("Edit")
            }
        }

        if (ability.can("delete", post)) {
            Button(onClick = { /* delete */ }) {
                Text("Delete")
            }
        }
    }
}
```

### Example 3: Repository Pattern

```kotlin
interface BlogPostRepository {
    suspend fun getAllPosts(): List<BlogPost>
    suspend fun updatePost(post: BlogPost): Result<BlogPost>
}

class AuthorizedBlogPostRepository(
    private val api: BlogApi,
    private val ability: Ability
) : BlogPostRepository {

    override suspend fun getAllPosts(): List<BlogPost> {
        val allPosts = api.getAllPosts()
        // Filter posts user can read
        return allPosts.filter { ability.can("read", it) }
    }

    override suspend fun updatePost(post: BlogPost): Result<BlogPost> {
        if (!ability.canAsync("update", post)) {
            return Result.failure(UnauthorizedException())
        }
        return api.updatePost(post)
    }
}
```

---

## Testing Your Permissions

### Unit Testing

```kotlin
class BlogPostViewModelTest {
    @Test
    fun `user can edit own posts`() {
        val userId = "user123"
        val ability = Ability.builder()
            .can("update", "BlogPost", conditions = mapOf("authorId" to userId))
            .build()

        val ownPost = BlogPost(authorId = userId, ...)
        val otherPost = BlogPost(authorId = "other", ...)

        assertTrue(ability.can("update", ownPost))
        assertFalse(ability.can("update", otherPost))
    }
}
```

### Integration Testing

```kotlin
class AuthorizationIntegrationTest {
    @Test
    fun `rules from JSON work correctly`() {
        val json = """
            [
                {"action": "read", "subject": "BlogPost", "inverted": false},
                {"action": "delete", "subject": "BlogPost", "inverted": true}
            ]
        """

        val rules = RawRule.listFromJson(json)
        val ability = Ability.fromRules(rules)

        assertTrue(ability.can("read", "BlogPost"))
        assertFalse(ability.can("delete", "BlogPost"))
    }
}
```

---

## Performance Tips

1. **Reuse Ability instances**: Create once, use many times
   ```kotlin
   // Good
   val ability = createAbilityForUser(user)
   ability.can("read", post1)
   ability.can("read", post2)

   // Bad (unnecessary recreation)
   createAbilityForUser(user).can("read", post1)
   createAbilityForUser(user).can("read", post2)
   ```

2. **Cache abilities per user**: Store in ViewModel or Repository
   ```kotlin
   class UserSession {
       val ability: Ability by lazy { createAbilityForCurrentUser() }
   }
   ```

3. **Use type literals for existence checks**: Avoid unnecessary object allocation
   ```kotlin
   // Efficient
   if (ability.can("read", "BlogPost")) { ... }

   // Less efficient (creates temporary object)
   if (ability.can("read", BlogPost("", "", "", false))) { ... }
   ```

4. **Batch permission checks**: Check multiple permissions in single pass
   ```kotlin
   val canEdit = ability.can("update", post)
   val canDelete = ability.can("delete", post)
   val canPublish = ability.can("publish", post)
   ```

---

## Troubleshooting

### Problem: Permission check always returns false

**Solution**: Check that subject types match exactly
```kotlin
// Wrong
ability.can("read", "blogPost") // lowercase
val post = BlogPost(...) // class name is "BlogPost"

// Correct
ability.can("read", "BlogPost") // matches class name
```

### Problem: Conditions not matching

**Solution**: Ensure condition keys match subject properties exactly
```kotlin
data class BlogPost(
    val authorId: String // Property name
)

// Correct
.can("update", "BlogPost", conditions = mapOf("authorId" to userId))

// Wrong
.can("update", "BlogPost", conditions = mapOf("author_id" to userId))
```

### Problem: Rules not updating

**Solution**: Call `update()` method with new rules
```kotlin
val newRules = fetchNewRules()
ability.update(newRules) // Don't create new Ability, update existing
```

---

## Next Steps

- Read the [full API documentation](./contracts/README.md)
- Explore [data model details](./data-model.md)
- Review [technical research decisions](./research.md)
- Check out the [sample app](../../sample/)

## Support

- **GitHub Issues**: [Report bugs or request features]
- **Documentation**: [Link to generated API docs]
- **Examples**: [Link to sample projects]
