package com.rai.quizha.db.model

import kotlinx.serialization.Serializable

@Serializable
data class Activity(
    val id: Long,
    val title: String,
    val timeduration: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)
