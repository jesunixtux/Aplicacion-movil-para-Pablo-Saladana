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
}
