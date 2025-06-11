package com.example.setoranhafalandosen.data.model

data class SetoranResponse(
    val response: Boolean,
    val message: String,
    val data: DosenData
)

data class DosenData(
    val nip: String,
    val nama: String,
    val email: String,
    val info_mahasiswa_pa: InfoMahasiswaPA
)

data class InfoMahasiswaPA(
    val ringkasan: List<RingkasanPerAngkatan>,
    val daftar_mahasiswa: List<Mahasiswa>
)

data class RingkasanPerAngkatan(
    val tahun: String,
    val total: Int
)

data class Mahasiswa(
    val email: String,
    val nim: String,
    val nama: String,
    val angkatan: String,
    val semester: Int,
    val info_setoran: InfoDasar
)

data class Dosen(
    val nip: String,
    val nama: String,
    val email: String
)

data class InfoDasar(
    val total_wajib_setor: Int,
    val total_sudah_setor: Int,
    val total_belum_setor: Int,
    val persentase_progres_setor: Float,
    val tgl_terakhir_setor: String?,
    val terakhir_setor: String,
    val komponen_setoran: List<SetoranItem> = emptyList()
)

data class SetoranItem(
    val id: String,
    val nama: String,
    val label: String,
    val sudah_setor: Boolean,
    val info_setoran: InfoSetoran?
)

data class InfoSetoran(
    val id: String,
    val tgl_setoran: String,
    val tgl_validasi: String,
    val dosen_yang_mengesahkan: Dosen
)

data class Setoran(
    val id: String? = null, // hanya diperlukan untuk delete
    val id_komponen_setoran: String,
    val nama_komponen_setoran: String
)

data class SetoranRequest(
    val data_setoran: List<Setoran>,
    val tgl_setoran: String? = null
)

// ✅ PERBAIKI sesuai respons JSON sebenarnya:
data class MahasiswaSetoranResponse(
    val response: Boolean,
    val message: String,
    val data: MahasiswaSetoranData
)

data class MahasiswaSetoranData(
    val info: MahasiswaInfo,
    val setoran: MahasiswaSetoranDetail
)

data class MahasiswaInfo(
    val nama: String,
    val nim: String,
    val email: String,
    val angkatan: String,
    val semester: Int,
    val dosen_pa: Dosen
)

data class MahasiswaSetoranDetail(
    val log: List<Any>,
    val info_dasar: InfoDasar,
    val ringkasan: List<Any>,
    val detail: List<SetoranItem>
)
