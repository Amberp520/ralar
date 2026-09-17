package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.AppScreen
import com.example.ui.RalarViewModel
import com.example.ui.components.RalarHeader
import com.example.ui.components.RalarNavigationTabs
import com.example.ui.screens.*
import com.example.ui.theme.RalarAccent
import com.example.ui.theme.RalarFail
import com.example.ui.theme.RalarPaper
import com.example.ui.theme.RalarTheme

class MainActivity : ComponentActivity() {
  private val viewModel: RalarViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      RalarTheme {
        val currentScreen by viewModel.currentScreen.collectAsState()
        val notification by viewModel.notification.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(notification) {
          notification?.let {
            snackbarHostState.showSnackbar(
              message = it.message,
              duration = SnackbarDuration.Short
            )
            viewModel.dismissNotification()
          }
        }

        Scaffold(
          modifier = Modifier
            .fillMaxSize()
            .background(RalarPaper),
          topBar = {
            RalarHeader(
              currentScreen = currentScreen,
              onNavigate = { viewModel.navigateTo(it) }
            )
          },
          bottomBar = {
            RalarNavigationTabs(
              currentScreen = currentScreen,
              onNavigate = { viewModel.navigateTo(it) }
            )
          },
          snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
              Snackbar(
                snackbarData = data,
                containerColor = com.example.ui.theme.RalarInk,
                contentColor = RalarPaper
              )
            }
          },
          contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
              .background(RalarPaper)
          ) {
            when (val screen = currentScreen) {
              is AppScreen.Dashboard -> DashboardScreen(viewModel = viewModel)
              is AppScreen.CreateRalar -> CreateRalarScreen(viewModel = viewModel)
              is AppScreen.PublicRalar -> PublicRalarScreen(slug = screen.slug, viewModel = viewModel)
              is AppScreen.VerificationInspector -> VerificationInspectorScreen(viewModel = viewModel)
              is AppScreen.SystemConfig -> SystemConfigScreen(viewModel = viewModel)
              is AppScreen.Overview -> OverviewScreen(viewModel = viewModel)
            }
          }
        }
      }
    }
  }
}
