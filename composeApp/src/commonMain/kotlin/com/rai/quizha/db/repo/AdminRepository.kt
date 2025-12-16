package com.rai.quizha.db.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import com.rai.quizha.db.helper.DatabaseHelper
import com.rai.quizha.db.model.Admin as DomainAdmin
import com.rai.quizha.db.Admin as SqlDelightAdmin

class AdminRepository(private val dbHelper: DatabaseHelper) {

    suspend fun insertAdmin(admin: DomainAdmin): Long = withContext(Dispatchers.IO) {
        dbHelper.query {
            adminQueries.insertAdmin(
                username = admin.username,
                password_hash = admin.passwordHash,
                fullname = admin.fullName
            )
            adminQueries.lastInsertRowId().executeAsOne()
        }
    }

    suspend fun deleteAdminById(id: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.query {
            adminQueries.deleteAdmin(id)
            true
        }
    }

    suspend fun getByUsername(username: String): DomainAdmin? = withContext(Dispatchers.IO) {
        dbHelper.query {
            adminQueries.selectAdminByUsername(username)
                .executeAsOneOrNull()
                ?.toDomain()
        }
    }

    fun getAllAdmins(): Flow<List<DomainAdmin>> = flow {
        emit(dbHelper.query {
            adminQueries.selectAllAdmin()
                .executeAsList()
                .map { it.toDomain() }
        })
    }
}

private fun SqlDelightAdmin.toDomain() = DomainAdmin(
    id = id,
    username = username,
    passwordHash = password_hash,
    fullName = fullname
)
