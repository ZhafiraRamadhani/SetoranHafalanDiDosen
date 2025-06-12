package com.example.setoranhafalandosen.tampilan.mahasiswa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.setoranhafalandosen.data.model.Mahasiswa

@Composable
fun MahasiswaItemCard(
    mahasiswa: Mahasiswa,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
                val totalWajib = mahasiswa.info_setoran.total_wajib_setor
                val sudahSetor = mahasiswa.info_setoran.total_sudah_setor
                val belumSetor = (totalWajib - sudahSetor).coerceAtLeast(0)
                val progressFraction = if (totalWajib > 0) sudahSetor.toFloat() / totalWajib else 0f

                Spacer(modifier = Modifier.height(8.dp))
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
