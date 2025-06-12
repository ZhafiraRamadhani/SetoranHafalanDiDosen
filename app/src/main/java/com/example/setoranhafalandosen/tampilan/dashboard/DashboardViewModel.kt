package com.example.setoranhafalandosen.tampilan.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import com.example.setoranhafalandosen.data.model.DosenData
import com.example.setoranhafalandosen.data.model.InfoMahasiswaPA
import com.example.setoranhafalandosen.data.model.Setoran
import com.example.setoranhafalandosen.data.model.SetoranItem
import com.example.setoranhafalandosen.data.model.SetoranRequest
import com.example.setoranhafalandosen.data.network.RetrofitClient
import com.example.setoranhafalandosen.data.network.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonElement
import com.google.gson.Gson

class HomeView(private val tokenManager: TokenManager) : ViewModel() {

    private val _dashboardState = MutableStateFlow<DashboardState>(DashboardState.Idle)
    val dashboardState: StateFlow<DashboardState> = _dashboardState

    private val _setoranMahasiswa = MutableStateFlow<List<SetoranItem>>(emptyList())
    val setoranMahasiswa: StateFlow<List<SetoranItem>> = _setoranMahasiswa

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName

    private val TAG = "HomeView"

    private fun getRolesFromToken(token: String): List<String>? {
        return try {
            val decodedJwt = JWT.decode(token)
            val realmAccess = decodedJwt.getClaim("realm_access")
            if (realmAccess.isNull) {
                Log.e(TAG, "realm_access claim is null")
                return null
            }

            // Get the claim as string using toString()
            val realmAccessStr = realmAccess.toString()
            Log.d(TAG, "Raw realm_access: $realmAccessStr")

            // Remove the outer quotes if present
            val cleanJson = realmAccessStr.trim('"')
            Log.d(TAG, "Cleaned JSON: $cleanJson")

            val gson = Gson()
            val realmAccessObj = gson.fromJson(cleanJson, JsonObject::class.java)
            if (realmAccessObj == null) {
                Log.e(TAG, "Failed to parse realm_access JSON")
                return null
            }

            Log.d(TAG, "Parsed realm_access: $realmAccessObj")

            if (!realmAccessObj.has("roles")) {
                Log.e(TAG, "No roles field in realm_access")
                return null
            }

            val rolesArray = realmAccessObj.getAsJsonArray("roles")
            if (rolesArray == null) {
                Log.e(TAG, "roles array is null in realm_access")
                return null
            }

            val roles = mutableListOf<String>()
            rolesArray.forEach { role ->
                roles.add(role.asString)
            }
            Log.d(TAG, "Extracted roles: $roles")
            roles
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing token roles: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    init {
        val idToken = tokenManager.getIdToken()
        if (idToken != null) {
            try {
                val decodedJwt = JWT.decode(idToken)
                val name = decodedJwt.getClaim("name").asString()
                    ?: decodedJwt.getClaim("preferred_username").asString()
                val roles = getRolesFromToken(idToken)
                Log.d(TAG, "Token roles: $roles")
                if (roles?.contains("dosen") != true) {
                    Log.e(TAG, "⚠️ Token tidak memiliki role dosen!")
                }
                _userName.value = name
                Log.d(TAG, "Nama pengguna dari id_token: $name")
            } catch (e: Exception) {
                Log.e(TAG, "Gagal menguraikan id_token: ${e.message}")
            }
        }
    }

    fun fetchSetoranSaya() {
        viewModelScope.launch {
            _dashboardState.value = DashboardState.Loading
            try {
                val token = tokenManager.getAccessToken()
                if (token != null) {
                    Log.d(TAG, "Mengambil data setoran dengan token: $token")
                    val response = RetrofitClient.apiService.getDosenPA(
                        token = "Bearer $token"
                    )
                    if (response.isSuccessful) {
                        val responseBody = response.body()?.string()
                        Log.d(TAG, "Data berhasil: $responseBody")
                        // Parse response body using Gson
                        val gson = Gson()
                        val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                        if (jsonObject.get("response").asBoolean) {
                            val data = jsonObject.getAsJsonObject("data")
                            _dashboardState.value = DashboardState.Success(
                                DosenData(
                                    nip = data.get("nip").asString,
                                    nama = data.get("nama").asString,
                                    email = data.get("email").asString,
                                    info_mahasiswa_pa = Gson().fromJson(data.getAsJsonObject("info_mahasiswa_pa"), InfoMahasiswaPA::class.java)
                                )
                            )
                        } else {
                            _dashboardState.value = DashboardState.Error("Gagal ambil data: ${jsonObject.get("message").asString}")
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Gagal ambil data: ${response.code()} - ${response.message()}, $errorBody")
                        handleErrorResponse(response.code(), errorBody, response.message())
                    }
                } else {
                    _dashboardState.value = DashboardState.Error("Token tidak ditemukan")
                }
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP exception: ${e.code()} - ${e.message()}")
                _dashboardState.value = DashboardState.Error("Kesalahan HTTP: ${e.message()} (Kode: ${e.code()})")
            } catch (e: Exception) {
                Log.e(TAG, "Exception: ${e.message}", e)
                _dashboardState.value = DashboardState.Error("Kesalahan jaringan: ${e.message}")
            }
        }
    }

    private suspend fun tryWithoutApiKey(token: String) {
        Log.d(TAG, "Mencoba akses tanpa apikey")
        val response = RetrofitClient.apiService.getDataDosenPA(
            token = "Bearer $token"
        )
        if (response.isSuccessful) {
            response.body()?.let { setoran ->
                Log.d(TAG, "Berhasil tanpa apikey: ${setoran.message}")
                _dashboardState.value = DashboardState.Success(setoran.data)
            } ?: run {
                Log.e(TAG, "Respons kosong tanpa apikey")
                _dashboardState.value = DashboardState.Error("Respons kosong dari server tanpa apikey")
            }
        } else {
            val errorBody = response.errorBody()?.string()
            handleErrorResponse(response.code(), errorBody, response.message())
        }
    }

    private fun handleErrorResponse(code: Int, errorBody: String?, message: String) {
        when (code) {
            401 -> refreshAccessToken()
            403 -> _dashboardState.value = DashboardState.Error("Akses ditolak (403): Periksa apikey atau scope.")
            404 -> _dashboardState.value = DashboardState.Error("Endpoint tidak ditemukan (404)")
            else -> _dashboardState.value = DashboardState.Error("Gagal ambil data: $message (Kode: $code, Body: $errorBody)")
        }
    }

    private fun refreshAccessToken() {
        viewModelScope.launch {
            try {
                val refreshToken = tokenManager.getRefreshToken()
                if (refreshToken != null) {
                    val response = RetrofitClient.kcApiService.refreshToken(
                        clientId = "setoran-mobile-dev",
                        clientSecret = "aqJp3xnXKudgC7RMOshEQP7ZoVKWzoSl",
                        grantType = "refresh_token",
                        refreshToken = refreshToken
                    )
                    if (response.isSuccessful) {
                        response.body()?.let { auth ->
                            tokenManager.saveTokens(auth.access_token, auth.refresh_token, auth.id_token)
                            Log.d(TAG, "Token diperbarui berhasil")
                        } ?: run {
                            Log.e(TAG, "Gagal memperbarui token (respons kosong)")
                            _dashboardState.value = DashboardState.Error("Gagal memperbarui token")
                        }
                    } else {
                        Log.e(TAG, "Gagal refresh token: ${response.message()} (Kode: ${response.code()})")
                        _dashboardState.value = DashboardState.Error("Gagal refresh token")
                    }
                } else {
                    Log.e(TAG, "Refresh token tidak tersedia")
                    _dashboardState.value = DashboardState.Error("Refresh token tidak tersedia")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saat refresh token: ${e.message}")
                _dashboardState.value = DashboardState.Error("Gagal memperbarui token: ${e.message}")
            }
        }
    }

    fun fetchSetoranMahasiswa(nim: String) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getAccessToken()
                if (token != null) {
                    val response = RetrofitClient.apiService.getSetoranMahasiswa(nim, "Bearer $token")
                    if (response.isSuccessful) {
                        val list = response.body()?.data?.setoran?.detail ?: emptyList()
                        _setoranMahasiswa.value = list
                        Log.d("Setoran", "✅ Jumlah surah yang ditemukan: ${list.size}")
                    } else {
                        Log.e("Setoran", "❌ Response error: ${response.errorBody()?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("Setoran", "❌ Exception: ${e.message}", e)
            }
        }
    }

    fun simpanSetoran(nim: String, data: List<Setoran>, tglSetoran: String? = null) {
        viewModelScope.launch {
            try {
                var token = tokenManager.getAccessToken()
                if (token == null) {
                    _dashboardState.value = DashboardState.Error("Token akses tidak tersedia")
                    return@launch
                }

                // Log token untuk debugging
                try {
                    val roles = getRolesFromToken(token)
                    Log.d(TAG, "Access token roles: $roles")
                    val decodedJwt = JWT.decode(token)
                    Log.d(TAG, "Token expired at: ${decodedJwt.expiresAt}")
                    Log.d(TAG, "Token claims: ${decodedJwt.claims}")

                    // Cek role dosen
                    if (roles?.contains("dosen") != true) {
                        _dashboardState.value = DashboardState.Error("Anda tidak memiliki akses untuk menyimpan setoran. Hanya dosen yang diizinkan.")
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Gagal decode token: ${e.message}")
                    _dashboardState.value = DashboardState.Error("Gagal memverifikasi token: ${e.message}")
                    return@launch
                }

                // Coba simpan dengan token saat ini
                val request = SetoranRequest(data_setoran = data, tgl_setoran = tglSetoran)
                Log.d(TAG, "Request body: ${Gson().toJson(request)}")
                Log.d(TAG, "Authorization header: Bearer $token")

                var response = RetrofitClient.apiService.simpanSetoran(
                    nim = nim,
                    body = request,
                    token = "Bearer $token"
                )

                // Jika token expired, coba refresh
                if (response.code() == 401) {
                    Log.d(TAG, "Token expired, mencoba refresh token...")
                    val refreshToken = tokenManager.getRefreshToken()
                    if (refreshToken != null) {
                        try {
                            val refreshResponse = RetrofitClient.kcApiService.refreshToken(
                                clientId = "setoran-mobile-dev",
                                clientSecret = "aqJp3xnXKudgC7RMOshEQP7ZoVKWzoSl",
                                grantType = "refresh_token",
                                refreshToken = refreshToken
                            )

                            if (refreshResponse.isSuccessful) {
                                refreshResponse.body()?.let { authResponse ->
                                    token = authResponse.access_token
                                    tokenManager.saveTokens(
                                        authResponse.access_token,
                                        authResponse.refresh_token,
                                        authResponse.id_token
                                    )

                                    // Verifikasi token baru
                                    val newRoles = getRolesFromToken(token)
                                    if (newRoles?.contains("dosen") != true) {
                                        _dashboardState.value = DashboardState.Error("Token baru tidak memiliki akses dosen")
                                        return@launch
                                    }

                                    // Coba simpan lagi dengan token baru
                                    response = RetrofitClient.apiService.simpanSetoran(
                                        nim = nim,
                                        body = request,
                                        token = "Bearer $token"
                                    )
                                } ?: run {
                                    _dashboardState.value = DashboardState.Error("Gagal mendapatkan token baru")
                                    return@launch
                                }
                            } else {
                                _dashboardState.value = DashboardState.Error("Gagal refresh token: ${refreshResponse.errorBody()?.string()}")
                                return@launch
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saat refresh token: ${e.message}")
                            _dashboardState.value = DashboardState.Error("Gagal refresh token: ${e.message}")
                            return@launch
                        }
                    } else {
                        _dashboardState.value = DashboardState.Error("Refresh token tidak tersedia")
                        return@launch
                    }
                }

                // Handle response akhir
                if (response.isSuccessful) {
                    Log.d(TAG, "✅ Setoran berhasil disimpan")
                    fetchSetoranMahasiswa(nim)
                    fetchSetoranSaya()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "❌ Gagal simpan: $errorBody")
                    _dashboardState.value = DashboardState.Error(
                        when {
                            errorBody?.contains("access_denied") == true ->
                                "Akses ditolak. Pastikan Anda login sebagai dosen."
                            response.code() == 401 -> "Token tidak valid atau expired"
                            response.code() == 403 -> "Anda tidak memiliki akses untuk menyimpan setoran"
                            else -> "Gagal menyimpan setoran: ${errorBody ?: response.message()}"
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception simpan: ${e.message}", e)
                _dashboardState.value = DashboardState.Error("Terjadi kesalahan: ${e.message}")
            }
        }
    }

    fun hapusSetoran(nim: String, data: List<Setoran>) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getAccessToken()
                if (token != null) {
                    val request = SetoranRequest(data_setoran = data)
                    val response = RetrofitClient.apiService.hapusSetoran(
                        nim = nim,
                        body = request,
                        token = "Bearer $token"
                    )
                    if (response.isSuccessful) {
                        Log.d("Setoran", "🗑️ Setoran berhasil dihapus")
                        fetchSetoranMahasiswa(nim) // Tambahkan ini agar otomatis update
                    } else {
                        Log.e("Setoran", "❌ Gagal hapus: ${response.errorBody()?.string()}")
                    }
                } else {
                    Log.e("Setoran", "Token akses tidak tersedia")
                }
            } catch (e: Exception) {
                Log.e("Setoran", "❌ Exception hapus: ${e.message}", e)
            }
        }
    }

    companion object {
        fun getFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(HomeView::class.java)) {
                        return HomeView(TokenManager(context)) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}
