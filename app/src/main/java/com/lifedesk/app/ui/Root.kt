package com.lifedesk.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifedesk.app.ui.components.NeonBackground
import com.lifedesk.app.ui.theme.Neon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.DonutLarge
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
    Tab("subscriptions", "Money", Icons.Outlined.DonutLarge),
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

    NeonBackground {
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                androidx.compose.material3.Snackbar(
                    data, containerColor = Neon.Surface2, contentColor = Neon.Text, actionColor = Neon.Cyan,
                    shape = RoundedCornerShape(16.dp),
                )
            }
        },
        bottomBar = {
            if (current in tabs.map { it.route }) FloatingNavBar(current) { nav.goTab(it) }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = startRoute,
            modifier = Modifier.padding(padding),
            // Futuristic, subtle: screens fade and zoom in slightly.
            enterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(260)) +
                androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(260), initialScale = 0.96f) },
            exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(180)) },
            popEnterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(260)) },
            popExitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(180)) +
                androidx.compose.animation.scaleOut(androidx.compose.animation.core.tween(180), targetScale = 0.96f) },
        ) {
            composable("welcome") {
                WelcomeScreen(vm) {
                    // With accounts available, offer sign-up right after onboarding (skippable).
                    val next = if (vm.account.value == null) "auth?first=true" else "home"
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


/** Floating glass navigation bar with a raised gradient scan button in the middle. */
@Composable
private fun FloatingNavBar(current: String?, onSelect: (String) -> Unit) {
    Box(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            Modifier.fillMaxWidth().height(66.dp).clip(RoundedCornerShape(26.dp))
                .background(Brush.verticalGradient(listOf(Neon.Surface2.copy(alpha = 0.96f), Neon.Surface.copy(alpha = 0.96f))))
                .border(1.dp, Brush.linearGradient(listOf(Neon.Cyan.copy(alpha = 0.4f), Neon.Stroke, Neon.Violet.copy(alpha = 0.4f))), RoundedCornerShape(26.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { t ->
                if (t.route == "scan") {
                    Spacer(Modifier.weight(1f))
                } else {
                    val selected = current == t.route
                    val tint by animateColorAsState(if (selected) Neon.Cyan else Neon.Muted, label = "tint")
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clickable(
                            interactionSource = remember { MutableInteractionSource() }, indication = null,
                        ) { onSelect(t.route) },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(t.icon, t.label, tint = tint, modifier = Modifier.size(22.dp))
                        Text(t.label, fontSize = 10.sp, color = tint, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        Box(
                            Modifier.padding(top = 3.dp).size(width = if (selected) 16.dp else 0.dp, height = 3.dp)
                                .clip(RoundedCornerShape(2.dp)).background(Neon.Primary),
                        )
                    }
                }
            }
        }
        // Raised scan button
        Box(
            Modifier.padding(bottom = 22.dp).size(66.dp)
                .drawBehind { drawCircle(Brush.radialGradient(listOf(Neon.Cyan.copy(alpha = 0.5f), Color.Transparent)), radius = size.minDimension * 0.75f) }
                .clip(CircleShape).background(Neon.Primary)
                .border(3.dp, Neon.Bg, CircleShape)
                .clickable { onSelect("scan") },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.CameraAlt, "Scan", tint = Color(0xFF06101E), modifier = Modifier.size(28.dp))
        }
    }
}
