package com.rai.quizha.db.model

import kotlinx.serialization.Serializable

@Serializable
data class StudentAnswer(
    val id: Long,
    val studentActivityResultId: Long,
    val questionId: Long,
    val selectedOption: String
)
