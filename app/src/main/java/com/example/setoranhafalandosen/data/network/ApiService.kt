package com.example.setoranhafalandosen.data.network

import com.example.setoranhafalandosen.data.model.*
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
    suspend fun getDataDosenPA(
        @Header("Authorization") token: String,
        @Query("apikey") accessToken: String
    ): Response<SetoranResponse>

    @POST("setoran-mahasiswa/{nim}")
    suspend fun simpanSetoran(
        @Path("nim") nim: String,
        @Body body: SetoranRequest,
        @Header("Authorization") token: String
    ): Response<ResponseBody>

    @HTTP(method = "DELETE", path = "setoran-mahasiswa/{nim}", hasBody = true)
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
}
