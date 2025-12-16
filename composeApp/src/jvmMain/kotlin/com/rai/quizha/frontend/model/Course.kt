package com.rai.quizha.frontend.model

data class Course(
    val name: String,
    val acronym: String,
    val majors: List<String> = emptyList() // Optional list of majors
)