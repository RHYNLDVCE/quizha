package com.rai.quizha.server.routes

import com.rai.quizha.db.model.StudentAnswer
import com.rai.quizha.db.repo.StudentAnswerRepository
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

fun Route.studentAnswerRoutes(repository: StudentAnswerRepository) {

    route("/student-answers") {

        // ====================================================
        // 1. SHARED ROUTES (Accessible by Admin OR Student)
        // ====================================================
        authenticate("auth-jwt", "student-auth") {
            // Add a new student answer
            post {
                val answer = call.receive<StudentAnswer>()
                val id = repository.addStudentAnswer(answer)
                call.respond(HttpStatusCode.Created, mapOf("id" to id))
            }
        }

        // ====================================================
        // 2. ADMIN ONLY ROUTES (Full Access)
        // ====================================================
        authenticate("auth-jwt") {
            // Update a student answer
            put {
                val answer = call.receive<StudentAnswer>()
                repository.updateStudentAnswer(answer)
                call.respond(HttpStatusCode.OK, "Student answer updated")
            }

            // Delete a student answer
            delete {
                val params = call.receive<Map<String, Long>>()
                val studentActivityResultId = params["studentActivityResultId"]
                val questionId = params["questionId"]

                if (studentActivityResultId == null || questionId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing studentActivityResultId or questionId")
                    return@delete
                }

                repository.deleteStudentAnswer(studentActivityResultId, questionId)
                call.respond(HttpStatusCode.OK, "Student answer deleted")
            }

            // Get all student answers
            get {
                val answers = repository.getAllStudentAnswers().toList().flatten()
                call.respond(HttpStatusCode.OK, answers)
            }

            // Get student answers by result (student_activity_result_id)
            get("result/{studentActivityResultId}") {
                val studentActivityResultId = call.parameters["studentActivityResultId"]?.toLongOrNull()
                if (studentActivityResultId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid studentActivityResultId")
                    return@get
                }

                val answers = repository.getStudentAnswersByResult(studentActivityResultId).toList().flatten()
                call.respond(HttpStatusCode.OK, answers)
            }

            // Get student answers by question
            get("question/{questionId}") {
                val questionId = call.parameters["questionId"]?.toLongOrNull()
                if (questionId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid questionId")
                    return@get
                }

                val answers = repository.getStudentAnswersByQuestion(questionId).toList().flatten()
                call.respond(HttpStatusCode.OK, answers)
            }

            // Count correct answers by result
            get("count-correct/{studentActivityResultId}") {
                val studentActivityResultId = call.parameters["studentActivityResultId"]?.toLongOrNull()
                if (studentActivityResultId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid studentActivityResultId")
                    return@get
                }

                val count = repository.countCorrectAnswersByResult(studentActivityResultId)
                call.respond(HttpStatusCode.OK, mapOf("correctCount" to count))
            }
        }
    }
}