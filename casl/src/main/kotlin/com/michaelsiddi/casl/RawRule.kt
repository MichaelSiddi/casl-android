package com.michaelsiddi.casl

import org.json.JSONArray
import org.json.JSONObject

/**
 * Serializable representation of an authorization rule.
 *
 * This class is designed for JSON serialization and isomorphic authorization
 * between Android client and backend server.
 *
 * Example JSON:
 * ```json
 * {
 *   "action": "read",
 *   "subject": "BlogPost",
 *   "conditions": { "authorId": "user123" },
 *   "fields": ["title", "content"],
 *   "inverted": false
 * }
 * ```
 *
 * Or with multiple actions:
 * ```json
 * {
 *   "action": ["read", "update"],
 *   "subject": "BlogPost"
 * }
 * ```
 *
 * @property action The action(s) this rule applies to (single string or list of strings)
 * @property subject The subject type this rule applies to (e.g., "BlogPost", "Comment")
 * @property conditions Optional attribute matchers for conditional authorization
 * @property fields Optional field restrictions (null means all fields)
 * @property inverted True for "cannot" rules, false for "can" rules
 */
public data class RawRule(
    val action: Any, // Can be String or List<String>
    val subject: String,
    val conditions: Map<String, Any?>? = null,
    val fields: List<String>? = null,
    val inverted: Boolean = false
) {
    init {
        when (action) {
            is String -> require(action.isNotBlank()) { "action must not be blank" }
            is List<*> -> {
                require(action.isNotEmpty()) { "action list must not be empty" }
                require(action.all { it is String && it.isNotBlank() }) {
                    "all actions in list must be non-blank strings"
                }
            }
            else -> throw IllegalArgumentException("action must be a String or List<String>")
        }
        require(subject.isNotBlank()) { "subject must not be blank" }
        require(fields == null || fields.isNotEmpty()) {
            "fields must not be empty if specified"
        }
    }

    /**
     * Get actions as a list (normalizes single action to list).
     */
    internal fun getActions(): List<String> {
        return when (action) {
            is String -> listOf(action)
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                action as List<String>
            }
            else -> emptyList()
        }
    }

    /**
     * Serialize this rule to JSON string.
     *
     * @return JSON string representation
     */
    public fun toJson(): String {
        val jsonObject = JSONObject()
        // Handle both string and list actions
        when (action) {
            is String -> jsonObject.put("action", action)
            is List<*> -> jsonObject.put("action", JSONArray(action))
        }
        jsonObject.put("subject", subject)

        conditions?.let {
            jsonObject.put("conditions", mapToJsonObject(it))
        }

        fields?.let {
            jsonObject.put("fields", JSONArray(it))
        }

        jsonObject.put("inverted", inverted)

        return jsonObject.toString()
    }

    /**
     * Convert to internal Rule representation.
     * Note: This keeps the action as-is (String or List<String>).
     * The Ability class will handle expanding array actions during indexing.
     *
     * @return Rule instance
     */
    internal fun toRule(): Rule {
        return Rule(
            action = action,
            subjectType = subject,
            conditions = conditions,
            fields = fields?.toSet(),
            inverted = inverted
        )
    }

    public companion object {
        /**
         * Parse RawRule from JSON string.
         *
         * @param json JSON string representation
         * @return Parsed RawRule
         * @throws org.json.JSONException if JSON is malformed
         * @throws IllegalArgumentException if required fields are missing or invalid
         */
        @JvmStatic
        public fun fromJson(json: String): RawRule {
            val jsonObject = JSONObject(json)
            return fromJsonObject(jsonObject)
        }

        /**
         * Parse RawRule from JSONObject.
         */
        private fun fromJsonObject(jsonObject: JSONObject): RawRule {
            // Handle both string and array actions
            val action: Any = when (val actionValue = jsonObject.get("action")) {
                is String -> actionValue
                is JSONArray -> jsonArrayToList(actionValue)
                else -> throw IllegalArgumentException("action must be a string or array")
            }
            val subject = jsonObject.getString("subject")

            val conditions = if (jsonObject.has("conditions")) {
                jsonObjectToMap(jsonObject.getJSONObject("conditions"))
            } else null

            val fields = if (jsonObject.has("fields")) {
                jsonArrayToList(jsonObject.getJSONArray("fields"))
            } else null

            val inverted = jsonObject.optBoolean("inverted", false)

            return RawRule(
                action = action,
                subject = subject,
                conditions = conditions,
                fields = fields,
                inverted = inverted
            )
        }

        /**
         * Parse multiple RawRules from JSON array string.
         *
         * @param json JSON array string representation
         * @return List of parsed RawRules
         * @throws org.json.JSONException if JSON is malformed
         * @throws IllegalArgumentException if any rule is invalid
         */
        @JvmStatic
        public fun listFromJson(json: String): List<RawRule> {
            val jsonArray = JSONArray(json)
            return (0 until jsonArray.length()).map { i ->
                fromJsonObject(jsonArray.getJSONObject(i))
            }
        }

        /**
         * Serialize list of rules to JSON array string.
         *
         * @param rules List of rules to serialize
         * @return JSON array string representation
         */
        @JvmStatic
        public fun listToJson(rules: List<RawRule>): String {
            val jsonArray = JSONArray()
            rules.forEach { rule ->
                jsonArray.put(JSONObject(rule.toJson()))
            }
            return jsonArray.toString()
        }

        /**
         * Convert Map to JSONObject recursively.
         */
        private fun mapToJsonObject(map: Map<String, Any?>): JSONObject {
            val jsonObject = JSONObject()
            map.forEach { (key, value) ->
                when (value) {
                    null -> jsonObject.put(key, JSONObject.NULL)
                    is Map<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        jsonObject.put(key, mapToJsonObject(value as Map<String, Any?>))
                    }
                    is List<*> -> jsonObject.put(key, JSONArray(value))
                    else -> jsonObject.put(key, value)
                }
            }
            return jsonObject
        }

        /**
         * Convert JSONObject to Map recursively.
         */
        private fun jsonObjectToMap(jsonObject: JSONObject): Map<String, Any?> {
            val map = mutableMapOf<String, Any?>()
            jsonObject.keys().forEach { key ->
                val value = jsonObject.get(key)
                map[key] = when (value) {
                    JSONObject.NULL -> null
                    is JSONObject -> jsonObjectToMap(value)
                    is JSONArray -> jsonArrayToList(value)
                    else -> value
                }
            }
            return map
        }

        /**
         * Convert JSONArray to List.
         */
        private fun jsonArrayToList(jsonArray: JSONArray): List<String> {
            return (0 until jsonArray.length()).map { i ->
                jsonArray.getString(i)
            }
        }
    }
}
