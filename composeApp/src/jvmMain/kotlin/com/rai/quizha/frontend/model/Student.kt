package com.rai.quizha.frontend.model

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


data class CreateStudentRequest(
    val name: String,
    val course: String
)