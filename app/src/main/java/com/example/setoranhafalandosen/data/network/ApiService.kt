package com.example.setoranhafalandosen.data.network

import com.example.setoranhafalandosen.data.model.AuthResponse
import com.example.setoranhafalandosen.data.model.MahasiswaSetoranResponse
import com.example.setoranhafalandosen.data.model.SetoranRequest
import com.example.setoranhafalandosen.data.model.SetoranResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @FormUrlEncoded
    @POST("/realms/dev/protocol/openid-connect/token")
    suspend fun login(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("grant_type") grantType: String,
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("scope") scope: String
    ): Response<AuthResponse>

    @FormUrlEncoded
    @POST("/realms/dev/protocol/openid-connect/token")
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("grant_type") grantType: String,
        @Field("refresh_token") refreshToken: String
    ): Response<AuthResponse>

    @GET("dosen/pa-saya")
    suspend fun getDataDosenPA(
        @Header("Authorization") token: String
    ): Response<SetoranResponse>

    @GET("dosen/pa-saya")
    suspend fun getDosenPA(
        @Header("Authorization") token: String
    ): Response<ResponseBody>

    @POST("mahasiswa/setoran/{nim}")
    suspend fun simpanSetoran(
        @Path("nim") nim: String,
        @Body body: SetoranRequest,
        @Header("Authorization") token: String
    ): Response<ResponseBody>

    @HTTP(method = "DELETE", path = "mahasiswa/setoran/{nim}", hasBody = true)
    suspend fun hapusSetoran(
        @Path("nim") nim: String,
        @Body body: SetoranRequest,
        @Header("Authorization") token: String
    ): Response<ResponseBody>

    @GET("mahasiswa/setoran/{nim}")
    suspend fun getSetoranMahasiswa(
        @Path("nim") nim: String,
        @Header("Authorization") token: String
    ): Response<MahasiswaSetoranResponse>

    // Logout endpoint (Keycloak)
    @FormUrlEncoded
    @POST("/realms/dev/protocol/openid-connect/logout")
    suspend fun logout(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("id_token_hint") idToken: String
    ): Response<ResponseBody>
}
