package com.rai.quizha.frontend.model

data class College(
    val name: String,
    val acronym: String,
    val courses: List<Course>
)