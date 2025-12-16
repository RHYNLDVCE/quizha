package com.rai.quizha.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rai.quizha.frontend.model.Activity
import com.rai.quizha.frontend.model.ActivityResult
import com.rai.quizha.frontend.model.LeaderboardEntry
import com.rai.quizha.frontend.model.Student
import com.rai.quizha.db.model.StudentAnswer
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ActivityViewModel(
    private val client: HttpClient,
    private val baseUrl: String,
    private val getToken: () -> String
) : ViewModel() {

    private fun authHeaders() = headersOf("Authorization", "Bearer ${getToken()}")

    private val _remainingTimes = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val remainingTimes: StateFlow<Map<Long, Int>> get() = _remainingTimes

    private val _activities = MutableStateFlow<ActivityResult<List<Activity>>>(ActivityResult.Loading)
    val activities: StateFlow<ActivityResult<List<Activity>>> get() = _activities

    private val _activityDetail = MutableStateFlow<ActivityResult<Activity>>(ActivityResult.Loading)
    val activityDetail: StateFlow<ActivityResult<Activity>> get() = _activityDetail

    private val _assignedStudents = MutableStateFlow<List<Student>>(emptyList())
    val assignedStudents: StateFlow<List<Student>> get() = _assignedStudents

    // --- Leaderboard State ---
    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> get() = _leaderboard

    // --- NEW: Leaderboard Error State ---
    private val _leaderboardError = MutableStateFlow<String?>(null)
    val leaderboardError: StateFlow<String?> get() = _leaderboardError

    private val timerJobs = mutableMapOf<Long, Job>()

    // ... [Existing CRUD Methods] ...

    fun fetchAllActivities() {
        if (_activities.value is ActivityResult.Loading || _activities.value is ActivityResult.Error) {
            _activities.value = ActivityResult.Loading
        }
        viewModelScope.launch {
            try {
                val list: List<Activity> = client.get("$baseUrl/activities") {
                    headers.appendAll(authHeaders())
                }.body()
                _activities.value = ActivityResult.Success(list)
            } catch (e: Exception) {
                _activities.value = ActivityResult.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun fetchActivityById(id: Long) {
        viewModelScope.launch {
            try {
                val activity: Activity = client.get("$baseUrl/activities/$id") {
                    headers.appendAll(authHeaders())
                }.body()
                _activityDetail.value = ActivityResult.Success(activity)
            } catch (e: ClientRequestException) {
                if (e.response.status == HttpStatusCode.NotFound) {
                    _activityDetail.value = ActivityResult.Error("Activity not found")
                } else {
                    _activityDetail.value = ActivityResult.Error(e.localizedMessage ?: "Unknown error")
                }
            } catch (e: Exception) {
                _activityDetail.value = ActivityResult.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun createActivity(activity: Activity, onResult: (Long) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.post("$baseUrl/activities") {
                    contentType(ContentType.Application.Json)
                    headers.appendAll(authHeaders())
                    setBody(activity)
                }
                val id = response.body<Map<String, Long>>()["id"] ?: 0
                onResult(id)
                fetchAllActivities()
            } catch (e: Exception) {
                println("Error creating activity: ${e.message}")
                onResult(0)
            }
        }
    }

    fun updateActivity(activity: Activity) {
        viewModelScope.launch {
            try {
                client.put("$baseUrl/activities/${activity.id}") {
                    contentType(ContentType.Application.Json)
                    headers.appendAll(authHeaders())
                    setBody(activity)
                }
                fetchAllActivities()
                fetchActivityById(activity.id)
            } catch (e: Exception) {
                println("Error updating activity: ${e.message}")
            }
        }
    }

    fun deleteActivity(id: Long) {
        viewModelScope.launch {
            try {
                client.delete("$baseUrl/activities/$id") {
                    headers.appendAll(authHeaders())
                }
                fetchAllActivities()
            } catch (e: Exception) {
                println("Error deleting activity: ${e.message}")
            }
        }
    }

    // ... [Timer Methods] ...
    fun startActivity(id: Long, durationMinutes: Int) {
        viewModelScope.launch {
            try {
                client.put("$baseUrl/activities/$id/start") { headers.appendAll(authHeaders()) }
                fetchAllActivities()
                fetchActivityById(id)
                val totalSeconds = durationMinutes * 60
                _remainingTimes.update { it + (id to totalSeconds) }
                runTimer(id)
            } catch (e: Exception) {}
        }
    }

    fun pauseActivity(id: Long) {
        viewModelScope.launch {
            try {
                timerJobs[id]?.cancel()
                timerJobs.remove(id)
                client.put("$baseUrl/activities/$id/pause") { headers.appendAll(authHeaders()) }
                fetchAllActivities()
                fetchActivityById(id)
            } catch (e: Exception) {}
        }
    }

    fun resumeActivity(id: Long) {
        viewModelScope.launch {
            try {
                client.put("$baseUrl/activities/$id/resume") { headers.appendAll(authHeaders()) }
                fetchAllActivities()
                fetchActivityById(id)
                runTimer(id)
            } catch (e: Exception) {}
        }
    }

    private fun runTimer(id: Long) {
        timerJobs[id]?.cancel()
        val job = viewModelScope.launch {
            var remaining = _remainingTimes.value[id] ?: 0
            while (remaining > 0) {
                delay(1000)
                remaining--
                _remainingTimes.update { it + (id to remaining) }
            }
            if (remaining == 0) completeActivity(id)
        }
        timerJobs[id] = job
    }

    fun completeActivity(id: Long) {
        viewModelScope.launch {
            try {
                timerJobs[id]?.cancel()
                timerJobs.remove(id)
                _remainingTimes.update { it - id }
                client.put("$baseUrl/activities/$id/complete") { headers.appendAll(authHeaders()) }
                fetchAllActivities()
                fetchActivityById(id)
            } catch (_: Exception) {}
        }
    }

    // ... [Student Assignment Methods] ...
    fun assignStudentToActivity(activityId: Long, studentId: Long, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.post("$baseUrl/activity-students") {
                    headers.appendAll(authHeaders())
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("activityId" to activityId, "studentId" to studentId))
                }
                if (response.status == HttpStatusCode.Created) {
                    onResult(true)
                    loadAssignedStudents(activityId)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun removeStudentFromActivity(activityId: Long, studentId: Long, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.delete("$baseUrl/activity-students") {
                    headers.appendAll(authHeaders())
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("activityId" to activityId, "studentId" to studentId))
                }
                if (response.status == HttpStatusCode.OK) {
                    onResult(true)
                    loadAssignedStudents(activityId)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun loadAssignedStudents(activityId: Long) {
        viewModelScope.launch {
            try {
                val result: List<Student> = client.get("$baseUrl/activity-students/activity/$activityId") {
                    headers.appendAll(authHeaders())
                }.body()
                _assignedStudents.value = result
            } catch (e: Exception) {
                _assignedStudents.value = emptyList()
            }
        }
    }

    fun generateStudentToken(activityId: Long, studentId: Long, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val response: Map<String, String> = client.post("$baseUrl/auth/student-token") {
                    headers.appendAll(authHeaders())
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("activityId" to activityId, "studentId" to studentId))
                }.body()
                onResult(response["token"])
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    // --- UPDATED: Leaderboard Fetch ---

    fun fetchLeaderboard(activityId: Long) {
        _leaderboardError.value = null // Reset error
        viewModelScope.launch {
            try {
                val result: List<LeaderboardEntry> = client.get("$baseUrl/student-activity-results/leaderboard/$activityId") {
                    headers.appendAll(authHeaders())
                }.body()

                println("DEBUG: Leaderboard loaded for $activityId: ${result.size} entries")
                _leaderboard.value = result
            } catch (e: Exception) {
                e.printStackTrace()
                _leaderboard.value = emptyList()
                _leaderboardError.value = "Failed to load leaderboard: ${e.message}"
            }
        }
    }

    fun fetchStudentAnswers(resultId: Long, onResult: (List<StudentAnswer>) -> Unit) {
        viewModelScope.launch {
            try {
                val answers: List<StudentAnswer> = client.get("$baseUrl/student-answers/result/$resultId") {
                    headers.appendAll(authHeaders())
                }.body()
                onResult(answers)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(emptyList())
            }
        }
    }
}