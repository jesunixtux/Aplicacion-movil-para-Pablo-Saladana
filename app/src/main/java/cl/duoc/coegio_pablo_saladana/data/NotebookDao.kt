package cl.duoc.coegio_pablo_saladana.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface NotebookDao {
    @Insert
    suspend fun insert(notebook: NotebookEntity): Long

    @Query("SELECT * FROM notebooks ORDER BY createdAt DESC, id DESC")
    suspend fun findAll(): List<NotebookEntity>

    @Query("SELECT * FROM notebooks WHERE title = :title AND teacherEmail = :teacherEmail LIMIT 1")
    suspend fun findByTitleAndTeacher(title: String, teacherEmail: String): NotebookEntity?
}
