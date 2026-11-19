package cl.duoc.coegio_pablo_saladana.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface StudentEntryDao {
    @Insert
    suspend fun insert(entry: StudentEntryEntity): Long

    @Update
    suspend fun update(entry: StudentEntryEntity)

    @Query("SELECT * FROM student_entries WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: Long): StudentEntryEntity?

    /** Filas pendientes de enviar al backend (nuevas, modificadas o con error). Solo lectura. */
    @Query("SELECT * FROM student_entries WHERE syncState != 'SYNCED' ORDER BY createdAt DESC, id DESC")
    suspend fun findPendingSync(): List<StudentEntryEntity>

    @Query("SELECT * FROM student_entries WHERE studentEmail = :email ORDER BY createdAt DESC, id DESC")
    suspend fun findByStudent(email: String): List<StudentEntryEntity>

    @Query(
        """
        SELECT student_entries.* FROM student_entries
        INNER JOIN notebooks ON notebooks.id = student_entries.notebookId
        WHERE notebooks.teacherEmail = :email AND student_entries.isDraft = 0
        ORDER BY student_entries.createdAt DESC, student_entries.id DESC
        """
    )
    suspend fun findSubmittedByTeacher(email: String): List<StudentEntryEntity>

    @Query("SELECT * FROM student_entries WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): StudentEntryEntity?

    @Query(
        """
        UPDATE student_entries
        SET classDate = :classDate, content = :content, activity = :activity,
            learning = :learning, doubts = :doubts, isDraft = :isDraft
        WHERE id = :id AND studentEmail = :studentEmail AND isDraft = 1
        """
    )
    suspend fun updateDraft(
        id: Long,
        studentEmail: String,
        classDate: String,
        content: String,
        activity: String,
        learning: String,
        doubts: String,
        isDraft: Boolean
    ): Int

    @Query(
        """
        UPDATE student_entries
        SET isReviewed = 1, teacherFeedback = :feedback
        WHERE id = :id AND isDraft = 0 AND notebookId IN (
            SELECT id FROM notebooks WHERE teacherEmail = :teacherEmail
        )
        """
    )
    suspend fun markReviewed(id: Long, teacherEmail: String, feedback: String): Int
}
