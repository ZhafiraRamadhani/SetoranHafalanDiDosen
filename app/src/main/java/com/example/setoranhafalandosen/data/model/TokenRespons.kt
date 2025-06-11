package com.example.setoranhafalandosen.data.model

data class TokenRespons(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val refresh_token: String
)