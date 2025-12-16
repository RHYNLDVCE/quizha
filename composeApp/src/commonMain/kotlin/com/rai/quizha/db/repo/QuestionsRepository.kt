package com.rai.quizha.db.repo

import com.rai.quizha.database.QuizhaDB
import com.rai.quizha.db.QuestionsQueries
import com.rai.quizha.db.model.Question
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import com.rai.quizha.db.Questions as DbQuestion

class QuestionsRepository(
    private val db: QuizhaDB,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val queries: QuestionsQueries = db.questionsQueries

    private fun DbQuestion.toDomain(): Question = Question(
        id = id,
        questionText = question_text,
        optionA = option_a,
        optionB = option_b,
        optionC = option_c,
        optionD = option_d,
        correctOption = correct_option,
        activityId = activity_id
    )

    suspend fun addQuestion(question: Question): Long = withContext(ioDispatcher) {
        queries.addQuestion(
            activity_id = question.activityId,
            question_text = question.questionText,
            option_a = question.optionA,
            option_b = question.optionB,
            option_c = question.optionC,
            option_d = question.optionD,
            correct_option = question.correctOption
        )
        queries.getLastInsertQuestion().executeAsOne()
    }


    suspend fun updateQuestion(question: Question) = withContext(ioDispatcher) {
        queries.updateQuestion(
            activity_id = question.activityId,
            question_text = question.questionText,
            option_a = question.optionA,
            option_b = question.optionB,
            option_c = question.optionC,
            option_d = question.optionD,
            correct_option = question.correctOption,
            id = question.id
        )
    }

    suspend fun deleteQuestion(id: Long) = withContext(ioDispatcher) {
        queries.deleteQuestion(id)
    }

    suspend fun deleteQuestionsByActivityId(activityId: Long) = withContext(ioDispatcher) {
        queries.deleteQuestionsByActivityId(activityId)
    }

    fun getAllQuestions(): Flow<List<Question>> = flow {
        emit(withContext(ioDispatcher) {
            queries.getAllQuestions().executeAsList().map { it.toDomain() }
        })
    }

    suspend fun getQuestionById(id: Long): Question? = withContext(ioDispatcher) {
        queries.getQuestionById(id).executeAsOneOrNull()?.toDomain()
    }

    fun getQuestionsByActivityId(activityId: Long): Flow<List<Question>> = flow {
        emit(withContext(ioDispatcher) {
            queries.getQuestionsByActivityId(activityId).executeAsList().map { it.toDomain() }
        })
    }

    suspend fun countQuestionsByActivityId(activityId: Long): Long = withContext(ioDispatcher) {
        queries.countQuestionsByActivityId(activityId).executeAsOne()
    }
}
