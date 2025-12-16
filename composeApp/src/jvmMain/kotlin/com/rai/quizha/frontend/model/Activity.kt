package com.rai.quizha.frontend.model

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

sealed class ActivityResult<out T> {
    object Loading : ActivityResult<Nothing>()
    data class Success<T>(val data: T) : ActivityResult<T>()
    data class Error(val message: String) : ActivityResult<Nothing>()
}
