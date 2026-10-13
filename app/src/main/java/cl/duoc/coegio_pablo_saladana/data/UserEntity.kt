package cl.duoc.coegio_pablo_saladana.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["rut"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firstName: String,
    val middleName: String,
    val paternalSurname: String,
    val maternalSurname: String,
    val rut: String,
    val email: String,
    val role: String,
    val gender: String,
    val passwordHash: String
)
