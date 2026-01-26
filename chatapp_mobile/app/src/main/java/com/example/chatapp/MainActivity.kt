package com.example.chatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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

                // --- SỬA LỖI 2: Tự động cập nhật Online/Offline theo vòng đời App ---
                // Khi App mở -> Online, Khi App ẩn/tắt -> Offline
                val lifecycleObserver = rememberLifecycleObserver(vm)

                val currentUser by vm.currentUser.collectAsState(initial = null)
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

                        // SỬA TẠI ĐÂY: đổi viewModel thành vm
                        ChatInfoScreen(navController, vm, partnerId, partnerEmail)
                    }
                }
            }
        }
    }

    // Hàm theo dõi vòng đời ứng dụng
    private fun rememberLifecycleObserver(viewModel: ChatViewModel): DefaultLifecycleObserver {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                // App hiện lên màn hình -> Set Online
                viewModel.setOnlineStatus(true)
            }

            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                // App bị ẩn hoặc tắt -> Set Offline
                viewModel.setOnlineStatus(false)
            }
        }
        // Đăng ký observer với toàn bộ quy trình ứng dụng
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        return observer
    }
}