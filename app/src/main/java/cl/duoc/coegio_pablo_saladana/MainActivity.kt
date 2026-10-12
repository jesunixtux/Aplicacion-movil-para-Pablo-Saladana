package cl.duoc.coegio_pablo_saladana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cl.duoc.coegio_pablo_saladana.data.UserRepository
import cl.duoc.coegio_pablo_saladana.ui.auth.AuthDestination
import cl.duoc.coegio_pablo_saladana.ui.auth.AuthScreens
import cl.duoc.coegio_pablo_saladana.ui.theme.CoegioPabloSaladanaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userRepository = UserRepository(applicationContext)

        setContent {
            CoegioPabloSaladanaTheme {
                var destination by remember { mutableStateOf(AuthDestination.Welcome) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    AuthScreens(
                        destination = destination,
                        userRepository = userRepository,
                        onDestinationChange = { destination = it }
                    )
                }
            }
        }
    }
}
