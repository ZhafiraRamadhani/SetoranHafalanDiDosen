package com.example.setoranhafalandosen.tampilan.profil

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch
import com.auth0.jwt.JWT
import com.example.setoranhafalandosen.data.network.TokenManager
import com.example.setoranhafalandosen.tampilan.login.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModel.getFactory(context))
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val tokenManager = remember { TokenManager(context) }
    val idToken = tokenManager.getIdToken()
    val (displayName, displayEmail) = remember(idToken) {
        if (idToken != null) {
            try {
                val jwt = JWT.decode(idToken)
                val name = jwt.getClaim("name").asString()
                    ?: jwt.getClaim("preferred_username").asString()
                val email = jwt.getClaim("email").asString()
                name to (email ?: "-")
            } catch (e: Exception) {
                "Pengguna" to "-"
            }
        } else {
            "Pengguna" to "-"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Profil") })
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(displayName, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(displayEmail, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    loginViewModel.logoutServer { success, msg ->
                        scope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                        if (success) {
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }) {
                    Text("Logout")
                }
            }
        }
    }
}