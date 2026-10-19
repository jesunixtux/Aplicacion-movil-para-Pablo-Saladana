package cl.duoc.coegio_pablo_saladana.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UserEntity::class, NotebookEntity::class, StudentEntryEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun notebookDao(): NotebookDao
    abstract fun studentEntryDao(): StudentEntryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bitacora.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE users ADD COLUMN gender TEXT NOT NULL DEFAULT 'Other'"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notebooks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        purpose TEXT NOT NULL,
                        instructions TEXT NOT NULL,
                        questions TEXT NOT NULL,
                        teacherEmail TEXT NOT NULL,
                        teacherName TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS student_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        notebookId INTEGER NOT NULL,
                        notebookTitle TEXT NOT NULL,
                        studentEmail TEXT NOT NULL,
                        studentName TEXT NOT NULL,
                        classDate TEXT NOT NULL,
                        content TEXT NOT NULL,
                        activity TEXT NOT NULL,
                        learning TEXT NOT NULL,
                        doubts TEXT NOT NULL,
                        isDraft INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
