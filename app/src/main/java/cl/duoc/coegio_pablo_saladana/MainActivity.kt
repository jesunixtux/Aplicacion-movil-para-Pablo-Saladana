package cl.duoc.coegio_pablo_saladana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberPlatformOverscrollFactory
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import cl.duoc.coegio_pablo_saladana.data.local.ProfilePhotoStorage
import cl.duoc.coegio_pablo_saladana.data.repository.NotebookRepository
import cl.duoc.coegio_pablo_saladana.data.repository.StudentEntryRepository
import cl.duoc.coegio_pablo_saladana.data.repository.UserRepository
import cl.duoc.coegio_pablo_saladana.ui.AppViewModel
import cl.duoc.coegio_pablo_saladana.ui.auth.AuthScreens
import cl.duoc.coegio_pablo_saladana.ui.auth.AuthViewModel
import cl.duoc.coegio_pablo_saladana.ui.navigation.AuthDestination
import cl.duoc.coegio_pablo_saladana.ui.theme.CoegioPabloSaladanaTheme

/** Monta el contenedor Compose y delega el estado y la lógica de sesión a los ViewModel. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userRepository = UserRepository(applicationContext)
        val notebookRepository = NotebookRepository(applicationContext)
        val studentEntryRepository = StudentEntryRepository(applicationContext)
        val profilePhotoStorage = ProfilePhotoStorage(applicationContext)

        val authViewModel = ViewModelProvider(
            this,
            AuthViewModel.factory(userRepository)
        )[AuthViewModel::class.java]

        val appViewModel = ViewModelProvider(
            this,
            AppViewModel.factory(
                appContext = applicationContext,
                userRepository = userRepository,
                notebookRepository = notebookRepository,
                studentEntryRepository = studentEntryRepository,
                profilePhotoStorage = profilePhotoStorage
            )
        )[AppViewModel::class.java]

        setContent {
            CoegioPabloSaladanaTheme {
                val overscrollFactory = rememberPlatformOverscrollFactory(
                    glowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                )
                val state = appViewModel.uiState
                var savedDestination by rememberSaveable { mutableStateOf(state.destination) }
                var savedNotebookId by rememberSaveable { mutableStateOf(state.selectedNotebookId) }
                var savedEditingEntryId by rememberSaveable { mutableStateOf(state.editingEntryId) }

                LaunchedEffect(Unit) {
                    appViewModel.initialize(
                        savedDestination = if (savedInstanceState == null) null else savedDestination,
                        savedNotebookId = if (savedInstanceState == null) null else savedNotebookId,
                        savedEditingEntryId = if (savedInstanceState == null) null else savedEditingEntryId
                    )
                }

                LaunchedEffect(state.destination, state.selectedNotebookId, state.editingEntryId) {
                    if (!state.isRestoringSession) {
                        savedDestination = state.destination
                        savedNotebookId = state.selectedNotebookId
                        savedEditingEntryId = state.editingEntryId
                    }
                }

                BackHandler(
                    enabled = !state.isRestoringSession && state.destination != AuthDestination.Welcome,
                    onBack = appViewModel::handleBack
                )

                CompositionLocalProvider(LocalOverscrollFactory provides overscrollFactory) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        if (state.isRestoringSession) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            AuthScreens(
                                destination = state.destination,
                                authViewModel = authViewModel,
                                currentAccount = state.currentAccount,
                                notebooks = state.notebooks,
                                availableStudents = state.availableStudents,
                                selectedNotebook = state.selectedNotebook,
                                editingEntry = state.editingEntry,
                                studentEntries = state.studentEntries,
                                teacherEntries = state.teacherEntries,
                                holidays = state.holidays,
                                profilePhotoUri = state.profilePhotoUri,
                                onDestinationChange = appViewModel::navigateTo,
                                onUserLoggedIn = appViewModel::onUserLoggedIn,
                                onNotebookSelected = appViewModel::onNotebookSelected,
                                onStartNewEntry = appViewModel::onStartNewEntry,
                                onEditDraftEntry = appViewModel::onEditDraftEntry,
                                onNotebookPublished = appViewModel::publishNotebook,
                                onAssignCourse = appViewModel::assignCourse,
                                onUpdateNotebookAssignments = appViewModel::updateNotebookAssignments,
                                onCloseNotebook = appViewModel::closeNotebook,
                                onSetNotebookReminders = appViewModel::setNotebookReminders,
                                onStudentEntrySaved = appViewModel::saveStudentEntry,
                                onReviewEntry = appViewModel::reviewEntry,
                                onPhotoEdited = appViewModel::updateProfilePhoto,
                                onUserLoggedOut = appViewModel::logout
                            )
                        }
                    }
                }
            }
        }
    }
}
