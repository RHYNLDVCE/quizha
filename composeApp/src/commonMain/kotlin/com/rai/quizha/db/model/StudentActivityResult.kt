package com.rai.quizha.db.model

import kotlinx.serialization.Serializable

@Serializable
data class StudentActivityResult(
    val id: Long,
    val activityStudentId: Long,
    val score: Int?,
    val startedAt: String,
    val completedAt: String?
)
