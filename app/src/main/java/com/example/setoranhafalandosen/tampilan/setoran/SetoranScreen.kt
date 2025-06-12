package com.example.setoranhafalandosen.tampilan.setoran

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
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
import kotlinx.coroutines.launch
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import com.example.setoranhafalandosen.data.model.Setoran
import com.example.setoranhafalandosen.tampilan.dashboard.DashboardState
import com.example.setoranhafalandosen.tampilan.dashboard.HomeView
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

val CreamBackground = Color(0xFFFFF8E1)

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

    val refreshTrigger = remember { mutableStateOf(false) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var resultDialogMessage by remember { mutableStateOf<String?>(null) }

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        homeViewModel.fetchSetoranSaya()
        homeViewModel.fetchSetoranMahasiswa(nim)
    }

    LaunchedEffect(refreshTrigger.value) {
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
            if (dashboardState is DashboardState.Success) {
                val showButtons = selectedSetoran.isNotEmpty() && !isProcessing
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        enabled = showButtons,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batalkan (${selectedSetoran.size})")
                    }
                    Button(
                        onClick = { showSaveDialog = true },
                        enabled = showButtons,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simpan (${selectedSetoran.size})")
                    }
                }
            }
        }
    ) { padding ->
        when (val state = dashboardState) {
            is DashboardState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is DashboardState.Success -> {
                val mahasiswa = state.data.info_mahasiswa_pa.daftar_mahasiswa.find { it.nim == nim }

                val filteredList = if (searchQuery.isBlank()) {
                    setoranList
                } else {
                    setoranList.filter { it.nama.contains(searchQuery, ignoreCase = true) }
                }
                val groupedSetoran = filteredList.groupBy { it.label.ifEmpty { "Lainnya" } }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        mahasiswa?.let {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CreamBackground)
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
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val totalWajib = it.info_setoran.total_wajib_setor
                                        val progressFraction = if (totalWajib > 0) {
                                            it.info_setoran.total_sudah_setor.toFloat() / totalWajib
                                        } else 0f
                                        LinearProgressIndicator(
                                            progress = progressFraction.coerceIn(0f, 1f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Progress Surah: ${it.info_setoran.total_sudah_setor} / $totalWajib",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Cari Surah") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)) // Biru muda lembut
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Informasi",
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Anda dapat memilih lebih dari satu surah untuk disetorkan sekaligus.",
                                    color = Color.Black,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tanggal Setoran: ")
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(onClick = { showDatePicker = true }) {
                                Text(selectedDate?.toString() ?: "Pilih Tanggal")
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
                                    containerColor = if (isSelected) CreamBackground else MaterialTheme.colorScheme.surface
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

                LaunchedEffect(dashboardState) {
                    if (dashboardState is DashboardState.Error) {
                        val errorMessage = (dashboardState as DashboardState.Error).message
                        snackbarHostState.showSnackbar(errorMessage)

                        if (errorMessage.contains("Token") ||
                            errorMessage.contains("Sesi") ||
                            errorMessage.contains("login") ||
                            errorMessage.contains("Akses ditolak")) {
                            kotlinx.coroutines.delay(1000)
                            navController.navigate("login") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        }
                    }
                }

                if (showSaveDialog) {
                    AlertDialog(
                        onDismissRequest = { showSaveDialog = false },
                        title = { Text("Konfirmasi Simpan") },
                        text = { Text("Apakah Anda yakin ingin menyimpan setoran?") },
                        confirmButton = {
                            TextButton(onClick = {
                                showSaveDialog = false
                                scope.launch {
                                    isProcessing = true
                                    try {
                                        homeViewModel.simpanSetoran(
                                            nim,
                                            selectedSetoran.values.toList(),
                                            tglSetoran = selectedDate?.toString()
                                        )
                                        snackbarHostState.showSnackbar("✅ Setoran berhasil disimpan")
                                        resultDialogMessage = "Setoran berhasil disimpan!"
                                        selectedSetoran.clear()
                                        homeViewModel.fetchSetoranMahasiswa(nim)
                                        homeViewModel.fetchSetoranSaya()
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Gagal menyimpan setoran: ${e.message}")
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }) {
                                Text("Ya")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSaveDialog = false }) {
                                Text("Tidak")
                            }
                        }
                    )
                }

                if (showCancelDialog) {
                    AlertDialog(
                        onDismissRequest = { showCancelDialog = false },
                        title = { Text("Konfirmasi Pembatalan") },
                        text = { Text("Apakah Anda yakin ingin membatalkan setoran terpilih?") },
                        confirmButton = {
                            TextButton(onClick = {
                                showCancelDialog = false
                                scope.launch {
                                    isProcessing = true
                                    try {
                                        homeViewModel.hapusSetoran(
                                            nim,
                                            selectedSetoran.values.toList()
                                        )
                                        snackbarHostState.showSnackbar("🗑️ Setoran dibatalkan")
                                        resultDialogMessage = "Setoran berhasil dibatalkan!"
                                        selectedSetoran.clear()
                                        refreshTrigger.value = !refreshTrigger.value
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("❌ Gagal membatalkan setoran")
                                    }
                                    isProcessing = false
                                }
                            }) {
                                Text("Ya")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCancelDialog = false }) {
                                Text("Tidak")
                            }
                        }
                    )
                }

                resultDialogMessage?.let { msg ->
                    AlertDialog(
                        onDismissRequest = { resultDialogMessage = null },
                        confirmButton = {
                            TextButton(onClick = { resultDialogMessage = null }) {
                                Text("OK")
                            }
                        },
                        title = { Text("Berhasil") },
                        text = { Text(msg) }
                    )
                }
            }

            is DashboardState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Terjadi kesalahan: ${state.message}")
                }
            }

            else -> {}
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            selectedDate = date
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}