package com.example.recapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.recapp.ui.list.RecordingListScreen
import com.example.recapp.ui.onboarding.SetupScreen
import com.example.recapp.ui.onboarding.SetupState
import com.example.recapp.ui.settings.SettingsScreen
import com.example.recapp.ui.theme.RecAppTheme

class MainActivity : ComponentActivity() {

    private var setupState by mutableStateOf<SetupState?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecAppTheme {
                val state = setupState ?: return@RecAppTheme
                val nav = rememberNavController()
                val start = if (state.isComplete) ROUTE_LIST else ROUTE_SETUP
                NavHost(navController = nav, startDestination = start) {
                    composable(ROUTE_SETUP) {
                        SetupScreen(
                            state = state,
                            onPermissionsResult = ::refreshSetupState,
                            onDone = {
                                nav.navigate(ROUTE_LIST) { popUpTo(ROUTE_SETUP) { inclusive = true } }
                            },
                        )
                    }
                    composable(ROUTE_LIST) {
                        // 権限やアクセシビリティが後から無効化された場合はセットアップへ戻す
                        LaunchedEffect(state.isComplete) {
                            if (!state.isComplete) nav.navigate(ROUTE_SETUP) { popUpTo(ROUTE_LIST) { inclusive = true } }
                        }
                        RecordingListScreen(onOpenSettings = { nav.navigate(ROUTE_SETTINGS) })
                    }
                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(onBack = { nav.popBackStack() }, onOpenSetup = { nav.navigate(ROUTE_SETUP) })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSetupState()
    }

    private fun refreshSetupState() {
        setupState = SetupState.from(this)
    }

    private companion object {
        const val ROUTE_SETUP = "setup"
        const val ROUTE_LIST = "list"
        const val ROUTE_SETTINGS = "settings"
    }
}
