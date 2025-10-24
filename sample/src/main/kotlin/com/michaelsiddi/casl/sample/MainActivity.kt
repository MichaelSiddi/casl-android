package com.michaelsiddi.casl.sample

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.michaelsiddi.casl.Ability
import com.michaelsiddi.casl.RawRule
import com.michaelsiddi.casl.extensions.buildAbility
import com.michaelsiddi.casl.extensions.filterBySubject
import com.michaelsiddi.casl.extensions.onlyPermissions

/**
 * Sample app demonstrating CASL Android Authorization Library usage.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val output = findViewById<TextView>(R.id.outputText)
        val results = StringBuilder()

        // Example 1: Basic Authorization
        results.appendLine("=== Example 1: Basic Authorization ===\n")
        val basicExample = demonstrateBasicAuthorization()
        results.appendLine(basicExample)

        // Example 2: Role-Based Access Control
        results.appendLine("\n=== Example 2: Role-Based Access Control ===\n")
        val rbacExample = demonstrateRBAC()
        results.appendLine(rbacExample)

        // Example 3: Field-Level Permissions
        results.appendLine("\n=== Example 3: Field-Level Permissions ===\n")
        val fieldExample = demonstrateFieldLevelPermissions()
        results.appendLine(fieldExample)

        // Example 4: JSON Serialization
        results.appendLine("\n=== Example 4: JSON Serialization ===\n")
        val jsonExample = demonstrateJSONSerialization()
        results.appendLine(jsonExample)

        // Example 5: Kotlin DSL
        results.appendLine("\n=== Example 5: Kotlin DSL ===\n")
        val dslExample = demonstrateKotlinDSL()
        results.appendLine(dslExample)

        output.text = results.toString()
    }

    private fun demonstrateBasicAuthorization(): String {
        val currentUserId = "user123"

        val ability = Ability.builder()
            .can("read", "BlogPost")
            .can("create", "BlogPost")
            .can("update", "BlogPost", mapOf("authorId" to currentUserId), null)
            .cannot("delete", "BlogPost", mapOf("published" to true), null)
            .build()

        val myPost = BlogPost(
            id = "post1",
            title = "My Post",
            content = "Content",
            authorId = currentUserId,
            published = false
        )

        val othersPost = BlogPost(
            id = "post2",
            title = "Other's Post",
            content = "Content",
            authorId = "other456",
            published = true
        )

        return buildString {
            appendLine("Can read any post: ${ability.can("read", "BlogPost")}")
            appendLine("Can update my post: ${ability.can("update", myPost)}")
            appendLine("Can update other's post: ${ability.can("update", othersPost)}")
            appendLine("Can delete my unpublished post: ${ability.can("delete", myPost)}")
            appendLine("Can delete published post: ${ability.can("delete", othersPost)}")
        }
    }

    private fun demonstrateRBAC(): String {
        fun createAbilityForRole(role: String, userId: String): Ability {
            val builder = Ability.builder()

            when (role) {
                "admin" -> {
                    builder
                        .can("manage", "BlogPost")
                        .can("manage", "User")
                        .can("manage", "Comment")
                }
                "editor" -> {
                    builder
                        .can("read", "BlogPost")
                        .can("create", "BlogPost")
                        .can("update", "BlogPost")
                        .can("delete", "BlogPost", mapOf("authorId" to userId), null)
                }
                "viewer" -> {
                    builder.can("read", "BlogPost")
                }
            }

            return builder.build()
        }

        val adminAbility = createAbilityForRole("admin", "admin1")
        val editorAbility = createAbilityForRole("editor", "editor1")
        val viewerAbility = createAbilityForRole("viewer", "viewer1")

        val post = BlogPost("1", "Title", "Content", "editor1", true)

        return buildString {
            appendLine("Admin can delete any post: ${adminAbility.can("delete", post)}")
            appendLine("Editor can delete own post: ${editorAbility.can("delete", post)}")
            appendLine("Viewer can delete post: ${viewerAbility.can("delete", post)}")
            appendLine("Viewer can read post: ${viewerAbility.can("read", post)}")
        }
    }

    private fun demonstrateFieldLevelPermissions(): String {
        val currentUserId = "user123"

        val ability = Ability.builder()
            .can("read", "User", null, listOf("name", "email"))
            .can("read", "User", mapOf("id" to currentUserId), listOf("phoneNumber", "address"))
            .build()

        val myProfile = User(
            id = currentUserId,
            name = "John Doe",
            email = "john@example.com",
            role = "editor",
            phoneNumber = "555-1234",
            address = "123 Main St"
        )

        val otherProfile = User(
            id = "other456",
            name = "Jane Smith",
            email = "jane@example.com",
            role = "viewer",
            phoneNumber = "555-5678",
            address = "456 Oak Ave"
        )

        return buildString {
            appendLine("Can read anyone's name: ${ability.can("read", myProfile, "name")}")
            appendLine("Can read my phone: ${ability.can("read", myProfile, "phoneNumber")}")
            appendLine("Can read other's phone: ${ability.can("read", otherProfile, "phoneNumber")}")
            appendLine("Can read other's email: ${ability.can("read", otherProfile, "email")}")
        }
    }

    private fun demonstrateJSONSerialization(): String {
        // Create ability
        val originalAbility = Ability.builder()
            .can("read", "BlogPost")
            .can("update", "BlogPost", mapOf("authorId" to "user123"), null)
            .build()

        // Export to JSON
        val rules = originalAbility.exportRules()
        val json = RawRule.listToJson(rules)

        // Import from JSON
        val importedRules = RawRule.listFromJson(json)
        val newAbility = Ability.fromRules(importedRules)

        val post = BlogPost("1", "Title", "Content", "user123", false)

        return buildString {
            appendLine("Exported JSON (truncated):")
            appendLine(json.take(100) + "...")
            appendLine("\nOriginal ability can update: ${originalAbility.can("update", post)}")
            appendLine("Imported ability can update: ${newAbility.can("update", post)}")
            appendLine("Rule count: ${importedRules.size}")
        }
    }

    private fun demonstrateKotlinDSL(): String {
        val userId = "user123"

        // Using Kotlin DSL
        val ability = buildAbility {
            can("read", "BlogPost")
            can("update", "BlogPost", mapOf("authorId" to userId))
            cannot("delete", "BlogPost", mapOf("published" to true))
        }

        // Using extension functions
        val rules = ability.exportRules()
        val blogPostRules = rules.filterBySubject("BlogPost")
        val permissions = blogPostRules.onlyPermissions()

        val post = BlogPost("1", "Title", "Content", userId, false)

        return buildString {
            appendLine("Can update (DSL): ${ability.can("update", post)}")
            appendLine("Total rules: ${rules.size}")
            appendLine("BlogPost rules: ${blogPostRules.size}")
            appendLine("Permission rules: ${permissions.size}")
        }
    }
}
