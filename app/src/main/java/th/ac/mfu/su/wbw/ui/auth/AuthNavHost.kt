package th.ac.mfu.su.wbw.ui.auth

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private const val ROUTE_LOGIN = "login"
private const val ROUTE_REGISTER = "register"

/** The logged-out graph: login ⇄ register. Login success flips the session. */
@Composable
fun AuthNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ROUTE_LOGIN) {
        composable(ROUTE_LOGIN) {
            LoginScreen(onNavigateToRegister = { navController.navigate(ROUTE_REGISTER) })
        }
        composable(ROUTE_REGISTER) {
            RegisterScreen(onBack = { navController.popBackStack() })
        }
    }
}
