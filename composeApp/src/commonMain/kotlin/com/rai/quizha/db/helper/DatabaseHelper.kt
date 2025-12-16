package com.rai.quizha.db.helper

import com.rai.quizha.database.QuizhaDB
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class DatabaseHelper(
    private val db: QuizhaDB,
    private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun <T> query(block: QuizhaDB.() -> T): T {
        return withContext(ioDispatcher) {
            block(db)
        }
    }
}