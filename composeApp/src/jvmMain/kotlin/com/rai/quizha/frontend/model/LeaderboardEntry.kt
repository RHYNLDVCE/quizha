package com.rai.quizha.frontend.model

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardEntry(
    val studentId: Long,
    val firstName: String,
    val lastName: String,
    val score: Int,
    val resultId: Long
)