package cl.duoc.coegio_pablo_saladana

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.rememberPlatformOverscrollFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cl.duoc.coegio_pablo_saladana.data.AuthViewModel
import cl.duoc.coegio_pablo_saladana.data.ProfilePhotoStorage
import cl.duoc.coegio_pablo_saladana.data.UserAccount
import cl.duoc.coegio_pablo_saladana.data.UserRepository
import cl.duoc.coegio_pablo_saladana.data.UserRole
import cl.duoc.coegio_pablo_saladana.ui.auth.AuthDestination
import cl.duoc.coegio_pablo_saladana.ui.auth.AuthScreens
import cl.duoc.coegio_pablo_saladana.ui.theme.CoegioPabloSaladanaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userRepository = UserRepository(applicationContext)
        val profilePhotoStorage = ProfilePhotoStorage(applicationContext)

        setContent {
            CoegioPabloSaladanaTheme {
                val overscrollFactory = rememberPlatformOverscrollFactory(
                    glowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                )
                val authViewModel = remember { AuthViewModel(userRepository) }
                var destination by remember { mutableStateOf(AuthDestination.Welcome) }
                var currentAccount by remember { mutableStateOf<UserAccount?>(null) }
                var profilePhotoUri by remember { mutableStateOf<Uri?>(null) }
                var isRestoringSession by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    val restoredSession = authViewModel.restoreSession()
                    currentAccount = restoredSession
                    profilePhotoUri = restoredSession?.let { profilePhotoStorage.uriFor(it.email) }
                    destination = when (restoredSession?.role) {
                        UserRole.Teacher -> AuthDestination.TeacherDashboard
                        UserRole.Student -> AuthDestination.StudentDashboard
                        null -> AuthDestination.Welcome
                    }
                    isRestoringSession = false
                }

                BackHandler(enabled = !isRestoringSession && destination != AuthDestination.Welcome) {
                    destination = when (destination) {
                        AuthDestination.Login, AuthDestination.Register -> AuthDestination.Welcome
                        AuthDestination.StudentNotebook, AuthDestination.StudentHistory -> AuthDestination.StudentDashboard
                        AuthDestination.StudentEntry -> AuthDestination.StudentNotebook
                        AuthDestination.TeacherCreateNotebook, AuthDestination.TeacherReviewEntries -> AuthDestination.TeacherDashboard
                        AuthDestination.Profile -> if (currentAccount?.role == cl.duoc.coegio_pablo_saladana.data.UserRole.Teacher) {
                            AuthDestination.TeacherDashboard
                        } else {
                            AuthDestination.StudentDashboard
                        }
                        else -> AuthDestination.Welcome
                    }
                }

                CompositionLocalProvider(LocalOverscrollFactory provides overscrollFactory) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        if (isRestoringSession) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            AuthScreens(
                                destination = destination,
                                authViewModel = authViewModel,
                                currentAccount = currentAccount,
                                profilePhotoUri = profilePhotoUri,
                                onDestinationChange = { destination = it },
                                onUserLoggedIn = {
                                    currentAccount = it
                                    authViewModel.saveSession(it)
                                    profilePhotoUri = profilePhotoStorage.uriFor(it.email)
                                },
                                onPhotoEdited = { bitmap: Bitmap ->
                                    currentAccount?.let { account ->
                                        profilePhotoStorage.save(account.email, bitmap)?.let { savedUri ->
                                            profilePhotoUri = savedUri
                                        }
                                    }
                                },
                                onUserLoggedOut = {
                                    authViewModel.clearSession()
                                    currentAccount = null
                                    profilePhotoUri = null
                                    destination = AuthDestination.Welcome
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
