package com.rai.quizha.db.model

import kotlinx.serialization.Serializable

@Serializable
data class Student(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val yearlevel: String,
    val department: String,
    val course: String,
    val birthdate: String
)
