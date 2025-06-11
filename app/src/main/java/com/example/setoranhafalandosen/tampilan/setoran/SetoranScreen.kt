package com.example.setoranhafalandosen.tampilan.setoran

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.setoranhafalandosen.data.model.Setoran
import com.example.setoranhafalandosen.data.model.SetoranItem
import com.example.setoranhafalandosen.tampilan.dashboard.DashboardState
import com.example.setoranhafalandosen.tampilan.dashboard.HomeView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetoranScreen(navController: NavController, nim: String) {
    val context = LocalContext.current
    val homeViewModel: HomeView = viewModel(factory = HomeView.getFactory(context))
    val dashboardState by homeViewModel.dashboardState.collectAsState()
    val setoranList by homeViewModel.setoranMahasiswa.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val selectedSetoran = remember { mutableStateMapOf<String, Setoran>() }
    var isProcessing by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var actionType by remember { mutableStateOf("") } // "post" or "delete"

    LaunchedEffect(Unit) {
        homeViewModel.fetchSetoranSaya()
        homeViewModel.fetchSetoranMahasiswa(nim)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setoran $nim") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    enabled = selectedSetoran.isNotEmpty() && !isProcessing,
                    onClick = {
                        actionType = "post"
                        showConfirmDialog = true
                    }
                ) {
                    Text("Simpan (${selectedSetoran.size})")
                }

                OutlinedButton(
                    enabled = selectedSetoran.isNotEmpty() && !isProcessing,
                    onClick = {
                        actionType = "delete"
                        showConfirmDialog = true
                    }
                ) {
                    Text("Batalkan (${selectedSetoran.size})")
                }
            }
        }
    ) { padding ->
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = {
                    Text(if (actionType == "post") "Konfirmasi Simpan" else "Konfirmasi Batalkan")
                },
                text = {
                    Text("Apakah Anda yakin ingin ${if (actionType == "post") "menyimpan" else "membatalkan"} ${selectedSetoran.size} setoran?")
                },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirmDialog = false
                        scope.launch {
                            isProcessing = true
                            try {
                                if (actionType == "post") {
                                    homeViewModel.simpanSetoran(nim, selectedSetoran.values.toList())
                                    homeViewModel.updateCacheSetoranLangsung(nim, setoranList)
                                    homeViewModel.fetchSetoranMahasiswa(nim)
                                    homeViewModel.fetchSetoranSaya()
                                    snackbarHostState.showSnackbar("✅ Setoran berhasil disimpan")
                                } else {
                                    homeViewModel.hapusSetoran(nim, selectedSetoran.values.toList())
                                    homeViewModel.updateCacheSetoranLangsung(nim, setoranList)
                                    homeViewModel.fetchSetoranMahasiswa(nim)
                                    homeViewModel.fetchSetoranSaya()
                                    snackbarHostState.showSnackbar("🗑️ Setoran dibatalkan")
                                }
                                selectedSetoran.clear()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("❌ Gagal memproses setoran")
                            }
                            isProcessing = false
                        }
                    }) {
                        Text("Ya")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Tidak")
                    }
                }
            )
        }

        when (val state = dashboardState) {
            is DashboardState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is DashboardState.Success -> {
                val mahasiswa = state.data.info_mahasiswa_pa.daftar_mahasiswa.find { it.nim == nim }
                val groupedSetoran = setoranList.groupBy { it.label.ifEmpty { "Lainnya" } }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        mahasiswa?.let {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val inisial = it.nama.firstOrNull()?.uppercase() ?: "?"
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = inisial,
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleLarge,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(it.nama, style = MaterialTheme.typography.titleMedium)
                                        Text("NIM: ${it.nim}")
                                        Text("Angkatan: ${it.angkatan}, Semester ${it.semester}")
                                    }
                                }
                            }
                        }
                    }

                    groupedSetoran.forEach { (label, items) ->
                        item {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(items, key = { it.id }) { item ->
                            val isSelected = selectedSetoran.containsKey(item.id)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isProcessing) {
                                        val setoran = Setoran(
                                            id = item.info_setoran?.id,
                                            id_komponen_setoran = item.id,
                                            nama_komponen_setoran = item.nama
                                        )
                                        if (isSelected) selectedSetoran.remove(item.id)
                                        else selectedSetoran[item.id] = setoran
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(item.nama, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        if (item.sudah_setor) "✅ Sudah Setor" else "❌ Belum Setor",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    item.info_setoran?.let {
                                        Text("Disahkan: ${it.dosen_yang_mengesahkan.nama}")
                                        Text("Tanggal: ${it.tgl_setoran}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is DashboardState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Terjadi kesalahan: ${state.message}")
                }
            }
            else -> {}
        }
    }
}
