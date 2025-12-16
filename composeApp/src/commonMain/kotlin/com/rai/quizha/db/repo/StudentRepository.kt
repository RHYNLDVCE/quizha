package com.rai.quizha.db.repo

import app.cash.sqldelight.coroutines.asFlow
import com.rai.quizha.database.QuizhaDB
import com.rai.quizha.db.Students as DbStudent
import com.rai.quizha.db.StudentsQueries
import com.rai.quizha.db.model.Student
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class StudentRepository(
    private val db: QuizhaDB,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val queries: StudentsQueries = db.studentsQueries

    private fun DbStudent.toDomain(): Student = Student(
        id = id,
        firstName = first_name,
        lastName = last_name,
        yearlevel = yearlevel,
        department = department,
        course = course,
        birthdate = birthdate
    )

    suspend fun insertStudent(student: Student): Long = withContext(ioDispatcher) {
        queries.addStudent(
            first_name = student.firstName,
            last_name = student.lastName,
            yearlevel = student.yearlevel,
            department = student.department,
            course = student.course,
            birthdate = student.birthdate
        )
        queries.getLastInsertStudent().executeAsOne()
    }

    suspend fun updateStudent(student: Student) = withContext(ioDispatcher) {
        queries.updateStudent(
            first_name = student.firstName,
            last_name = student.lastName,
            yearlevel = student.yearlevel,
            department = student.department,
            course = student.course,
            birthdate = student.birthdate,
            id = student.id
        )
    }

    suspend fun deleteStudent(id: Long) = withContext(ioDispatcher) {
        queries.deleteStudent(id)
    }

    suspend fun getStudentById(id: Long): Student? = withContext(ioDispatcher) {
        queries.getStudentById(id).executeAsOneOrNull()?.toDomain()
    }

    fun getAllStudents(): Flow<List<Student>> =
        queries.getAllStudents().asFlow().map { it.executeAsList().map { s -> s.toDomain() } }

    fun getStudentsByYearlevel(yearlevel: String): Flow<List<Student>> =
        queries.getStudentsByYearlevel(yearlevel).asFlow().map { it.executeAsList().map { s -> s.toDomain() } }

    fun getStudentsByDepartment(department: String): Flow<List<Student>> =
        queries.getStudentsByDepartment(department).asFlow().map { it.executeAsList().map { s -> s.toDomain() } }

    fun searchStudentsByName(name: String): Flow<List<Student>> {
        val pattern = "%$name%"
        return queries.searchStudentsByName(pattern, pattern).asFlow().map { it.executeAsList().map { s -> s.toDomain() } }
    }

    // ----------------- SUSPENDING VERSIONS FOR ROUTES -----------------
    suspend fun getAllStudentsList(): List<Student> = getAllStudents().first()

    suspend fun getStudentsByYearlevelList(yearlevel: String): List<Student> =
        getStudentsByYearlevel(yearlevel).first()

    suspend fun getStudentsByDepartmentList(department: String): List<Student> =
        getStudentsByDepartment(department).first()

    suspend fun searchStudentsByNameList(name: String): List<Student> =
        searchStudentsByName(name).first()
}
