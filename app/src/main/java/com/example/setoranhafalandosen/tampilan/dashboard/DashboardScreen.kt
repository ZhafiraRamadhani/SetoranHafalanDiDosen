package com.example.setoranhafalandosen.tampilan.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.background
import com.example.setoranhafalandosen.data.model.Mahasiswa

@Composable
fun MyTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = Color(0xFF008966),
        onPrimary = Color.White,
        secondary = Color(0xFFF0FFF0),
        onSecondary = Color.Black,
        background = Color(0xFFC8E6C9),
        onBackground = Color.Black,
        surface = Color(0xFFA5D6A7),
        onSurface = Color.Black
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
                                        expandedAngkatan.value = if (expandedAngkatan.value == ringkasan.tahun) null else ringkasan.tahun
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Angkatan ${ringkasan.tahun}", style = MaterialTheme.typography.titleMedium)
                                            Text("Mahasiswa: ${ringkasan.total}, Sudah Setor: $jumlahSudahSetor", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.rotate(if (expandedAngkatan.value == ringkasan.tahun) 180f else 0f)
                                        )
                                    }
                                    val summaryFraction = if (ringkasan.total > 0) jumlahSudahSetor.toFloat() / ringkasan.total else 0f
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = summaryFraction,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Progress: ${"%d / %d".format(jumlahSudahSetor, ringkasan.total)}", style = MaterialTheme.typography.bodySmall)
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
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val inisial = mahasiswa.nama.firstOrNull()?.uppercase() ?: "?"
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Text(inisial, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(mahasiswa.nama, style = MaterialTheme.typography.titleSmall)
                Text("NIM: ${mahasiswa.nim}", style = MaterialTheme.typography.bodySmall)
                Text("Sudah Setor: ${mahasiswa.info_setoran.total_sudah_setor}", style = MaterialTheme.typography.bodySmall)
            }

            Column(modifier = Modifier.weight(1f)) {
                Spacer(modifier = Modifier.height(8.dp))

                val totalWajib = mahasiswa.info_setoran.total_wajib_setor
                val sudahSetor = mahasiswa.info_setoran.total_sudah_setor
                val belumSetor = (totalWajib - sudahSetor).coerceAtLeast(0)
                val progressFraction = if (totalWajib > 0) {
                    sudahSetor.toFloat() / totalWajib
                } else 0f

                LinearProgressIndicator(
                    progress = progressFraction.coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Sudah: $sudahSetor / $totalWajib, Belum: $belumSetor",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}