package com.rai.quizha.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rai.quizha.frontend.model.Question
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class QuestionsViewModel(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val getToken: () -> String
) : ViewModel() {

    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions

    private val _selectedQuestion = MutableStateFlow<Question?>(null)
    val selectedQuestion: StateFlow<Question?> = _selectedQuestion

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    // TRACKING: Remember which activity we are currently viewing
    // This prevents the "loadAllQuestions" bug when editing inside an activity
    private var currentActivityId: Long? = null

    private fun authHeader(builder: HttpRequestBuilder) {
        val token = getToken()
        println("[QuestionsVM] Token: $token")
        if (token.isNotEmpty()) {
            builder.header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    // --- LOADERS ---

    // ------------------------------------------------------------
    // Load all questions
    // ------------------------------------------------------------
    fun loadAllQuestions() {
        currentActivityId = null // Reset context to "All"
        viewModelScope.launch {
            println("[QuestionsVM] -> GET $baseUrl/questions")

            try {
                val result: List<Question> = httpClient.get("$baseUrl/questions") {
                    authHeader(this)
                }.body()

                println("[QuestionsVM] <- Response: ${result.size} questions received")
                _questions.value = result

            } catch (e: Exception) {
                println("[QuestionsVM] ERROR loadAllQuestions: ${e.message}")
                _message.value = "Failed to load questions"
            }
        }
    }

    // ------------------------------------------------------------
    // Load question by ID
    // ------------------------------------------------------------
    fun loadQuestionById(id: Long) {
        viewModelScope.launch {
            println("[QuestionsVM] -> GET $baseUrl/questions/$id")

            try {
                val result: Question = httpClient.get("$baseUrl/questions/$id") {
                    authHeader(this)
                }.body()

                println("[QuestionsVM] <- Response: $result")
                _selectedQuestion.value = result

            } catch (e: Exception) {
                println("[QuestionsVM] ERROR loadQuestionById: ${e.message}")
                _message.value = "Failed to load question"
            }
        }
    }

    // ------------------------------------------------------------
    // Load questions by Activity ID
    // ------------------------------------------------------------
    fun loadQuestionsByActivityId(activityId: Long) {
        currentActivityId = activityId

        // 1. CLEAR LIST immediately so UI shows "Loading" or "Empty" instead of old data
        _questions.value = emptyList()

        viewModelScope.launch {
            println("[QuestionsVM] -> GET $baseUrl/questions/activity/$activityId")

            try {
                val result: List<Question> = httpClient.get("$baseUrl/questions/activity/$activityId") {
                    authHeader(this)
                }.body()

                println("[QuestionsVM] <- Response: ${result.size} questions")
                _questions.value = result

            } catch (e: Exception) {
                e.printStackTrace() // Print the actual error to console
                println("[QuestionsVM] ERROR: ${e.message}")

                // 2. Ensure list remains empty on error
                _questions.value = emptyList()
                _message.value = "Failed to load: ${e.localizedMessage}"
            }
        }
    }

    // ------------------------------------------------------------
    // Count questions by Activity ID
    // ------------------------------------------------------------
    fun loadQuestionCount(activityId: Long, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            println("[QuestionsVM] -> GET $baseUrl/questions/activity/$activityId/count")

            try {
                val response: Map<String, Int> =
                    httpClient.get("$baseUrl/questions/activity/$activityId/count") {
                        authHeader(this)
                    }.body()

                val count = response["count"] ?: 0
                println("[QuestionsVM] <- Response count: $count")
                onResult(count)

            } catch (e: Exception) {
                println("[QuestionsVM] ERROR loadQuestionCount: ${e.message}")
                onResult(0)
            }
        }
    }

    // --- ACTIONS ---

    // Helper to refresh the list based on what the user was looking at last
    private fun refreshList() {
        if (currentActivityId != null) {
            loadQuestionsByActivityId(currentActivityId!!)
        } else {
            loadAllQuestions()
        }
    }

    // ------------------------------------------------------------
    // Create new question
    // ------------------------------------------------------------
    fun createQuestion(question: Question, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            println("[QuestionsVM] -> POST $baseUrl/questions")
            println("[QuestionsVM] Payload: $question")

            try {
                val response: Map<String, Long> =
                    httpClient.post("$baseUrl/questions") {
                        authHeader(this)
                        contentType(ContentType.Application.Json)
                        setBody(question)
                    }.body()

                val newId = response["id"] ?: 0
                println("[QuestionsVM] <- Created Question ID: $newId")

                onCreated(newId)
                _message.value = "Question created"

                // FIXED: Refresh based on context
                refreshList()

            } catch (e: Exception) {
                println("[QuestionsVM] ERROR createQuestion: ${e.message}")
                _message.value = "Failed to create question"
            }
        }
    }

    // ------------------------------------------------------------
    // Update question
    // ------------------------------------------------------------
    fun updateQuestion(question: Question, onUpdated: () -> Unit) {
        viewModelScope.launch {
            println("[QuestionsVM] -> PUT $baseUrl/questions/${question.id}")
            println("[QuestionsVM] Payload: $question")

            try {
                httpClient.put("$baseUrl/questions/${question.id}") {
                    authHeader(this)
                    contentType(ContentType.Application.Json)
                    setBody(question)
                }

                println("[QuestionsVM] <- Update completed")

                onUpdated()
                _message.value = "Question updated"

                // FIXED: Refresh based on context
                refreshList()

            } catch (e: Exception) {
                println("[QuestionsVM] ERROR updateQuestion: ${e.message}")
                _message.value = "Failed to update question"
            }
        }
    }

    // ------------------------------------------------------------
    // Delete question
    // ------------------------------------------------------------
    fun deleteQuestion(id: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            println("[QuestionsVM] -> DELETE $baseUrl/questions/$id")

            try {
                httpClient.delete("$baseUrl/questions/$id") {
                    authHeader(this)
                }

                println("[QuestionsVM] <- Deleted ID: $id")

                onDeleted()
                _message.value = "Question deleted"

                // FIXED: Refresh based on context
                refreshList()

            } catch (e: Exception) {
                println("[QuestionsVM] ERROR deleteQuestion: ${e.message}")
                _message.value = "Failed to delete question"
            }
        }
    }

    // ------------------------------------------------------------
    // Delete questions by Activity ID
    // ------------------------------------------------------------
    fun deleteQuestionsByActivityId(activityId: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            println("[QuestionsVM] -> DELETE $baseUrl/questions/activity/$activityId")

            try {
                httpClient.delete("$baseUrl/questions/activity/$activityId") {
                    authHeader(this)
                }

                println("[QuestionsVM] <- Deleted questions for activity $activityId")

                onDeleted()
                _message.value = "Activity questions deleted"

                // Since we deleted ALL questions for this activity, the list is now empty.
                // We can just refresh using the ID.
                loadQuestionsByActivityId(activityId)

            } catch (e: Exception) {
                println("[QuestionsVM] ERROR deleteQuestionsByActivityId: ${e.message}")
                _message.value = "Failed to delete activity questions"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}