package com.rai.quizha.server.helper

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.rai.quizha.server.model.JwtConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuthentication(jwtConfig: JwtConfig) {
    install(Authentication) {
        // 1. Admin Auth (Existing)
        jwt("auth-jwt") {
            verifier(
                JWT.require(Algorithm.HMAC256(jwtConfig.secret))
                    .withAudience(jwtConfig.audience)
                    .withIssuer(jwtConfig.issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("role").asString() == "admin") {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }

        // 2. Student Auth (New)
        jwt("student-auth") {
            verifier(
                JWT.require(Algorithm.HMAC256(jwtConfig.secret))
                    .withAudience(jwtConfig.audience)
                    .withIssuer(jwtConfig.issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("role").asString() == "student" &&
                    credential.payload.getClaim("studentId").asLong() != null) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }
}