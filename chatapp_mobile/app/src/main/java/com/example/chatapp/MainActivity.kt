package com.example.chatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.chatapp.ui.theme.ChatAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ChatAppTheme {
                val navController = rememberNavController()
                val vm: ChatViewModel = viewModel()
                val context = LocalContext.current
                
                // Trạng thái chờ kiểm tra session
                var isCheckingSession by remember { mutableStateOf(true) }
                val currentUser by vm.currentUser.collectAsState()

                LaunchedEffect(Unit) {
                    vm.checkSavedSession(context) {
                        isCheckingSession = false
                    }
                }

                if (isCheckingSession) {
                    // Màn hình chờ (Splash)
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val startDest = if (currentUser != null) "user_list" else "login"

                    NavHost(navController = navController, startDestination = startDest) {
                        composable("login") { LoginScreen(navController, vm) }
                        composable("user_list") { UserListScreen(navController, vm) }
                        composable(
                            "chat/{id}/{email}",
                            arguments = listOf(
                                navArgument("id") { type = NavType.StringType },
                                navArgument("email") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id") ?: ""
                            val email = backStackEntry.arguments?.getString("email") ?: ""
                            ChatScreen(navController, vm, id, email)
                        }
                        composable(
                            route = "chat_info/{partnerId}/{partnerEmail}",
                            arguments = listOf(
                                navArgument("partnerId") { type = NavType.StringType },
                                navArgument("partnerEmail") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val partnerId = backStackEntry.arguments?.getString("partnerId") ?: ""
                            val partnerEmail = backStackEntry.arguments?.getString("partnerEmail") ?: ""
                            ChatInfoScreen(navController, vm, partnerId, partnerEmail)
                        }
                    }
                }
            }
        }
    }
}
