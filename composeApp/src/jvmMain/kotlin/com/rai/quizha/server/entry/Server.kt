package com.rai.quizha.server.entry

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.rai.quizha.database.QuizhaDB
import com.rai.quizha.db.helper.DatabaseHelper
import com.rai.quizha.db.model.Admin
import com.rai.quizha.db.repo.*
import com.rai.quizha.server.model.JwtConfig
import com.rai.quizha.server.helper.configureAuthentication
import com.rai.quizha.server.helper.configureContentNegotiation
import com.rai.quizha.server.helper.configureCors
import com.rai.quizha.server.helper.configureStatusPages
import com.rai.quizha.server.routes.activityRoutes
import com.rai.quizha.server.routes.activitySocketRoutes
import com.rai.quizha.server.routes.activityStudentRoutes
import com.rai.quizha.server.routes.adminLoginRoute
import com.rai.quizha.server.routes.adminRoutes
import com.rai.quizha.server.routes.authRoutes
import com.rai.quizha.server.routes.questionsRoutes
import com.rai.quizha.server.routes.studentActivityResultRoutes
import com.rai.quizha.server.routes.studentAnswerRoutes
import com.rai.quizha.server.routes.studentRoutes
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import java.security.MessageDigest
import io.ktor.server.websocket.*
import kotlin.time.Duration.Companion.seconds

fun startEmbeddedServer(port: Int, jwtConfig: JwtConfig) {
    embeddedServer(Netty, port = port) {
        configureKtor(jwtConfig)
    }.start(wait = false)

    println("📡 Embedded server running at http://localhost:$port")
}

fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}

fun Application.configureKtor(jwtConfig: JwtConfig) {

    // Plugins
    configureContentNegotiation()
    configureCors()
    configureStatusPages()
    configureAuthentication(jwtConfig)
    configureWebSockets()

    // Database setup
    val dbFile = File("quizha.db")
    val shouldCreateSchema = !dbFile.exists()

    val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.path}")
    driver.execute(null, "PRAGMA foreign_keys = ON;", 0)

    if (shouldCreateSchema) {
        QuizhaDB.Schema.create(driver)
    }
    val db = QuizhaDB(driver)
    val dbHelper = DatabaseHelper(db, Dispatchers.IO)

    // Repositories
    val adminRepo = AdminRepository(dbHelper)
    val studentRepo = StudentRepository(db = db, ioDispatcher = Dispatchers.IO)
    val activityRepo = ActivityRepository(db = db, ioDispatcher = Dispatchers.IO)
    val activityStudentRepo = ActivityStudentRepository(db = db, ioDispatcher = Dispatchers.IO)
    val questionRepo = QuestionsRepository(db = db, ioDispatcher = Dispatchers.IO)
    val resultsRepo = StudentActivityResultRepository(db = db, ioDispatcher = Dispatchers.IO)
    val studentAnswerRepo = StudentAnswerRepository(db = db, ioDispatcher = Dispatchers.IO)

    // Ensure default admin
    runBlocking(Dispatchers.IO) {
        if (adminRepo.getByUsername("admin") == null) {
            val hashedPassword = hashPassword("admin123")
            adminRepo.insertAdmin(
                admin = Admin(
                    id = 0L,
                    username = "admin",
                    passwordHash = hashedPassword,
                    fullName = "Default Admin"
                )
            )
            println("✓ Default admin created (username=admin, password=admin123)")
        } else {
            println("✓ Default admin exists, skipping auto-create")
        }
    }

    // JWT token generator (For Admin Login)
    val generateJwt: (Long, String) -> String = { adminId, username ->
        jwtConfig.generateToken(adminId, username)
    }

    // Routing
    routing {
        get("/") { call.respondText("Quizzy Embedded Server Running") }

        // Public login route
        adminLoginRoute(adminRepo, generateJwt)
        activitySocketRoutes()

        // Questions Routes (Handles its own 'student-auth' and 'auth-jwt' blocks)
        questionsRoutes(questionRepo)
        studentAnswerRoutes(studentAnswerRepo)
        studentActivityResultRoutes(resultsRepo, studentAnswerRepo, activityStudentRepo)
        // JWT-protected routes (Admin Only)
        authenticate("auth-jwt") {
            authRoutes(jwtConfig) // <-- New Token Generation Route

            adminRoutes(adminRepo)
            studentRoutes(studentRepo)
            activityRoutes(activityRepo)
            activityStudentRoutes(activityStudentRepo)

        }
    }
}

private fun hashPassword(plain: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(plain.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}