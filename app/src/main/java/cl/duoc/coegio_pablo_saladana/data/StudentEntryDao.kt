package cl.duoc.coegio_pablo_saladana.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface StudentEntryDao {
    @Insert
    suspend fun insert(entry: StudentEntryEntity): Long

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
}
