package com.rai.quizha.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rai.quizha.frontend.model.Student
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StudentViewModel(
    private val client: HttpClient,
    private val baseUrl: String,
    private val getToken: () -> String? // Function to get the current JWT
) : ViewModel() {

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students

    private val _student = MutableStateFlow<Student?>(null)
    val student: StateFlow<Student?> = _student

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private fun authHeader(): String = "Bearer ${getToken() ?: ""}"

    private suspend inline fun <reified T> authorizedGet(endpoint: String): T {
        println("DEBUG: GET $endpoint with token -> ${getToken()}")
        return client.get("$baseUrl$endpoint") {
            header("Authorization", authHeader())
        }.body()
    }

    private suspend inline fun <reified T> authorizedPost(endpoint: String, body: Any): T {
        println("DEBUG: POST $endpoint with body -> $body and token -> ${getToken()}")
        return client.post("$baseUrl$endpoint") {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
    }

    private suspend fun authorizedPut(endpoint: String, body: Any) {
        println("DEBUG: PUT $endpoint with body -> $body and token -> ${getToken()}")
        client.put("$baseUrl$endpoint") {
            header("Authorization", authHeader())
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    private suspend fun authorizedDelete(endpoint: String) {
        println("DEBUG: DELETE $endpoint with token -> ${getToken()}")
        client.delete("$baseUrl$endpoint") {
            header("Authorization", authHeader())
        }
    }

    fun createStudent(student: Student) {
        viewModelScope.launch {
            try {
                authorizedPost<Unit>("/students", student)
                fetchStudents()
            } catch (e: Exception) {
                println("ERROR: createStudent -> ${e.message}")
                _error.value = e.message
            }
        }
    }

    fun fetchStudents() {
        viewModelScope.launch {
            try {
                val token = getToken()
                println("DEBUG: fetchStudents using token = $token") // <-- log token

                val response: List<Student> = authorizedGet("/students")
                _students.value = response
            } catch (e: Exception) {
                println("ERROR: fetchStudents -> ${e.message}")
                _error.value = e.message
            }
        }
    }

    fun fetchStudent(id: Long) {
        viewModelScope.launch {
            try {
                val response: Student = authorizedGet("/students/$id")
                _student.value = response
            } catch (e: Exception) {
                println("ERROR: fetchStudent -> ${e.message}")
                _error.value = e.message
                _student.value = null
            }
        }
    }

    fun updateStudent(student: Student, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                authorizedPut("/students/${student.id}", student)
                fetchStudents()
                onResult(true)
            } catch (e: Exception) {
                println("ERROR: updateStudent -> ${e.message}")
                _error.value = e.message
                onResult(false)
            }
        }
    }



    fun deleteStudent(id: Long) {
        viewModelScope.launch {
            try {
                authorizedDelete("/students/$id")
                fetchStudents()
            } catch (e: Exception) {
                println("ERROR: deleteStudent -> ${e.message}")
                _error.value = e.message
            }
        }
    }

    fun getStudentsByYearlevel(yearlevel: String) {
        viewModelScope.launch {
            try {
                val response: List<Student> = authorizedGet("/students/yearlevel/$yearlevel")
                _students.value = response
            } catch (e: Exception) {
                println("ERROR: getStudentsByYearlevel -> ${e.message}")
                _error.value = e.message
            }
        }
    }

    fun getStudentsByDepartment(department: String) {
        viewModelScope.launch {
            try {
                val response: List<Student> = authorizedGet("/students/department/$department")
                _students.value = response
            } catch (e: Exception) {
                println("ERROR: getStudentsByDepartment -> ${e.message}")
                _error.value = e.message
            }
        }
    }

    fun searchStudents(name: String) {
        viewModelScope.launch {
            try {
                val response: List<Student> = authorizedGet("/students/search?name=$name")
                _students.value = response
            } catch (e: Exception) {
                println("ERROR: searchStudents -> ${e.message}")
                _error.value = e.message
            }
        }
    }
}
