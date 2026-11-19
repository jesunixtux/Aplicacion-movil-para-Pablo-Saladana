package cl.duoc.coegio_pablo_saladana

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.duoc.coegio_pablo_saladana.data.local.AppDatabase
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifica la migración 10→11 (metadatos de sincronización) y la instalación nueva.
 * Requiere emulador/dispositivo: ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class Migration10To11Test {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val testDbName = "migration-10-11-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(testDbName)
        context.deleteDatabase("fresh-v11-test.db")
    }

    @Test
    fun migrationAddsSyncColumnsWithoutLosingExistingRows() {
        context.deleteDatabase(testDbName)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(testDbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(10) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS notebooks (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                title TEXT NOT NULL, purpose TEXT NOT NULL,
                                instructions TEXT NOT NULL, questions TEXT NOT NULL,
                                teacherEmail TEXT NOT NULL, teacherName TEXT NOT NULL,
                                createdAt INTEGER NOT NULL, course TEXT NOT NULL DEFAULT 'General',
                                assignedStudentEmails TEXT NOT NULL DEFAULT '', remoteId INTEGER,
                                subject TEXT NOT NULL DEFAULT 'Lenguaje y Comunicación',
                                month INTEGER NOT NULL DEFAULT 1, year INTEGER NOT NULL DEFAULT 2026,
                                isClosed INTEGER NOT NULL DEFAULT 0, publishedAt INTEGER NOT NULL DEFAULT 0,
                                closedAt INTEGER, remindersEnabled INTEGER NOT NULL DEFAULT 1
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS student_entries (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                notebookId INTEGER NOT NULL, notebookTitle TEXT NOT NULL,
                                studentEmail TEXT NOT NULL, studentName TEXT NOT NULL,
                                classDate TEXT NOT NULL, content TEXT NOT NULL,
                                activity TEXT NOT NULL, learning TEXT NOT NULL, doubts TEXT NOT NULL,
                                isDraft INTEGER NOT NULL, createdAt INTEGER NOT NULL,
                                isReviewed INTEGER NOT NULL DEFAULT 0, teacherFeedback TEXT NOT NULL DEFAULT '',
                                remoteId INTEGER, photoUris TEXT NOT NULL DEFAULT '',
                                fileUris TEXT NOT NULL DEFAULT '', links TEXT NOT NULL DEFAULT '',
                                submittedAt INTEGER, reviewedAt INTEGER, reviewerEmail TEXT NOT NULL DEFAULT ''
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            "INSERT INTO notebooks (id, title, purpose, instructions, questions, teacherEmail, teacherName, createdAt, course, publishedAt) " +
                                "VALUES (1, 'Bitácora existente', 'P', 'I', '', 'd@e.cl', 'D', 1700000000000, '5° Básico A', 1700000000000)"
                        )
                        db.execSQL(
                            "INSERT INTO student_entries (id, notebookId, notebookTitle, studentEmail, studentName, classDate, content, activity, learning, doubts, isDraft, createdAt, submittedAt) " +
                                "VALUES (1, 1, 'Bitácora existente', 'a@e.cl', 'A', '18/10/2026', 'C', 'A', 'L', '', 0, 1700000000001, 1700000000001)"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

        val db = helper.writableDatabase
        AppDatabase.MIGRATION_10_11.migrate(db)

        db.query("SELECT title, syncState, syncError, updatedAt FROM notebooks WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Bitácora existente", cursor.getString(0))
            assertEquals("LOCAL_ONLY", cursor.getString(1))
            assertTrue(cursor.isNull(2))
            assertEquals(1700000000000L, cursor.getLong(3)) // updatedAt hereda createdAt
        }
        db.query("SELECT content, syncState, updatedAt FROM student_entries WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("C", cursor.getString(0))
            assertEquals("LOCAL_ONLY", cursor.getString(1))
            assertEquals(1700000000001L, cursor.getLong(2))
        }
        helper.close()
    }

    @Test
    fun freshInstallStartsLocalOnlyAndPendingSyncQueriesWork() = runBlocking {
        val database = Room.databaseBuilder(context, AppDatabase::class.java, "fresh-v11-test.db").build()
        try {
            val notebookId = database.notebookDao().insert(
                NotebookEntity(
                    title = "Nueva", purpose = "P", instructions = "I", questions = "",
                    teacherEmail = "d@e.cl", teacherName = "D", createdAt = 1L
                )
            )
            val entryId = database.studentEntryDao().insert(
                StudentEntryEntity(
                    notebookId = notebookId, notebookTitle = "Nueva", studentEmail = "a@e.cl",
                    studentName = "A", classDate = "18/10/2026", content = "C", activity = "A",
                    learning = "L", doubts = "", isDraft = true, createdAt = 2L
                )
            )

            val notebook = database.notebookDao().findById(notebookId)!!
            assertEquals("LOCAL_ONLY", notebook.syncState)
            assertEquals(0L, notebook.updatedAt)
            assertEquals(listOf(notebookId), database.notebookDao().findPendingSync().map { it.id })

            val entry = database.studentEntryDao().findById(entryId)!!
            assertEquals("LOCAL_ONLY", entry.syncState)
            assertEquals(listOf(entryId), database.studentEntryDao().findPendingSync().map { it.id })

            // Los datos sobreviven al cierre y reapertura de la base.
            database.close()
            val reopened = Room.databaseBuilder(context, AppDatabase::class.java, "fresh-v11-test.db").build()
            assertEquals("Nueva", reopened.notebookDao().findById(notebookId)!!.title)
            assertEquals("C", reopened.studentEntryDao().findById(entryId)!!.content)
            reopened.close()
        } finally {
            if (database.isOpen) database.close()
        }
    }
}
