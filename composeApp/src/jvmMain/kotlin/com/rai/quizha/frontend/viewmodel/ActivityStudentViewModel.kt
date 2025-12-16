package com.rai.quizha.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rai.quizha.frontend.model.Activity
import com.rai.quizha.frontend.model.Student
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ActivityStudentViewModel(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val getToken: () -> String
) : ViewModel() {

    private val _studentsByActivity = MutableStateFlow<List<Student>>(emptyList())
    val studentsByActivity: StateFlow<List<Student>> get() = _studentsByActivity

    private val _activitiesByStudent = MutableStateFlow<List<Activity>>(emptyList())
    val activitiesByStudent: StateFlow<List<Activity>> get() = _activitiesByStudent

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> get() = _message

    private fun authHeader(builder: HttpRequestBuilder) {
        val token = getToken()
        if (token.isNotEmpty()) {
            builder.header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    fun addStudentToActivity(activityId: Long, studentId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = httpClient.post("$baseUrl/activity-students") {
                    authHeader(this)
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("activityId" to activityId, "studentId" to studentId))
                }
                onResult(response.status == HttpStatusCode.Created)
                _message.value = if (response.status == HttpStatusCode.Created) "Student added" else "Failed to add student"
            } catch (e: Exception) {
                e.printStackTrace()
                _message.value = "Error adding student"
                onResult(false)
            }
        }
    }

    fun removeStudentFromActivity(activityId: Long, studentId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = httpClient.delete("$baseUrl/activity-students") {
                    authHeader(this)
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("activityId" to activityId, "studentId" to studentId))
                }
                onResult(response.status == HttpStatusCode.OK)
                _message.value = if (response.status == HttpStatusCode.OK) "Student removed" else "Failed to remove student"
            } catch (e: Exception) {
                e.printStackTrace()
                _message.value = "Error removing student"
                onResult(false)
            }
        }
    }

    fun getStudentsByActivity(activityId: Long) {
        viewModelScope.launch {
            try {
                val students: List<Student> = httpClient.get("$baseUrl/activity-students/activity/$activityId") {
                    authHeader(this)
                }.body()
                _studentsByActivity.value = students
            } catch (e: Exception) {
                e.printStackTrace()
                _studentsByActivity.value = emptyList()
                _message.value = "Failed to load students"
            }
        }
    }

    fun getActivitiesByStudent(studentId: Long) {
        viewModelScope.launch {
            try {
                val activities: List<Activity> = httpClient.get("$baseUrl/activity-students/student/$studentId") {
                    authHeader(this)
                }.body()
                _activitiesByStudent.value = activities
            } catch (e: Exception) {
                e.printStackTrace()
                _activitiesByStudent.value = emptyList()
                _message.value = "Failed to load activities"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
