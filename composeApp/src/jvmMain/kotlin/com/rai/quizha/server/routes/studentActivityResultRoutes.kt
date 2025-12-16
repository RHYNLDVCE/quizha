package com.rai.quizha.server.routes

import com.rai.quizha.db.model.StudentActivityResult
import com.rai.quizha.db.repo.ActivityStudentRepository
import com.rai.quizha.db.repo.StudentActivityResultRepository
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
import kotlinx.serialization.Serializable
import java.time.Instant

// Local DTO
@Serializable
data class LeaderboardResponse(
    val studentId: Long,
    val firstName: String,
    val lastName: String,
    val score: Int,
    val resultId: Long
)

fun Route.studentActivityResultRoutes(
    repository: StudentActivityResultRepository,
    answerRepository: StudentAnswerRepository,
    activityStudentRepository: ActivityStudentRepository
) {

    route("/student-activity-results") {

        // ... [Shared Routes] ...
        authenticate("auth-jwt", "student-auth") {
            post("start-by-ids") {
                val params = call.receive<Map<String, Long>>()
                val activityId = params["activityId"]
                val studentId = params["studentId"]

                if (activityId == null || studentId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing IDs")
                    return@post
                }

                val linkId = activityStudentRepository.getIdByActivityAndStudent(activityId, studentId)
                if (linkId == null) {
                    call.respond(HttpStatusCode.Forbidden, "Student not assigned to this activity")
                    return@post
                }

                val newResult = StudentActivityResult(
                    id = 0,
                    activityStudentId = linkId,
                    score = 0,
                    startedAt = Instant.now().toString(),
                    completedAt = null
                )

                val id = repository.addStudentActivityResult(newResult)
                call.respond(HttpStatusCode.Created, mapOf("id" to id))
            }

            post {
                val result = call.receive<StudentActivityResult>()
                val id = repository.addStudentActivityResult(result)
                call.respond(HttpStatusCode.Created, mapOf("id" to id))
            }

            post("{id}/finish") {
                val id = call.parameters["id"]?.toLongOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                    return@post
                }

                val correctCount = answerRepository.countCorrectAnswersByResult(id)
                val existingResult = repository.getStudentActivityResultById(id)

                if (existingResult != null) {
                    val updatedResult = existingResult.copy(
                        score = correctCount.toInt(),
                        completedAt = Instant.now().toString()
                    )
                    repository.updateStudentActivityResult(updatedResult)

                    call.respond(HttpStatusCode.OK, mapOf(
                        "message" to "Quiz finished",
                        "score" to correctCount
                    ))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Result not found")
                }
            }

            get("{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                    return@get
                }
                val result = repository.getStudentActivityResultById(id)
                if (result == null) {
                    call.respond(HttpStatusCode.NotFound, "Student activity result not found")
                } else {
                    call.respond(HttpStatusCode.OK, result)
                }
            }
        }

        // ====================================================
        // 2. ADMIN ONLY ROUTES
        // ====================================================
        authenticate("auth-jwt") {

            // --- LEADERBOARD ROUTE (UPDATED) ---
            get("leaderboard/{activityId}") {
                val activityId = call.parameters["activityId"]?.toLongOrNull()
                if (activityId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid activityId")
                    return@get
                }

                try {
                    // Use the new list function directly
                    val rows = repository.getLeaderboardList(activityId)

                    val response = rows.map {
                        LeaderboardResponse(it.studentId, it.firstName, it.lastName, it.score ?: 0, it.resultId)
                    }
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, "Error fetching leaderboard: ${e.message}")
                }
            }

            // ... [Other Admin Routes] ...
            put("{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                    return@put
                }
                val result = call.receive<StudentActivityResult>().copy(id = id)
                repository.updateStudentActivityResult(result)
                call.respond(HttpStatusCode.OK, "Student activity result updated")
            }

            delete("{id}") {
                val id = call.parameters["id"]?.toLongOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                    return@delete
                }
                repository.deleteStudentActivityResult(id)
                call.respond(HttpStatusCode.OK, "Student activity result deleted")
            }

            get {
                val results = repository.getAllStudentActivityResults().toList().flatten()
                call.respond(HttpStatusCode.OK, results)
            }

            get("enrollment/{activityStudentId}") {
                val activityStudentId = call.parameters["activityStudentId"]?.toLongOrNull()
                if (activityStudentId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid activityStudentId")
                    return@get
                }
                val results = repository.getStudentActivityResultsByEnrollment(activityStudentId).toList().flatten()
                call.respond(HttpStatusCode.OK, results)
            }

            get("count") {
                val count = repository.countStudentActivityResults()
                call.respond(HttpStatusCode.OK, mapOf("count" to count))
            }
        }
    }
}