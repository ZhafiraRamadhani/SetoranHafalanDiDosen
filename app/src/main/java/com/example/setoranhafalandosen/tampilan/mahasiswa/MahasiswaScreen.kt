package com.example.setoranhafalandosen.tampilan.mahasiswa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.setoranhafalandosen.tampilan.dashboard.DashboardState
import com.example.setoranhafalandosen.tampilan.dashboard.HomeView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MahasiswaScreen(navController: NavController) {
    val viewModel: HomeView = viewModel(factory = HomeView.getFactory(LocalContext.current))
    val state by viewModel.dashboardState.collectAsState()
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(Unit) {
        viewModel.fetchSetoranSaya()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Mahasiswa") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Filter berdasarkan nama atau NIM:",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Cari")
                },
                placeholder = { Text("Ketik nama atau NIM...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (val currentState = state) {
                is DashboardState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is DashboardState.Success -> {
                    val semuaMahasiswa = currentState.data.info_mahasiswa_pa.daftar_mahasiswa

                    // 🔍 Filter mahasiswa berdasarkan nama/NIM (tanpa collapse)
                    val filteredMahasiswa = semuaMahasiswa.filter {
                        it.nama.contains(searchQuery.text, ignoreCase = true) ||
                                it.nim.contains(searchQuery.text, ignoreCase = true)
                    }

                    // 🧮 Group by angkatan
                    val groupedByAngkatan = filteredMahasiswa.groupBy { it.angkatan }

                    if (filteredMahasiswa.isEmpty()) {
                        Text(
                            text = "Tidak ada mahasiswa ditemukan.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        LazyColumn {
                            groupedByAngkatan.toSortedMap().forEach { (angkatan, listMhs) ->
                                item {
                                    Text(
                                        text = "Angkatan $angkatan",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }

                                items(listMhs, key = { it.nim }) { mahasiswa ->
                                    MahasiswaItemCard(mahasiswa = mahasiswa, navController = navController)
                                }
                            }
                        }
                    }
                }

                is DashboardState.Error -> {
                    Text("Terjadi kesalahan: ${currentState.message}")
                }

                else -> {}
            }
        }
    }
}
