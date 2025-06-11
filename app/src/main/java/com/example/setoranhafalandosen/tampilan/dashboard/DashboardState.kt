package com.example.setoranhafalandosen.tampilan.dashboard

import com.example.setoranhafalandosen.data.model.DosenData

sealed class DashboardState {
    object Idle : DashboardState()
    object Loading : DashboardState()
    data class Success(val data: DosenData) : DashboardState()
    data class Error(val message: String) : DashboardState()
}