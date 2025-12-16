package com.rai.quizha.server.routes

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

// Thread-safe map: ActivityID -> Set of Sessions
val activitySessions = ConcurrentHashMap<Long, MutableSet<DefaultWebSocketSession>>()

fun Route.activitySocketRoutes() {
    // Mobile clients connect here: ws://ip:port/ws/activity/{id}
    webSocket("/ws/activity/{id}") {
        val id = call.parameters["id"]?.toLongOrNull()

        if (id == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid ID"))
            return@webSocket
        }

        // 1. Use newKeySet for safer concurrency (prevents crashes during broadcast)
        val sessions = activitySessions.computeIfAbsent(id) {
            ConcurrentHashMap.newKeySet()
        }
        sessions.add(this)

        try {
            // Keep connection alive
            for (frame in incoming) {
                // Listen for messages (optional)
            }
        } finally {
            // 2. Cleanup when they disconnect
            sessions.remove(this)

            // Optional: Remove the empty set to save memory if no one is connected
            if (sessions.isEmpty()) {
                activitySessions.remove(id, sessions)
            }
        }
    }
}

// Call this function from your Admin PUT routes (Pause/Resume/Start)
suspend fun broadcastActivityStatus(activityId: Long, status: String) {
    val sessions = activitySessions[activityId] ?: return
    val message = "STATUS_UPDATE:$status"

    // This iteration is now safe even if a student disconnects mid-loop
    sessions.forEach { session ->
        try {
            session.send(Frame.Text(message))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}