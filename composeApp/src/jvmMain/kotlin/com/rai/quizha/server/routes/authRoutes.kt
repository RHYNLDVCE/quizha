package com.rai.quizha.server.routes

import com.rai.quizha.server.model.JwtConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(jwtConfig: JwtConfig) {
    route("/auth/student-token") {
        post {
            val params = call.receive<Map<String, Long>>()
            val studentId = params["studentId"]
            val activityId = params["activityId"]

            if (studentId == null || activityId == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing ids")
                return@post
            }

            val token = jwtConfig.generateStudentToken(studentId, activityId)
            call.respond(HttpStatusCode.OK, mapOf("token" to token))
        }
    }
}