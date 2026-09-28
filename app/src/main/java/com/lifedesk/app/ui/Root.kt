package com.lifedesk.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lifedesk.app.ui.screens.AuthScreen
import com.lifedesk.app.ui.screens.EditScreen
import com.lifedesk.app.ui.screens.LockScreen
import com.lifedesk.app.ui.screens.HomeScreen
import com.lifedesk.app.ui.screens.ItemDetailScreen
import com.lifedesk.app.ui.screens.ItemsScreen
import com.lifedesk.app.ui.screens.ScanScreen
import com.lifedesk.app.ui.screens.SettingsScreen
import com.lifedesk.app.ui.screens.SubscriptionsScreen
import com.lifedesk.app.ui.screens.WelcomeScreen

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Outlined.Home),
    Tab("items", "Everything", Icons.Outlined.FolderOpen),
    Tab("scan", "Scan", Icons.Filled.CameraAlt),
    Tab("subscriptions", "Money", Icons.Outlined.Subscriptions),
    Tab("settings", "Settings", Icons.Outlined.Settings),
)

fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo("home") { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun LifeDeskRoot(vm: AppViewModel) {
    val nav = rememberNavController()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val pendingRoute by vm.pendingRoute.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val scan by vm.scan.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route?.substringBefore('?')
    // Fixed for the lifetime of the NavHost; the welcome screen pops itself once onboarding is done.
    val startRoute = remember { if (settings.onboarded) "home" else "welcome" }

    LaunchedEffect(pendingRoute, settings.onboarded) {
        val r = pendingRoute ?: return@LaunchedEffect
        if (!settings.onboarded) return@LaunchedEffect
        vm.consumeRoute()
        nav.navigate(r) { launchSingleTop = true }
    }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (current in tabs.map { it.route }) {
                NavigationBar {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = current == t.route,
                            onClick = { nav.goTab(t.route) },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = startRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable("welcome") {
                WelcomeScreen(vm) {
                    // With accounts available, offer sign-up right after onboarding (skippable).
                    val next = if (vm.accountsAvailable && vm.account.value == null) "auth?first=true" else "home"
                    nav.navigate(next) { popUpTo("welcome") { inclusive = true } }
                }
            }
            composable("auth?first={first}", arguments = listOf(navArgument("first") { type = NavType.BoolType; defaultValue = false })) { entry ->
                val first = entry.arguments?.getBoolean("first") ?: false
                AuthScreen(vm, onDone = {
                    if (first) nav.navigate("home") { popUpTo(0) { inclusive = true } } else nav.popBackStack()
                }, skipLabel = if (first) "Continue without an account" else null)
            }
            composable("home") { HomeScreen(vm, nav) }
            composable(
                "items?status={status}&q={q}",
                arguments = listOf(
                    navArgument("status") { type = NavType.StringType; defaultValue = "ALL" },
                    navArgument("q") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                ItemsScreen(vm, nav, entry.arguments?.getString("status") ?: "ALL", entry.arguments?.getString("q") ?: "")
            }
            composable(
                "scan?replace={replace}",
                arguments = listOf(navArgument("replace") { type = NavType.LongType; defaultValue = -1L }),
            ) { entry ->
                ScanScreen(vm, nav, entry.arguments?.getLong("replace")?.takeIf { it > 0 })
            }
            composable("edit") { EditScreen(vm, nav) }
            composable("item/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                ItemDetailScreen(vm, nav, entry.arguments?.getLong("id") ?: 0L)
            }
            composable("subscriptions") { SubscriptionsScreen(vm, nav) }
            composable("settings") { SettingsScreen(vm, nav) }
        }
    }

    val locked by vm.locked.collectAsStateWithLifecycle()
    if (locked) LockScreen(vm)

    (scan as? ScanState.Failed)?.let { failed ->
        AlertDialog(
            onDismissRequest = vm::dismissScanError,
            confirmButton = { TextButton(onClick = vm::dismissScanError) { Text("OK") } },
            title = { Text("Something went wrong") },
            text = { Text(failed.message) },
        )
    }
}
