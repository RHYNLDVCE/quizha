package com.rai.quizha.frontend.model

import kotlinx.serialization.Serializable

@Serializable
data class Admin(
    val id: Long? = null,
    val username: String,
    val fullName: String,
    val password: String? = null // optional, only used for creation/login
)

@Serializable
data class LoginResponse(
    val id: Long,
    val username: String,
    val fullName: String,
    val token: String
)


@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)