package com.example.setoranhafalandosen.tampilan.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.setoranhafalandosen.data.network.RetrofitClient
import com.example.setoranhafalandosen.data.network.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val tokenManager: TokenManager) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val response = RetrofitClient.kcApiService.login(
                    clientId = "setoran-mobile-dev",
                    clientSecret = "aqJp3xnXKudgC7RMOshEQP7ZoVKWzoSl",
                    grantType = "password",
                    username = username,
                    password = password,
                    scope = "openid profile email"
                )
                if (response.isSuccessful) {
                    response.body()?.let { auth ->
                        tokenManager.saveTokens(auth.access_token, auth.refresh_token, auth.id_token)
                        _loginState.value = LoginState.Success
                    } ?: run {
                        _loginState.value = LoginState.Error("Respons kosong")
                    }
                } else {
                    _loginState.value = LoginState.Error("Login gagal: ${response.message()}")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Kesalahan: ${e.message}")
            }
        }
    }

    fun logout() {
        // Clear local tokens without network call
        tokenManager.clearTokens()
        _loginState.value = LoginState.Idle
    }

    fun logoutServer(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val idToken = tokenManager.getIdToken()
            if (idToken == null) {
                tokenManager.clearTokens()
                onResult(true, "Token lokal dibersihkan")
                return@launch
            }
            try {
                val resp = RetrofitClient.kcApiService.logout(
                    clientId = "setoran-mobile-dev",
                    clientSecret = "aqJp3xnXKudgC7RMOshEQP7ZoVKWzoSl",
                    idToken = idToken
                )
                if (resp.isSuccessful) {
                    tokenManager.clearTokens()
                    onResult(true, "Logout berhasil")
                } else {
                    onResult(false, "Gagal logout: ${resp.message()}")
                }
            } catch (e: Exception) {
                onResult(false, "Kesalahan: ${e.message}")
            }
        }
    }

    companion object {
        fun getFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                        return LoginViewModel(TokenManager(context)) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}