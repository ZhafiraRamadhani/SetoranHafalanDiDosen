package com.example.setoranhafalandosen.tampilan.navigasi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.setoranhafalandosen.tampilan.dashboard.HomeScreen
import com.example.setoranhafalandosen.tampilan.login.LoginScreen
import com.example.setoranhafalandosen.tampilan.setoran.SetoranScreen


@Composable
fun SetupNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(navController = navController)
        }
        composable("dashboard") {
            HomeScreen(navController = navController)
        }
        composable(
            route = "setoran/{nim}",
            arguments = listOf(navArgument("nim") { type = NavType.StringType })
        ) { backStackEntry ->
            val nim = backStackEntry.arguments?.getString("nim") ?: ""
            SetoranScreen(navController = navController, nim = nim)
        }
    }
}
