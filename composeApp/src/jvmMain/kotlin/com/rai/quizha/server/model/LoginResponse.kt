package com.rai.quizha.server.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val id: Long,
    val username: String,
    val fullName: String,
    val token: String
)
