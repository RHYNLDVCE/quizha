package com.rai.quizha.server.routes

import com.rai.quizha.db.model.Question
import com.rai.quizha.db.repo.QuestionsRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.flow.toList

fun Route.questionsRoutes(repository: QuestionsRepository) {

    route("/questions") {

        // ====================================================
        // 1. SHARED ROUTES (Accessible by Admin OR Student)
        // ====================================================
        // By listing both providers, Ktor will try "auth-jwt" first (Admin).
        // If that fails/doesn't apply, it tries "student-auth" (Student).
        // If either succeeds, the request proceeds.
        authenticate("auth-jwt", "student-auth") {

            // Get questions by activity ID
            get("activity/{activityId}") {
                val activityId = call.parameters["activityId"]?.toLongOrNull()
                if (activityId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid activity ID")
                    return@get
                }
                val questions = repository.getQuestionsByActivityId(activityId).toList().flatten()
                call.respond(HttpStatusCode.OK, questions)
            }

            // Count questions by activity ID
            get("activity/{activityId}/count") {
                val activityId = call.parameters["activityId"]?.toLongOrNull()
                if (activityId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid activity ID")
                    return@get
                }
                val count = repository.countQuestionsByActivityId(activityId)
                call.respond(HttpStatusCode.OK, mapOf("count" to count))
            }
        }

        // ====================================================
        // 2. ADMIN ONLY ROUTES (Full Access)
        // ====================================================
        authenticate("auth-jwt") {

            // Add a new question
            post {
                val question = call.receive<Question>()
                val id = repository.addQuestion(question)
                call.respond(HttpStatusCode.Created, mapOf("id" to id))
            }

            // Update a question
            put("{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid question ID")
                    return@put
                }
                val question = call.receive<Question>().copy(id = id)
                repository.updateQuestion(question)
                call.respond(HttpStatusCode.OK, "Question updated")
            }

            // Delete a question
            delete("{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid question ID")
                    return@delete
                }
                repository.deleteQuestion(id)
                call.respond(HttpStatusCode.OK, "Question deleted")
            }

            // Delete all questions for a specific activity
            delete("activity/{activityId}") {
                val activityId = call.parameters["activityId"]?.toLongOrNull()
                if (activityId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid activity ID")
                    return@delete
                }
                repository.deleteQuestionsByActivityId(activityId)
                call.respond(HttpStatusCode.OK, "Questions deleted for activity")
            }

            // Get all questions (For Admin Dashboard / Debugging)
            get {
                val questions = repository.getAllQuestions().toList().flatten()
                call.respond(HttpStatusCode.OK, questions)
            }

            // Get question by ID (For Admin Editing)
            get("{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid question ID")
                    return@get
                }
                val question = repository.getQuestionById(id)
                if (question == null) {
                    call.respond(HttpStatusCode.NotFound, "Question not found")
                } else {
                    call.respond(HttpStatusCode.OK, question)
                }
            }
        }
    }
}