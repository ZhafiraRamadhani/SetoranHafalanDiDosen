package com.example.setoranhafalandosen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.setoranhafalandosen.ui.theme.SetoranHafalanDosenTheme
import androidx.navigation.compose.rememberNavController
import com.example.setoranhafalandosen.tampilan.navigasi.SetupNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SetoranHafalanDosenTheme {
                val navController = rememberNavController()
                SetupNavGraph(navController = navController) // ✅ cukup ini saja
            }
        }
    }
}

