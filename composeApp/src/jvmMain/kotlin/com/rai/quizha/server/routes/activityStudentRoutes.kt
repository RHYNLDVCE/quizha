package com.rai.quizha.server.routes

import com.rai.quizha.db.model.Activity
import com.rai.quizha.db.model.Student
import com.rai.quizha.db.repo.ActivityStudentRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.flow.toList

fun Route.activityStudentRoutes(repository: ActivityStudentRepository) {

    route("/activity-students") {

        // Add student to activity
        post {
            val params = call.receive<Map<String, Long>>()
            val activityId = params["activityId"]
            val studentId = params["studentId"]

            if (activityId == null || studentId == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing activityId or studentId")
                return@post
            }

            val id = repository.addStudentToActivity(activityId, studentId)
            call.respond(HttpStatusCode.Created, mapOf("id" to id))
        }

        // Remove student from activity
        delete {
            val params = call.receive<Map<String, Long>>()
            val activityId = params["activityId"]
            val studentId = params["studentId"]

            if (activityId == null || studentId == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing activityId or studentId")
                return@delete
            }

            repository.removeStudentFromActivity(activityId, studentId)
            call.respond(HttpStatusCode.OK, "Student removed from activity")
        }

        // Get students by activity
        get("activity/{activityId}") {
            val activityId = call.parameters["activityId"]?.toLongOrNull()
            if (activityId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid activityId")
                return@get
            }

            val students: List<Student> = repository.getStudentsByActivity(activityId).toList().flatten()
            call.respond(HttpStatusCode.OK, students)
        }

        // Get activities by student
        get("student/{studentId}") {
            val studentId = call.parameters["studentId"]?.toLongOrNull()
            if (studentId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid studentId")
                return@get
            }

            val activities: List<Activity> = repository.getActivitiesByStudent(studentId).toList().flatten()
            call.respond(HttpStatusCode.OK, activities)
        }
    }
}
