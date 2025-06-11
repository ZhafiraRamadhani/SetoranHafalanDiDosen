package com.example.setoranhafalandosen.tampilan.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.setoranhafalandosen.data.model.Mahasiswa
import kotlinx.coroutines.launch

@Composable
fun MyTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = Color(0xFF008966),        // Hijau tua
        onPrimary = Color.White,
        secondary = Color(0xFFF0FFF0),      // Hijau sedang
        onSecondary = Color.White,
        background = Color(0xFFC8E6C9),     // Latar hijau muda
        onBackground = Color.Black,
        surface = Color(0xFFA5D6A7),        // Warna kartu
        onSurface = Color.Black,
        tertiary = Color(0xFFF0FFF0),
        onTertiary = Color.Black
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val dashboardViewModel: HomeView = viewModel(factory = HomeView.getFactory(context))
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val expandedAngkatan = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { dashboardViewModel.fetchSetoranSaya() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Rekap Mahasiswa PA") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (val state = dashboardState) {
                is DashboardState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                is DashboardState.Success -> {
                    val data = state.data
                    val ringkasanList = data.info_mahasiswa_pa.ringkasan
                    val semuaMahasiswa = data.info_mahasiswa_pa.daftar_mahasiswa

                    item {
                        Text("Dosen PA: ${data.nama}", style = MaterialTheme.typography.titleMedium)
                        Text("Email: ${data.email}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                    }

                    ringkasanList.forEach { ringkasan ->
                        val mahasiswaAngkatan = semuaMahasiswa.filter { it.angkatan == ringkasan.tahun }
                        val jumlahSudahSetor = mahasiswaAngkatan.count { it.info_setoran.total_sudah_setor > 0 }

                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable {
                                        expandedAngkatan.value =
                                            if (expandedAngkatan.value == ringkasan.tahun) null else ringkasan.tahun
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Angkatan ${ringkasan.tahun}", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Mahasiswa: ${ringkasan.total}, Sudah Setor: $jumlahSudahSetor",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        if (expandedAngkatan.value == ringkasan.tahun) {
                            items(mahasiswaAngkatan, key = { it.nim }) { mhs ->
                                MahasiswaItemCard(mahasiswa = mhs, navController = navController)
                            }
                        }
                    }
                }

                is DashboardState.Error -> {
                    item {
                        LaunchedEffect(state.message) {
                            scope.launch { snackbarHostState.showSnackbar(state.message) }
                        }
                    }
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun MahasiswaItemCard(mahasiswa: Mahasiswa, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { navController.navigate("setoran/${mahasiswa.nim}") },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = mahasiswa.nama, style = MaterialTheme.typography.titleSmall)
            Text(text = "NIM: ${mahasiswa.nim}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Angkatan: ${mahasiswa.angkatan}", style = MaterialTheme.typography.bodySmall)
            Text(
                text = "Sudah Setor: ${mahasiswa.info_setoran.total_sudah_setor}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}