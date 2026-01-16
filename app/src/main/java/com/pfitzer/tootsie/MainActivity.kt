package com.pfitzer.tootsie

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.pfitzer.tootsie.presentation.navigation.TootsieNavHost
import com.pfitzer.tootsie.presentation.theme.TootsieTheme
import com.pfitzer.tootsie.data.local.datastore.PreferencesManager
import androidx.compose.foundation.isSystemInDarkTheme
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for Tootsie app.
 * Single Activity architecture with Jetpack Compose Navigation.
 * Also handles OAuth callback deep links.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private var authorizationCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle OAuth callback from deep link
        handleOAuthCallback(intent)

        setContent {
            val themePreference by preferencesManager.theme.collectAsState(initial = "system")
            val darkTheme = when (themePreference) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            TootsieTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    TootsieNavHost(
                        navController = navController,
                        authorizationCode = authorizationCode,
                        onAuthCodeConsumed = { authorizationCode = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthCallback(intent)
    }

    /**
     * Extract authorization code from OAuth callback deep link.
     * Expected format: tootsie://oauth?code=AUTHORIZATION_CODE
     */
    private fun handleOAuthCallback(intent: Intent?) {
        val uri = intent?.data
        if (uri != null && uri.scheme == "tootsie" && uri.host == "oauth") {
            val code = uri.getQueryParameter("code")
            if (code != null) {
                authorizationCode = code
            }
        }
    }
}
