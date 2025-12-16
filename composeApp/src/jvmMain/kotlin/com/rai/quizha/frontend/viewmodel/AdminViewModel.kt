package com.rai.quizha.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rai.quizha.frontend.model.Admin
import com.rai.quizha.frontend.model.LoginRequest
import com.rai.quizha.frontend.model.LoginResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminViewModel(private val client: HttpClient, private val baseUrl: String) : ViewModel() {

    private val _admins = MutableStateFlow<List<Admin>>(emptyList())
    val admins: StateFlow<List<Admin>> = _admins

    private val _loginResult = MutableStateFlow<String?>(null) // JWT token
    val loginResult: StateFlow<String?> = _loginResult

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                val response = client.post("$baseUrl/admins/login") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(username, password))  // plain text password
                }


                when (response.status) {
                    HttpStatusCode.OK -> {
                        val loginResponse: LoginResponse = response.body() // deserialize only on 200
                        _loginResult.value = loginResponse.token
                        _error.value = null
                    }
                    HttpStatusCode.Unauthorized -> {
                        _error.value = "Invalid username or password"
                        _loginResult.value = null
                    }
                    else -> {
                        _error.value = "Unexpected error: ${response.status}"
                        _loginResult.value = null
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
                _loginResult.value = null
            }
        }
    }


    fun fetchAdmins() {
        viewModelScope.launch {
            try {
                val response: List<Admin> = client.get("$baseUrl/admins").body()
                _admins.value = response
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun createAdmin(admin: Admin) {
        viewModelScope.launch {
            try {
                client.post("$baseUrl/admins") {
                    contentType(ContentType.Application.Json)
                    setBody(admin)
                }
                fetchAdmins()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteAdmin(id: Long) {
        viewModelScope.launch {
            try {
                client.delete("$baseUrl/admins/$id")
                fetchAdmins()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
