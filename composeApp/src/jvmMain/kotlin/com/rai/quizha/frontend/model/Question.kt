package com.rai.quizha.frontend.model

import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val id: Long = 0,
    val activityId: Long,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOption: String
)
