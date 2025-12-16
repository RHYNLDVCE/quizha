package com.rai.quizha.db.repo

import com.rai.quizha.database.QuizhaDB
import com.rai.quizha.db.ActivityQueries
import com.rai.quizha.db.model.Activity
import com.rai.quizha.db.Activity as DbActivity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class ActivityRepository(
    private val db: QuizhaDB,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val queries: ActivityQueries = db.activityQueries

    private fun DbActivity.toDomain(): Activity = Activity(
        id = id,
        title = title,
        timeduration = timeduration,
        status = status,
        createdAt = created_at,
        updatedAt = updated_at
    )


    suspend fun insertActivity(activity: Activity): Long = withContext(ioDispatcher) {
        db.transactionWithResult {
            // Perform the insert (returns Unit)
            queries.addActivity(
                title = activity.title,
                timeduration = activity.timeduration,
                status = "pending",
                created_at = activity.createdAt,
                updated_at = activity.updatedAt
            )
            // Return the last inserted row ID
            queries.getLastInsertActivity().executeAsOne()
        }
    }



    suspend fun startActivity(id: Long) = withContext(ioDispatcher) {
        queries.updateActivityStatus(
            id = id,
            status = "ongoing",
            updated_at = System.currentTimeMillis().toString()
        )
    }


    suspend fun markActivityCompleted(id: Long) = withContext(ioDispatcher) {
        queries.updateActivityStatus(
            id = id,
            status = "completed",
            updated_at = System.currentTimeMillis().toString()
        )
    }


    suspend fun updateActivity(activity: Activity) = withContext(ioDispatcher) {
        queries.updateActivity(
            title = activity.title,
            timeduration = activity.timeduration,
            status = activity.status,
            updated_at = activity.updatedAt,
            id = activity.id
        )
    }

    suspend fun deleteActivity(id: Long) = withContext(ioDispatcher) {
        queries.deleteActivity(id)
    }

    suspend fun getActivityById(id: Long): Activity? = withContext(ioDispatcher) {
        queries.getActivityById(id).executeAsOneOrNull()?.toDomain()
    }

    fun getAllActivities(): Flow<List<Activity>> = flow {
        emit(withContext(ioDispatcher) {
            queries.getAllActivities().executeAsList().map { it.toDomain() }
        })
    }

    fun getAllActivitiesByCreatedDesc(): Flow<List<Activity>> = flow {
        emit(withContext(ioDispatcher) {
            queries.getAllActivitiesByCreatedDesc().executeAsList().map { it.toDomain() }
        })
    }

    fun getAllActivitiesByCreatedAsc(): Flow<List<Activity>> = flow {
        emit(withContext(ioDispatcher) {
            queries.getAllActivitiesByCreatedAsc().executeAsList().map { it.toDomain() }
        })
    }

    fun getActivitiesByStatus(status: String): Flow<List<Activity>> = flow {
        emit(withContext(ioDispatcher) {
            queries.getActivitiesByStatus(status).executeAsList().map { it.toDomain() }
        })
    }

    suspend fun countActivities(): Long = withContext(ioDispatcher) {
        queries.countActivities().executeAsOne()
    }

    suspend fun countActivitiesByStatus(status: String): Long = withContext(ioDispatcher) {
        queries.countActivitiesByStatus(status).executeAsOne()
    }
}