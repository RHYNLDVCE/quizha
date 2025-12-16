package com.rai.quizha.server.routes

import com.rai.quizha.db.repo.AdminRepository
import com.rai.quizha.server.model.LoginResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.security.MessageDigest

fun Route.adminLoginRoute(repository: AdminRepository, generateToken: (Long, String) -> String) {

    route("/admins/login") {
        post {
            val credentials = call.receive<Map<String, String>>()
            val username = credentials["username"] ?: ""
            val password = credentials["password"] ?: ""

            val admin = repository.getByUsername(username)
            println("Login attempt: $username / $password")
            println("Stored hash: ${admin?.passwordHash}")
            println("Match: ${if (admin != null) verifyPassword(password, admin.passwordHash) else "N/A"}")
            // -------------------
            if (admin == null || !verifyPassword(password, admin.passwordHash)) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid username or password")
                return@post
            }

            // Generate JWT token for this admin
            val token = generateToken(admin.id, admin.username)

            call.respond(
                HttpStatusCode.OK,
                LoginResponse(
                    id = admin.id,
                    username = admin.username,
                    fullName = admin.fullName,
                    token = token
                )
            )

        }
    }
}

private fun verifyPassword(plain: String, hash: String): Boolean {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(plain.toByteArray())
    val hex = digest.joinToString("") { "%02x".format(it) }
    return hex == hash
}
