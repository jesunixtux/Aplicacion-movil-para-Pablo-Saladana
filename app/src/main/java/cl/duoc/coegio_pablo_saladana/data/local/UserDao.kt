package cl.duoc.coegio_pablo_saladana.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE rut = :rut LIMIT 1")
    suspend fun findByRut(rut: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'Student' ORDER BY course ASC, firstName ASC, paternalSurname ASC")
    suspend fun findAllStudents(): List<UserEntity>

    @Query("UPDATE users SET passwordHash = :newHash WHERE id = :id AND passwordHash = :oldHash")
    suspend fun updatePasswordHash(id: Long, oldHash: String, newHash: String): Int

    @Query("SELECT * FROM users WHERE role = 'Teacher' AND assignedCourse = :course AND assignedSubject = :subject LIMIT 1")
    suspend fun findTeacherByAssignment(course: String, subject: String): UserEntity?

    @Query("UPDATE users SET assignedCourse = :course, assignedSubject = :subject WHERE email = :email AND role = 'Teacher' AND assignedCourse IS NULL")
    suspend fun updateTeacherAssignment(email: String, course: String, subject: String): Int
}
