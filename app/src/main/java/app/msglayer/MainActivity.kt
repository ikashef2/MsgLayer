package app.msglayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.msglayer.ui.activity.ActivityScreen
import app.msglayer.ui.ask.AskScreen
import app.msglayer.ui.finance.FinanceScreen
import app.msglayer.ui.inbox.InboxScreen
import app.msglayer.ui.message.MessageDetailScreen
import app.msglayer.ui.navigation.Dest
import app.msglayer.ui.overview.OverviewScreen
import app.msglayer.ui.rules.RulesScreen
import app.msglayer.ui.search.SearchScreen
import app.msglayer.ui.settings.SettingsScreen
import app.msglayer.ui.theme.Bg
import app.msglayer.ui.theme.MsgLayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MsgLayerTheme {
                MsgLayerRoot()
            }
        }
    }
}

private data class Tab(val dest: Dest, val label: String, val icon: ImageVector)

@Composable
fun MsgLayerRoot() {
    val nav = rememberNavController()
    val tabs = listOf(
        Tab(Dest.Overview, "Overview", Icons.Outlined.Home),
        Tab(Dest.Inbox, "Inbox", Icons.Outlined.Inbox),
        Tab(Dest.Finance, "Finance", Icons.Outlined.AccountBalanceWallet),
        Tab(Dest.Ask, "Ask", Icons.Outlined.ChatBubbleOutline),
        Tab(Dest.Search, "Search", Icons.Outlined.Search)
    )
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = route in tabs.map { it.dest.route }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.dest.route,
                            onClick = {
                                nav.navigate(tab.dest.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Dest.Overview.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Dest.Overview.route) {
                OverviewScreen(
                    onAsk = { nav.navigate(Dest.Ask.route) },
                    onOpenMessage = { nav.navigate(Dest.MessageDetail.path(it)) },
                    onOpenActivity = { nav.navigate(Dest.Activity.route) },
                    onOpenSettings = { nav.navigate(Dest.Settings.route) }
                )
            }
            composable(Dest.Inbox.route) {
                InboxScreen(onOpenMessage = { nav.navigate(Dest.MessageDetail.path(it)) })
            }
            composable(Dest.Finance.route) { FinanceScreen() }
            composable(Dest.Ask.route) {
                AskScreen(onOpenMessage = { nav.navigate(Dest.MessageDetail.path(it)) })
            }
            composable(Dest.Search.route) {
                SearchScreen(onOpenMessage = { nav.navigate(Dest.MessageDetail.path(it)) })
            }
            composable(Dest.Rules.route) {
                RulesScreen(onBack = { nav.popBackStack() })
            }
            composable(Dest.Activity.route) {
                ActivityScreen(onBack = { nav.popBackStack() })
            }
            composable(Dest.Settings.route) {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onRules = { nav.navigate(Dest.Rules.route) },
                    onActivity = { nav.navigate(Dest.Activity.route) }
                )
            }
            composable(
                Dest.MessageDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                MessageDetailScreen(
                    messageId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}
