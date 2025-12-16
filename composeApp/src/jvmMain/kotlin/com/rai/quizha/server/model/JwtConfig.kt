package com.rai.quizha.server.model

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val expirationMillis: Long = 3600000 // 1 hour (or longer for quizzes)
) {
    private val algorithm = Algorithm.HMAC256(secret)

    // Existing Admin Token Generator
    fun generateToken(adminId: Long, username: String): String {
        val now = System.currentTimeMillis()
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("id", adminId)
            .withClaim("username", username)
            .withClaim("role", "admin") // <--- THIS LINE IS CRITICAL
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + expirationMillis))
            .sign(algorithm)
    }

    // NEW: Student Token Generator
    fun generateStudentToken(studentId: Long, activityId: Long): String {
        val now = System.currentTimeMillis()
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("studentId", studentId)
            .withClaim("activityId", activityId)
            .withClaim("role", "student")
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + 86400000)) // 24 hours validity
            .sign(algorithm)
    }

    fun getAlgorithm(): Algorithm = algorithm
}