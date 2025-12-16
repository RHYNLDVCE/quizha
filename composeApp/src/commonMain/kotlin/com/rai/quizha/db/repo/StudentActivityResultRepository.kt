package com.rai.quizha.db.repo

import com.rai.quizha.database.QuizhaDB
import com.rai.quizha.db.StudentActivityResultQueries
import com.rai.quizha.db.model.StudentActivityResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import com.rai.quizha.db.Student_Activity_Results as DbStudentActivityResult

// DTO for Repo usage
data class LeaderboardRow(
    val studentId: Long,
    val firstName: String,
    val lastName: String,
    val score: Int?,
    val resultId: Long
)

class StudentActivityResultRepository(
    private val db: QuizhaDB,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val queries: StudentActivityResultQueries = db.studentActivityResultQueries

    private fun DbStudentActivityResult.toDomain(): StudentActivityResult = StudentActivityResult(
        id = id,
        activityStudentId = activity_student_id,
        score = score?.toInt(),
        startedAt = started_at,
        completedAt = completed_at
    )

    suspend fun addStudentActivityResult(result: StudentActivityResult): Long = withContext(ioDispatcher) {
        queries.addStudentActivityResult(
            activity_student_id = result.activityStudentId,
            score = result.score?.toLong(),
            started_at = result.startedAt,
            completed_at = result.completedAt
        ).value as Long
    }

    suspend fun updateStudentActivityResult(result: StudentActivityResult) = withContext(ioDispatcher) {
        queries.updateStudentActivityResult(
            score = result.score?.toLong(),
            completed_at = result.completedAt,
            id = result.id
        ).value
    }

    suspend fun deleteStudentActivityResult(id: Long) = withContext(ioDispatcher) {
        queries.deleteStudentActivityResult(id).value
    }

    suspend fun getStudentActivityResultById(id: Long): StudentActivityResult? = withContext(ioDispatcher) {
        queries.getStudentActivityResultById(id).executeAsOneOrNull()?.toDomain()
    }

    fun getAllStudentActivityResults(): Flow<List<StudentActivityResult>> = flow {
        emit(withContext(ioDispatcher) {
            queries.getAllStudentActivityResults().executeAsList().map { it.toDomain() }
        })
    }

    fun getStudentActivityResultsByEnrollment(activityStudentId: Long): Flow<List<StudentActivityResult>> = flow {
        emit(withContext(ioDispatcher) {
            queries.getStudentActivityResultsByEnrollment(activityStudentId).executeAsList().map { it.toDomain() }
        })
    }

    suspend fun countStudentActivityResults(): Long = withContext(ioDispatcher) {
        queries.countStudentActivityResults().executeAsOne()
    }

    // --- REFACTORED: Direct List Fetch for Routes ---
    suspend fun getLeaderboardList(activityId: Long): List<LeaderboardRow> = withContext(ioDispatcher) {
        queries.getLeaderboard(activityId).executeAsList().map {
            LeaderboardRow(
                studentId = it.student_id,
                firstName = it.first_name,
                lastName = it.last_name,
                score = it.score?.toInt(),
                resultId = it.result_id
            )
        }
    }
}