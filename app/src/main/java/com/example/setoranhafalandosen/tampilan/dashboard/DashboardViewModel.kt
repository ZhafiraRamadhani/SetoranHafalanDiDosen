package com.example.setoranhafalandosen.tampilan.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.auth0.jwt.JWT
import com.example.setoranhafalandosen.data.model.*
import com.example.setoranhafalandosen.data.network.RetrofitClient
import com.example.setoranhafalandosen.data.network.TokenManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import java.time.LocalDate
import java.util.*

class HomeView(
    private val context: Context,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _dashboardState = MutableStateFlow<DashboardState>(DashboardState.Idle)
    val dashboardState: StateFlow<DashboardState> = _dashboardState

    private val _setoranMahasiswa = MutableStateFlow<List<SetoranItem>>(emptyList())
    val setoranMahasiswa: StateFlow<List<SetoranItem>> = _setoranMahasiswa

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName

    private val TAG = "HomeView"

    init {
        val idToken = tokenManager.getIdToken()
        if (idToken != null) {
            try {
                val decodedJwt = JWT.decode(idToken)
                val name = decodedJwt.getClaim("name").asString()
                    ?: decodedJwt.getClaim("preferred_username").asString()
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
                    val response = RetrofitClient.apiService.getDataDosenPA(
                        token = "Bearer $token",
                        accessToken = token
                    )
                    if (response.isSuccessful) {
                        response.body()?.let { setoran ->
                            Log.d(TAG, "Data berhasil: ${setoran.message}")
                            _dashboardState.value = DashboardState.Success(setoran.data)
                        } ?: run {
                            Log.e(TAG, "Response body kosong")
                            _dashboardState.value = DashboardState.Error("Respons kosong dari server")
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Gagal ambil data: ${response.code()} - ${response.message()}, $errorBody")
                        if (response.code() == 403) {
                            tryWithoutApiKey(token)
                        } else {
                            handleErrorResponse(response.code(), errorBody, response.message())
                        }
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
                        Log.d(TAG, "Token diperbarui. Memuat ulang data...")
                        fetchSetoranSaya()
                    } ?: run {
                        _dashboardState.value = DashboardState.Error("Gagal memperbarui token (respons kosong)")
                    }
                } else {
                    _dashboardState.value = DashboardState.Error("Gagal refresh token: ${response.message()} (Kode: ${response.code()})")
                }
            } else {
                _dashboardState.value = DashboardState.Error("Refresh token tidak tersedia")
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
                        val result = response.body()
                        val list = result?.data?.setoran?.detail ?: emptyList()
                        _setoranMahasiswa.value = emptyList()
                        delay(50)
                        _setoranMahasiswa.value = list.toList()
                        result?.let { simpanJsonLokal(context, it) }
                    } else {
                        Log.e("Setoran", "❌ Gagal ambil: ${response.errorBody()?.string()}")
                        bacaJsonLokal(context)?.let {
                            _setoranMahasiswa.value = it.data.setoran.detail
                            Log.w("Setoran", "⚠️ Gunakan cache lokal")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Setoran", "❌ Exception fetchSetoranMahasiswa: ${e.message}", e)
                bacaJsonLokal(context)?.let {
                    _setoranMahasiswa.value = it.data.setoran.detail
                    Log.w("Setoran", "⚠️ Gunakan cache lokal")
                }
            }
        }
    }

    fun simpanSetoran(nim: String, data: List<Setoran>) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getAccessToken()
                if (token != null) {
                    val request = SetoranRequest(data_setoran = data)
                    val response = RetrofitClient.apiService.simpanSetoran(nim, request, "Bearer $token")
                    if (response.isSuccessful) {
                        Log.d("Setoran", "✅ Setoran berhasil disimpan")
                        fetchSetoranMahasiswa(nim)
                    } else {
                        Log.e("Setoran", "❌ Gagal simpan: ${response.errorBody()?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("Setoran", "❌ Exception simpan: ${e.message}", e)
            }
        }
    }

    fun hapusSetoran(nim: String, data: List<Setoran>) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getAccessToken()
                if (token != null) {
                    val request = SetoranRequest(data_setoran = data)
                    val response = RetrofitClient.apiService.hapusSetoran(nim, request, "Bearer $token")
                    if (response.isSuccessful) {
                        Log.d("Setoran", "🗑️ Setoran berhasil dihapus")
                        fetchSetoranMahasiswa(nim)
                    } else {
                        Log.e("Setoran", "❌ Gagal hapus: ${response.errorBody()?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("Setoran", "❌ Exception hapus: ${e.message}", e)
            }
        }
    }

    fun updateCacheSetoranLangsung(nim: String, updatedList: List<SetoranItem>) {
        val dummyResponse = MahasiswaSetoranResponse(
            response = true,
            message = "Updated manually from local",
            data = MahasiswaSetoranData(
                info = MahasiswaInfo(
                    nama = "",
                    nim = nim,
                    email = "",
                    angkatan = "",
                    semester = 0,
                    dosen_pa = Dosen("", "", "")
                ),
                setoran = MahasiswaSetoranDetail(
                    log = emptyList(),
                    info_dasar = InfoDasar(
                        total_wajib_setor = updatedList.size,
                        total_sudah_setor = updatedList.count { it.sudah_setor },
                        total_belum_setor = updatedList.count { !it.sudah_setor },
                        persentase_progres_setor = if (updatedList.isNotEmpty())
                            updatedList.count { it.sudah_setor } * 100f / updatedList.size else 0f,
                        tgl_terakhir_setor = updatedList.maxByOrNull { it.info_setoran?.tgl_setoran ?: "" }?.info_setoran?.tgl_setoran,
                        terakhir_setor = "",
                        komponen_setoran = updatedList
                    ),
                    ringkasan = emptyList(),
                    detail = updatedList
                )
            )
        )
        simpanJsonLokal(context, dummyResponse)
    }

    private fun simpanJsonLokal(context: Context, data: MahasiswaSetoranResponse) {
        try {
            val file = File(context.filesDir, "setoran_mahasiswa.json")
            val json = GsonBuilder().setPrettyPrinting().create().toJson(data)
            file.writeText(json)
            Log.d("SetoranJSON", "✅ JSON berhasil disimpan: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("SetoranJSON", "❌ Gagal simpan JSON: ${e.message}")
        }
    }

    private fun bacaJsonLokal(context: Context): MahasiswaSetoranResponse? {
        return try {
            val file = File(context.filesDir, "setoran_mahasiswa.json")
            if (!file.exists()) return null
            val json = file.readText()
            Gson().fromJson(json, MahasiswaSetoranResponse::class.java)
        } catch (e: Exception) {
            Log.e("SetoranJSON", "❌ Gagal baca JSON: ${e.message}")
            null
        }
    }

    companion object {
        fun getFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(HomeView::class.java)) {
                        return HomeView(context, TokenManager(context)) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}