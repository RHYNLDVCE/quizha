package com.rai.quizha.db.model

import kotlinx.serialization.Serializable

@Serializable
data class Admin(
    val id: Long,
    val username: String,
    val passwordHash: String,
    val fullName: String
)
