package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.auth.AuthRepository
import com.example.auth.AuthState
import com.example.auth.AuthViewModel
import com.example.data.db.TempleMapDatabase
import com.example.data.repository.TempleRepository
import com.example.sync.DriveSyncManager
import com.example.ui.auth.AuthScreen
import com.example.ui.home.AuthenticatedHomeScreen
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.theme.TravelTempleTheme
import com.example.ui.viewmodel.TempleViewModel

class MainActivity : ComponentActivity() {

    private val authRepository by lazy { AuthRepository(applicationContext) }
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModel.Factory(authRepository)
    }

    private val templeDatabase by lazy {
        TempleMapDatabase.getDatabase(applicationContext, lifecycleScope)
    }

    private val templeRepository by lazy {
        TempleRepository(
            placeDao = templeDatabase.placeDao(),
            categoryDao = templeDatabase.categoryDao(),
            tripDao = templeDatabase.tripDao(),
            visitDao = templeDatabase.visitDao(),
            mediaLinkDao = templeDatabase.mediaLinkDao()
        )
    }

    private val driveSyncManager by lazy {
        DriveSyncManager(applicationContext, templeRepository)
    }

    private val templeViewModel: TempleViewModel by viewModels {
        TempleViewModel.Factory(applicationContext, templeRepository, driveSyncManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by templeViewModel.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                com.example.ui.viewmodel.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                com.example.ui.viewmodel.ThemeMode.DARK -> true
                com.example.ui.viewmodel.ThemeMode.LIGHT -> false
            }

            TravelTempleTheme(darkTheme = isDark) {
                val authState by authViewModel.authState.collectAsStateWithLifecycle()
                val isSigningIn by authViewModel.isSigningIn.collectAsStateWithLifecycle()
                val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()

                Crossfade(targetState = authState, label = "AuthCrossfade") { state ->
                    when (state) {
                        is AuthState.Loading -> {
                            LoadingSplashScreen()
                        }
                        is AuthState.Unauthenticated -> {
                            AuthScreen(
                                viewModel = authViewModel,
                                isSigningIn = isSigningIn,
                                errorMessage = errorMessage
                            )
                        }
                        is AuthState.Authenticated -> {
                            // Sync once on launch with Google Drive as requested by user
                            LaunchedEffect(state.user.id) {
                                templeViewModel.syncWithDriveOnLaunch(state.user.email)
                            }

                            AuthenticatedHomeScreen(
                                user = state.user,
                                templeViewModel = templeViewModel,
                                onSignOut = { authViewModel.signOut() }
                            )
                        }
                        is AuthState.Error -> {
                            AuthScreen(
                                viewModel = authViewModel,
                                isSigningIn = isSigningIn,
                                errorMessage = state.message
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = TerracottaPrimary,
            modifier = Modifier.size(44.dp)
        )
    }
}
