package com.rai.quizha.server.routes

import com.rai.quizha.db.model.Admin
import com.rai.quizha.db.repo.AdminRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.flow.toList

fun Route.adminRoutes(repository: AdminRepository) {

    route("/admins") {

        // Create a new admin
        post {
            val admin = call.receive<Admin>()
            val id = repository.insertAdmin(admin)
            call.respond(HttpStatusCode.Created, mapOf("id" to id))
        }

        // Get all admins
        get {
            val admins = repository.getAllAdmins().toList().flatten()
            call.respond(HttpStatusCode.OK, admins)
        }

        // Get admin by username
        get("{username}") {
            val username = call.parameters["username"] ?: ""
            val admin = repository.getByUsername(username)
            if (admin == null) {
                call.respond(HttpStatusCode.NotFound, "Admin not found")
            } else {
                call.respond(HttpStatusCode.OK, admin)
            }
        }

        // Delete admin by ID
        delete("{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@delete
            }
            val deleted = repository.deleteAdminById(id)
            if (deleted) {
                call.respond(HttpStatusCode.OK, "Admin deleted")
            } else {
                call.respond(HttpStatusCode.NotFound, "Admin not found")
            }
        }

    }
}