package cl.duoc.coegio_pablo_saladana.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface NotebookDao {
    @Insert
    suspend fun insert(notebook: NotebookEntity): Long

    @Update
    suspend fun update(notebook: NotebookEntity)

    @Query("SELECT * FROM notebooks ORDER BY createdAt DESC, id DESC")
    suspend fun findAll(): List<NotebookEntity>

    @Query("SELECT * FROM notebooks WHERE remoteOwnerId = :userId OR cachedForUserId = :userId ORDER BY createdAt DESC, id DESC")
    suspend fun findForRemoteUser(userId: Long): List<NotebookEntity>

    @Query("SELECT * FROM notebooks WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): NotebookEntity?

    @Query("SELECT * FROM notebooks WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: Long): NotebookEntity?

    /** Filas pendientes de enviar al backend (nuevas, modificadas o con error). Solo lectura. */
    @Query("SELECT * FROM notebooks WHERE syncState != 'SYNCED' ORDER BY createdAt DESC, id DESC")
    suspend fun findPendingSync(): List<NotebookEntity>

    @Query("SELECT * FROM notebooks WHERE title = :title AND teacherEmail = :teacherEmail LIMIT 1")
    suspend fun findByTitleAndTeacher(title: String, teacherEmail: String): NotebookEntity?

    @Query("SELECT * FROM notebooks WHERE title = :title AND teacherEmail = :teacherEmail AND createdAt = :createdAt LIMIT 1")
    suspend fun findByRemoteIdentity(title: String, teacherEmail: String, createdAt: Long): NotebookEntity?

    @Query(
        """
        UPDATE notebooks
        SET course = :course, assignedStudentEmails = :assignedStudentEmails
        WHERE id = :notebookId AND teacherEmail = :teacherEmail
        """
    )
    suspend fun updateCourseAndStudents(
        notebookId: Long,
        teacherEmail: String,
        course: String,
        assignedStudentEmails: String
    ): Int
}
