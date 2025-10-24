package com.casl.sample

/**
 * Sample domain models for demonstrating CASL authorization.
 */

data class BlogPost(
    val id: String,
    val title: String,
    val content: String,
    val authorId: String,
    val published: Boolean
)

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val phoneNumber: String? = null,
    val address: String? = null
)

data class Comment(
    val id: String,
    val postId: String,
    val authorId: String,
    val content: String
)
