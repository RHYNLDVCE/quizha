package com.rai.quizha.db.repo

import com.rai.quizha.database.QuizhaDB
import com.rai.quizha.db.StudentAnswerQueries
import com.rai.quizha.db.model.StudentAnswer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import com.rai.quizha.db.Student_Answer as DbStudentAnswer

class StudentAnswerRepository(
    private val db: QuizhaDB,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val queries: StudentAnswerQueries = db.studentAnswerQueries

    private fun DbStudentAnswer.toDomain(): StudentAnswer = StudentAnswer(
        id = id,
        studentActivityResultId = student_activity_result_id,
        questionId = question_id,
        selectedOption = selected_option
    )

    suspend fun addStudentAnswer(answer: StudentAnswer): Long = withContext(ioDispatcher) {
        queries.addStudentAnswer(
            student_activity_result_id = answer.studentActivityResultId,
            question_id = answer.questionId,
            selected_option = answer.selectedOption
        ).value as Long
    }

    suspend fun updateStudentAnswer(answer: StudentAnswer) = withContext(ioDispatcher) {
        queries.updateStudentAnswer(
            selected_option = answer.selectedOption,
            student_activity_result_id = answer.studentActivityResultId,
            question_id = answer.questionId
        ).value
    }

    suspend fun deleteStudentAnswer(studentActivityResultId: Long, questionId: Long) = withContext(ioDispatcher) {
        queries.deleteStudentAnswer(studentActivityResultId, questionId).value
    }

    fun getAllStudentAnswers(): Flow<List<StudentAnswer>> = flow {
        emit(withContext(ioDispatcher) {
            queries.getAllStudentAnswers().executeAsList().map { it.toDomain() }
        })
    }

    fun getStudentAnswersByResult(studentActivityResultId: Long): Flow<List<StudentAnswer>> = flow {
        emit(withContext(ioDispatcher) {
            queries.getStudentAnswersByResult(studentActivityResultId).executeAsList().map { it.toDomain() }
        })
    }

    fun getStudentAnswersByQuestion(questionId: Long): Flow<List<StudentAnswer>> = flow {
        emit(withContext(ioDispatcher) {
            queries.getStudentAnswersByQuestion(questionId).executeAsList().map { it.toDomain() }
        })
    }

    suspend fun countCorrectAnswersByResult(studentActivityResultId: Long): Long = withContext(ioDispatcher) {
        queries.countCorrectAnswersByResult(studentActivityResultId).executeAsOne()
    }
}
