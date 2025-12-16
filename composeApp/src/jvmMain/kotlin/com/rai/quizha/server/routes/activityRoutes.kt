package com.rai.quizha.server.routes

import com.rai.quizha.db.model.Activity
import com.rai.quizha.db.repo.ActivityRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.toList

fun Route.activityRoutes(repository: ActivityRepository) {

    route("/activities") {
        // Create a new activity
        post {
            val activity = call.receive<Activity>()
            val id = repository.insertActivity(activity.copy(status = "pending"))
            call.respond(HttpStatusCode.Created, mapOf("id" to id))
        }

        // Get all activities
        get {
            val activities = repository.getAllActivities().toList().flatten()
            call.respond(HttpStatusCode.OK, activities)
        }

        // Get activities sorted by created date descending
        get("created-desc") {
            val activities = repository.getAllActivitiesByCreatedDesc().toList().flatten()
            call.respond(HttpStatusCode.OK, activities)
        }

        // Get activities sorted by created date ascending
        get("created-asc") {
            val activities = repository.getAllActivitiesByCreatedAsc().toList().flatten()
            call.respond(HttpStatusCode.OK, activities)
        }

        // Get activity by ID
        get("{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@get
            }
            val activity = repository.getActivityById(id)
            if (activity == null) {
                call.respond(HttpStatusCode.NotFound, "Activity not found")
            } else {
                call.respond(HttpStatusCode.OK, activity)
            }
        }

        // Update an activity
        put("{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@put
            }
            val activity = call.receive<Activity>().copy(id = id, status = "pending")
            repository.updateActivity(activity)
            call.respond(HttpStatusCode.OK, "Activity updated")
        }

        // Delete an activity
        delete("{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@delete
            }
            repository.deleteActivity(id)
            call.respond(HttpStatusCode.OK, "Activity deleted")
        }

        // --- 1. START ACTIVITY (Updated with Broadcast) ---
        put("/{id}/start") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@put
            }

            // Update DB
            repository.startActivity(id) // Sets status = 'ongoing'

            // Broadcast to Mobile Clients
            broadcastActivityStatus(id, "ongoing")

            call.respond(HttpStatusCode.OK, "Activity started")
        }

        // --- 2. PAUSE ACTIVITY (New) ---
        put("/{id}/pause") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@put
            }

            val activity = repository.getActivityById(id)
            if (activity == null) {
                call.respond(HttpStatusCode.NotFound, "Activity not found")
                return@put
            }

            // Update DB status to 'paused'
            repository.updateActivity(activity.copy(status = "paused"))

            // Broadcast
            broadcastActivityStatus(id, "paused")

            call.respond(HttpStatusCode.OK, "Activity paused")
        }

        // --- 3. RESUME ACTIVITY (New) ---
        put("/{id}/resume") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@put
            }

            val activity = repository.getActivityById(id)
            if (activity == null) {
                call.respond(HttpStatusCode.NotFound, "Activity not found")
                return@put
            }

            // Update DB status back to 'ongoing'
            repository.updateActivity(activity.copy(status = "ongoing"))

            // Broadcast
            broadcastActivityStatus(id, "ongoing")

            call.respond(HttpStatusCode.OK, "Activity resumed")
        }

        // --- 4. COMPLETE ACTIVITY (Updated with Broadcast) ---
        put("/{id}/complete") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@put
            }

            // Update DB
            repository.markActivityCompleted(id)

            // Broadcast
            broadcastActivityStatus(id, "completed")

            call.respond(HttpStatusCode.OK, "Activity completed")
        }

        // Get activities by status
        get("status/{status}") {
            val status = call.parameters["status"] ?: ""
            val activities = repository.getActivitiesByStatus(status).toList().flatten()
            call.respond(HttpStatusCode.OK, activities)
        }

        // Count total activities
        get("count") {
            val count = repository.countActivities()
            call.respond(HttpStatusCode.OK, mapOf("count" to count))
        }

        // Count activities by status
        get("count/status/{status}") {
            val status = call.parameters["status"] ?: ""
            val count = repository.countActivitiesByStatus(status)
            call.respond(HttpStatusCode.OK, mapOf("count" to count))
        }
    }
}