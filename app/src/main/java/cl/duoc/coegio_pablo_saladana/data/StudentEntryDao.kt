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
}
