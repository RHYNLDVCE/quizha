package com.rai.quizha.db.model

import kotlinx.serialization.Serializable

@Serializable
data class QrToken(
    val id: Long,
    val activityId: Long,
    val studentId: Long,
    val token: String,
    val expiresAt: String
)
