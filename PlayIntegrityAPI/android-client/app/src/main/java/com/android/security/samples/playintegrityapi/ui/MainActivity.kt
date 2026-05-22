package com.android.security.samples.playintegrityapi.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.android.security.samples.playintegrityapi.feature.bank.ui.navigation.bankScreen
import com.android.security.samples.playintegrityapi.feature.bank.ui.navigation.navigateToBank
import com.android.security.samples.playintegrityapi.feature.streaming.ui.navigation.navigateToStreaming
import com.android.security.samples.playintegrityapi.feature.streaming.ui.navigation.streamingScreen
import com.android.security.samples.playintegrityapi.navigation.HOME_ROUTE
import com.android.security.samples.playintegrityapi.navigation.homeScreen
import com.android.security.samples.playintegrityapi.core.ui.theme.PiaSampleTheme
import com.android.security.samples.playintegrityapi.feature.game.ui.navigation.gameScreen
import com.android.security.samples.playintegrityapi.feature.game.ui.navigation.navigateToGame
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PiaSampleTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PlayIntegrityApp()
                }
            }
        }
    }
}

/**
 * Top-level composable that manages application state and navigation.
 */
@Composable
fun PlayIntegrityApp() {
    val navController = rememberNavController()
    val animationDuration = 300

    NavHost(
        navController = navController,
        startDestination = HOME_ROUTE,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(animationDuration)
            ) + fadeIn(animationSpec = tween(animationDuration))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(animationDuration))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(animationDuration))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(animationDuration)
            ) + fadeOut(animationSpec = tween(animationDuration))
        }
    ) {
        homeScreen(
            onNavigateToBank = { navController.navigateToBank() },
            onNavigateToStreaming = { navController.navigateToStreaming() },
            onNavigateToGame = { navController.navigateToGame() }
        )

        bankScreen(
            onBackClick = { navController.popBackStack() }
        )

        streamingScreen(
            onBackClick = { navController.popBackStack() }
        )

        gameScreen(
            onBackClick = { navController.popBackStack() }
        )
    }
}