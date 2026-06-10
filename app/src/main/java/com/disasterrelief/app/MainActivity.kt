package com.disasterrelief.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.disasterrelief.app.presentation.navigation.AppNavigation
import com.disasterrelief.app.presentation.theme.DisasterReliefTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Activity entry point. All navigation is handled via
 * Jetpack Compose Navigation within [AppNavigation].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DisasterReliefTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
