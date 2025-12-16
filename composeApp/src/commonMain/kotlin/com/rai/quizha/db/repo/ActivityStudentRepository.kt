package com.rai.quizha.db.repo

import com.rai.quizha.database.QuizhaDB
import com.rai.quizha.db.ActivityQueries
import com.rai.quizha.db.ActivityStudentsQueries
import com.rai.quizha.db.Activity as DbActivity
import com.rai.quizha.db.model.Activity
import com.rai.quizha.db.model.Student
import com.rai.quizha.db.Students as DbStudent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class ActivityStudentRepository(
    private val db: QuizhaDB,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val activityStudentQueries: ActivityStudentsQueries = db.activityStudentsQueries
    private val activityQueries: ActivityQueries = db.activityQueries

    private fun DbStudent.toDomain(): Student = Student(
        id = id,
        firstName = first_name,
        lastName = last_name,
        yearlevel = yearlevel,
        department = department,
        course = course,
        birthdate = birthdate
    )

    private fun DbActivity.toDomain(): Activity = Activity(
        id = id,
        title = title,
        timeduration = timeduration,
        status = status,
        createdAt = created_at,
        updatedAt = updated_at
    )

    suspend fun addStudentToActivity(activityId: Long, studentId: Long): Long = withContext(ioDispatcher) {
        activityStudentQueries.addStudentToActivity(activityId, studentId)
        activityStudentQueries.getLastInsertActivityStudent().executeAsOne()
    }

    suspend fun removeStudentFromActivity(activityId: Long, studentId: Long) = withContext(ioDispatcher) {
        activityStudentQueries.removeStudentFromActivity(activityId, studentId)
    }

    fun getStudentsByActivity(activityId: Long): Flow<List<Student>> = flow {
        emit(withContext(ioDispatcher) {
            activityStudentQueries.getStudentsByActivity(activityId).executeAsList().map { it.toDomain() }
        })
    }



    suspend fun getIdByActivityAndStudent(activityId: Long, studentId: Long): Long? = withContext(ioDispatcher) {
        activityStudentQueries.getIdByActivityAndStudent(activityId, studentId).executeAsOneOrNull()
    }
    fun getActivitiesByStudent(studentId: Long): Flow<List<Activity>> = flow {
        emit(withContext(ioDispatcher) {
            activityStudentQueries.getActivitiesByStudent(studentId).executeAsList().map { it.toDomain() }
        })
    }
}
