package com.rai.quizha.server.routes

import com.rai.quizha.db.model.Student
import com.rai.quizha.db.repo.StudentRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.studentRoutes(repository: StudentRepository) {

    route("/students") {

        // Create a new student
        post {
            val student = call.receive<Student>()
            val id = repository.insertStudent(student)
            call.respond(HttpStatusCode.Created, mapOf("id" to id))
        }

        // Get all students
        get {
            val students = repository.getAllStudentsList() // suspending snapshot
            call.respond(HttpStatusCode.OK, students)
        }

        // Get a student by ID
        get("{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@get
            }
            val student = repository.getStudentById(id)
            if (student == null) {
                call.respond(HttpStatusCode.NotFound, "Student not found")
            } else {
                call.respond(HttpStatusCode.OK, student)
            }
        }

        // Update a student
        put("{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@put
            }
            val student = call.receive<Student>().copy(id = id)
            repository.updateStudent(student)
            call.respond(HttpStatusCode.OK, "Student updated")
        }

        // Delete a student
        delete("{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@delete
            }
            repository.deleteStudent(id)
            call.respond(HttpStatusCode.OK, "Student deleted")
        }

        // Get students by year level
        get("yearlevel/{yearlevel}") {
            val yearlevel = call.parameters["yearlevel"] ?: ""
            val students = repository.getStudentsByYearlevelList(yearlevel)
            call.respond(HttpStatusCode.OK, students)
        }

        // Get students by department
        get("department/{department}") {
            val department = call.parameters["department"] ?: ""
            val students = repository.getStudentsByDepartmentList(department)
            call.respond(HttpStatusCode.OK, students)
        }

        // Search students by name
        get("search") {
            val name = call.request.queryParameters["name"] ?: ""
            val students = repository.searchStudentsByNameList(name)
            call.respond(HttpStatusCode.OK, students)
        }
    }
}
