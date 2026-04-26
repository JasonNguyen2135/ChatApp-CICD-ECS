package com.example.chatapp

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

@Composable
fun LoginScreen(navController: NavController, viewModel: ChatViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    var isLoginMode by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val zaloBlue = Color(0xFF0068FF)

    // Tự động kiểm tra session khi mở màn hình
    LaunchedEffect(Unit) {
        viewModel.checkSavedSession(context) { success ->
            if (success) {
                navController.navigate("user_list") { popUpTo("login") { inclusive = true } }
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Zalo Chat", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = zaloBlue)
        Text(if (isLoginMode) "Chào mừng quay trở lại" else "Tạo tài khoản mới", color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        if (!isLoginMode) {
            Box(
                Modifier.size(100.dp).clip(CircleShape).background(Color(0xFFEEEEEE)).clickable { photoPicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    Image(painter = rememberAsyncImagePainter(selectedImageUri), contentDescription = null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.AddAPhoto, null, tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = firstName, onValueChange = { firstName = it },
                    label = { Text("Họ") }, modifier = Modifier.weight(1f).padding(end = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = lastName, onValueChange = { lastName = it },
                    label = { Text("Tên") }, modifier = Modifier.weight(1f).padding(start = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, null, tint = zaloBlue) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Mật khẩu") }, leadingIcon = { Icon(Icons.Default.Lock, null, tint = zaloBlue) },
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(image, null) }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val cleanEmail = email.trim()
                val cleanPass = password.trim()
                if (cleanEmail.isNotEmpty() && cleanPass.isNotEmpty()) {
                    isLoading = true
                    val onResult: (Boolean) -> Unit = { success ->
                        isLoading = false
                        if (success) {
                            navController.navigate("user_list") { popUpTo("login") { inclusive = true } }
                        } else {
                            Toast.makeText(context, "Thao tác thất bại", Toast.LENGTH_LONG).show()
                        }
                    }

                    if (isLoginMode) {
                        viewModel.login(cleanEmail, cleanPass, context, onResult)
                    } else {
                        viewModel.signUp(cleanEmail, cleanPass, firstName, lastName, selectedImageUri, context, onResult)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = zaloBlue),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text(if (isLoginMode) "ĐĂNG NHẬP" else "ĐĂNG KÝ")
        }

        TextButton(onClick = { isLoginMode = !isLoginMode }) {
            Text(if (isLoginMode) "Chưa có tài khoản? Đăng ký ngay" else "Đã có tài khoản? Đăng nhập", color = zaloBlue)
        }
    }
}
