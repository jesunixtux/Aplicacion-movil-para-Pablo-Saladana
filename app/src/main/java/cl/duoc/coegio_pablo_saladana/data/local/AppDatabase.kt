package cl.duoc.coegio_pablo_saladana.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Base local de Room y punto único de acceso a usuarios, bitácoras y registros. */
@Database(
    entities = [UserEntity::class, NotebookEntity::class, StudentEntryEntity::class],
    version = 11,
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
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11
                ).build().also { instance = it }
            }
        }

        // Mantiene las cuentas existentes al incorporar el campo de género.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE users ADD COLUMN gender TEXT NOT NULL DEFAULT 'Other'"
                )
            }
        }

        // Añade las bitácoras sin eliminar la información de usuarios.
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

        // Añade los registros personales que escriben los estudiantes.
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

        // Incorpora estado de revisión docente y retroalimentación en los registros.
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE student_entries ADD COLUMN isReviewed INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE student_entries ADD COLUMN teacherFeedback TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        // Incorpora sistema de cursos en usuarios/bitácoras y asignación directa de estudiantes.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE users ADD COLUMN course TEXT NOT NULL DEFAULT '1° Medio A'"
                )
                db.execSQL(
                    "ALTER TABLE notebooks ADD COLUMN course TEXT NOT NULL DEFAULT 'General'"
                )
                db.execSQL(
                    "ALTER TABLE notebooks ADD COLUMN assignedStudentEmails TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        // Conserva los datos existentes al avanzar desde la versión anterior.
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notebooks ADD COLUMN remoteId INTEGER")
                db.execSQL("ALTER TABLE student_entries ADD COLUMN remoteId INTEGER")
            }
        }

        // Añade el campo de curso asignado para profesores.
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN assignedCourse TEXT")
            }
        }

        // Conserva los datos existentes mientras añade el mínimo del caso: periodo,
        // asignatura, cierre, evidencias y trazabilidad de revisión.
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN assignedSubject TEXT")
                db.execSQL("UPDATE users SET assignedSubject = 'Lenguaje y Comunicación' WHERE assignedCourse IS NOT NULL")
                // Mantiene compatibles los cursos ficticios de demostraciones anteriores.
                listOf(
                    "1° Medio A" to "5° Básico A",
                    "1° Medio B" to "5° Básico B",
                    "2° Medio A" to "6° Básico A",
                    "2° Medio B" to "6° Básico B",
                    "3° Medio A" to "7° Básico A",
                    "4° Medio A" to "8° Básico A"
                ).forEach { (oldCourse, newCourse) ->
                    db.execSQL("UPDATE users SET course = ? WHERE course = ?", arrayOf(newCourse, oldCourse))
                    db.execSQL("UPDATE users SET assignedCourse = ? WHERE assignedCourse = ?", arrayOf(newCourse, oldCourse))
                    db.execSQL("UPDATE notebooks SET course = ? WHERE course = ?", arrayOf(newCourse, oldCourse))
                }
                db.execSQL("ALTER TABLE notebooks ADD COLUMN subject TEXT NOT NULL DEFAULT 'Lenguaje y Comunicación'")
                db.execSQL("ALTER TABLE notebooks ADD COLUMN month INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE notebooks ADD COLUMN year INTEGER NOT NULL DEFAULT 2026")
                db.execSQL("ALTER TABLE notebooks ADD COLUMN isClosed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notebooks ADD COLUMN publishedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notebooks ADD COLUMN closedAt INTEGER")
                db.execSQL("ALTER TABLE notebooks ADD COLUMN remindersEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE notebooks SET publishedAt = createdAt")
                db.execSQL("ALTER TABLE student_entries ADD COLUMN photoUris TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE student_entries ADD COLUMN fileUris TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE student_entries ADD COLUMN links TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE student_entries ADD COLUMN submittedAt INTEGER")
                db.execSQL("ALTER TABLE student_entries ADD COLUMN reviewedAt INTEGER")
                db.execSQL("ALTER TABLE student_entries ADD COLUMN reviewerEmail TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE student_entries SET submittedAt = createdAt WHERE isDraft = 0")
            }
        }

        // Preparación de sincronización (Tarea 3): solo AÑADE columnas con defaults
        // conservadores. No borra ni modifica datos existentes; updatedAt hereda
        // createdAt para que la futura resolución de conflictos tenga una marca real.
        internal val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notebooks ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
                db.execSQL("ALTER TABLE notebooks ADD COLUMN syncError TEXT")
                db.execSQL("ALTER TABLE notebooks ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE notebooks SET updatedAt = createdAt")
                db.execSQL("ALTER TABLE student_entries ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
                db.execSQL("ALTER TABLE student_entries ADD COLUMN syncError TEXT")
                db.execSQL("ALTER TABLE student_entries ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE student_entries SET updatedAt = createdAt")
            }
        }
    }
}
