package com.rai.quizha.model

import kotlinx.serialization.Serializable

@Serializable
data class QrPayload(val activityId: Long, val studentId: Long, val token: String)
