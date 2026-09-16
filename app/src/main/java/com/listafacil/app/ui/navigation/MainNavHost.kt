package com.listafacil.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.listafacil.app.ui.AppViewModel
import com.listafacil.app.ui.screens.comparator.ComparatorScreen
import com.listafacil.app.ui.screens.dashboard.DashboardScreen
import com.listafacil.app.ui.screens.establishments.EstablishmentsScreen
import com.listafacil.app.ui.screens.history.PriceHistoryScreen
import com.listafacil.app.ui.screens.list.ActiveListScreen
import com.listafacil.app.ui.screens.list.AddEditItemScreen
import com.listafacil.app.ui.screens.lists.FinishedListDetailScreen
import com.listafacil.app.ui.screens.lists.FinishedListsScreen
import com.listafacil.app.ui.screens.profile.ProfileScreen
import com.listafacil.app.ui.screens.reports.ReportsScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavHost(
    onThemeChange: (Boolean) -> Unit,
    appViewModel: AppViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val activeList by appViewModel.activeList.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun navigateTop(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Lista Fácil",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(com.listafacil.app.ui.theme.AppDimens.drawerTitlePadding)
                )
                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    selected = currentRoute == Routes.DASHBOARD,
                    onClick = { scope.launch { drawerState.close() }; navigateTop(Routes.DASHBOARD) }
                )
                NavigationDrawerItem(
                    label = { Text("Comparador") },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    selected = currentRoute == Routes.COMPARATOR,
                    onClick = { scope.launch { drawerState.close() }; navigateTop(Routes.COMPARATOR) }
                )
                NavigationDrawerItem(
                    label = { Text("Relatórios") },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    selected = currentRoute == Routes.REPORTS,
                    onClick = { scope.launch { drawerState.close() }; navigateTop(Routes.REPORTS) }
                )
                NavigationDrawerItem(
                    label = { Text("Últimas listas") },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                    selected = currentRoute == Routes.FINISHED_LISTS,
                    onClick = { scope.launch { drawerState.close() }; navigateTop(Routes.FINISHED_LISTS) }
                )
                NavigationDrawerItem(
                    label = { Text("Estabelecimentos") },
                    icon = { Icon(Icons.Default.Place, contentDescription = null) },
                    selected = currentRoute == Routes.ESTABLISHMENTS,
                    onClick = { scope.launch { drawerState.close() }; navigateTop(Routes.ESTABLISHMENTS) }
                )
                NavigationDrawerItem(
                    label = { Text("Perfil") },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                    selected = currentRoute == Routes.PROFILE,
                    onClick = { scope.launch { drawerState.close() }; navigateTop(Routes.PROFILE) }
                )

                // Seção contextual: lista ativa aberta (requisito §13)
                val active = activeList
                if (active != null) {
                    HorizontalDivider()
                    Text(
                        "Lista ativa",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(com.listafacil.app.ui.theme.AppDimens.drawerSectionPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text(active.name) },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                        selected = currentRoute?.startsWith("active_list") == true,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Routes.activeList(active.id))
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("Adicionar produto") },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Routes.addItem(active.id))
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("Finalizar lista") },
                        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Routes.activeList(active.id)) {
                                popUpTo(Routes.activeList(active.id)) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(titleForRoute(currentRoute)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.DASHBOARD,
                modifier = Modifier.padding(padding)
            ) {
                composable(Routes.DASHBOARD) {
                    DashboardScreen(
                        onOpenActiveList = { navController.navigate(Routes.activeList(it)) },
                        onOpenFinishedLists = { navigateTop(Routes.FINISHED_LISTS) },
                        onOpenReports = { navigateTop(Routes.REPORTS) },
                        onOpenProfile = { navigateTop(Routes.PROFILE) },
                        onOpenComparator = { navigateTop(Routes.COMPARATOR) }
                    )
                }
                composable(
                    Routes.ACTIVE_LIST,
                    arguments = listOf(navArgument("listId") { type = NavType.StringType })
                ) {
                    ActiveListScreen(
                        onAddItem = { listId -> navController.navigate(Routes.addItem(listId)) },
                        onEditItem = { listId, itemId ->
                            navController.navigate(Routes.addItem(listId, itemId))
                        },
                        onViewHistory = { productId ->
                            navController.navigate(Routes.history(productId))
                        },
                        onFinished = {
                            navController.navigate(Routes.DASHBOARD) {
                                popUpTo(Routes.DASHBOARD) { inclusive = true }
                            }
                        }
                    )
                }
                composable(
                    Routes.ADD_ITEM,
                    arguments = listOf(
                        navArgument("listId") { type = NavType.StringType },
                        navArgument("itemId") { type = NavType.StringType; nullable = true; defaultValue = null }
                    )
                ) {
                    AddEditItemScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    Routes.HISTORY,
                    arguments = listOf(navArgument("productId") { type = NavType.StringType })
                ) {
                    PriceHistoryScreen()
                }
                composable(Routes.FINISHED_LISTS) {
                    FinishedListsScreen(
                        onOpenDetail = { navController.navigate(Routes.finishedDetail(it)) }
                    )
                }
                composable(
                    Routes.FINISHED_DETAIL,
                    arguments = listOf(navArgument("listId") { type = NavType.StringType })
                ) {
                    FinishedListDetailScreen(onOpenReports = { navigateTop(Routes.REPORTS) })
                }
                composable(Routes.REPORTS) { ReportsScreen() }
                composable(Routes.COMPARATOR) { ComparatorScreen() }
                composable(Routes.PROFILE) {
                    ProfileScreen(onThemeChange = onThemeChange)
                }
                composable(Routes.ESTABLISHMENTS) {
                    EstablishmentsScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

private fun titleForRoute(route: String?): String = when {
    route == null -> "Lista Fácil"
    route.startsWith("active_list") -> "Lista de compras"
    route.startsWith("add_item") -> "Produto"
    route.startsWith("history") -> "Histórico de preços"
    route == Routes.FINISHED_LISTS -> "Últimas listas"
    route.startsWith("finished_detail") -> "Lista finalizada"
    route == Routes.REPORTS -> "Relatórios"
    route == Routes.COMPARATOR -> "Comparador"
    route == Routes.PROFILE -> "Perfil"
    route == Routes.ESTABLISHMENTS -> "Estabelecimentos"
    else -> "Lista Fácil"
}
