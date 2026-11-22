package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.PopLiveRepository
import com.example.data.attemptAutoSignIn
import com.example.data.authStateFlow
import com.example.data.signOut
import com.example.model.UiState
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GoLiveStudioScreen
import com.example.ui.screens.LiveDiscoveryScreen
import com.example.ui.screens.LiveRoomScreen
import com.example.ui.screens.RankingsScreen
import com.example.ui.screens.WalletAndProfileScreen
import com.example.ui.theme.CoralPink
import com.example.ui.theme.CosmicObsidian
import com.example.ui.theme.VelvetSurface
import com.example.viewmodel.PopLiveViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object NavRoutes {
    const val DISCOVER = "discover"
    const val RANKINGS = "rankings"
    const val GO_LIVE = "go_live"
    const val WALLET = "wallet"
    const val LIVE_ROOM = "live_room/{streamId}"

    fun liveRoom(streamId: String): String = "live_room/$streamId"
}

private data class TopLevelTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val topLevelTabs = listOf(
    TopLevelTab(
        route = NavRoutes.DISCOVER,
        labelRes = R.string.nav_discover,
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        testTag = "nav_discover",
    ),
    TopLevelTab(
        route = NavRoutes.RANKINGS,
        labelRes = R.string.nav_rankings,
        selectedIcon = Icons.Filled.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents,
        testTag = "nav_rankings",
    ),
    TopLevelTab(
        route = NavRoutes.GO_LIVE,
        labelRes = R.string.nav_go_live,
        selectedIcon = Icons.Filled.Videocam,
        unselectedIcon = Icons.Outlined.Videocam,
        testTag = "nav_go_live",
    ),
    TopLevelTab(
        route = NavRoutes.WALLET,
        labelRes = R.string.nav_wallet,
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
        testTag = "nav_wallet",
    ),
)

@Composable
fun PopLiveRoot(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope,
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedPopLiveShell(currentUserId = user.uid)
        }
    }
}

@Composable
private fun AuthenticatedPopLiveShell(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: PopLiveViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                PopLiveViewModel(PopLiveRepository(db), currentUserId)
            }
        },
    )

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val streamsState by viewModel.liveStreamsState.collectAsStateWithLifecycle()
    val followingState by viewModel.followingHostsState.collectAsStateWithLifecycle()
    val rankedCreators by viewModel.rankedCreatorsState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val activeStreamState by viewModel.activeStreamState.collectAsStateWithLifecycle()
    val activeMessagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val activeSeatsState by viewModel.activePartySeatsState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val followingOnly by viewModel.followingOnly.collectAsStateWithLifecycle()
    val partyOnly by viewModel.partyOnly.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomChrome = currentRoute in topLevelTabs.map { it.route }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = CosmicObsidian,
            contentWindowInsets = if (showBottomChrome) WindowInsets.safeDrawing else WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showBottomChrome && !useRail) {
                    NavigationBar(
                        containerColor = VelvetSurface,
                        modifier = Modifier.navigationBarsPadding(),
                    ) {
                        topLevelTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes),
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = CoralPink,
                                    indicatorColor = CoralPink,
                                ),
                                modifier = Modifier.testTag(tab.testTag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (showBottomChrome && useRail) {
                    NavigationRail(containerColor = VelvetSurface) {
                        topLevelTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes),
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.DISCOVER,
                    modifier = Modifier.weight(1f).fillMaxSize(),
                ) {
                    composable(NavRoutes.DISCOVER) {
                        LiveDiscoveryScreen(
                            profileState = profileState,
                            streamsState = streamsState,
                            followingState = followingState,
                            selectedCategory = selectedCategory,
                            followingOnly = followingOnly,
                            partyOnly = partyOnly,
                            searchQuery = searchQuery,
                            isBusy = isBusy,
                            onSelectCategory = viewModel::selectCategory,
                            onToggleFollowingOnly = viewModel::toggleFollowingOnly,
                            onTogglePartyOnly = viewModel::togglePartyOnly,
                            onResetFilters = viewModel::resetDiscoveryFilters,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onWatchStream = { stream ->
                                viewModel.setActiveStream(stream.id)
                                navController.navigate(NavRoutes.liveRoom(stream.id))
                            },
                            onGoLiveClick = { navigateTopLevel(NavRoutes.GO_LIVE) },
                            onLaunchDemoRooms = viewModel::launchDemoStreams,
                            onWalletClick = { navigateTopLevel(NavRoutes.WALLET) },
                        )
                    }

                    composable(NavRoutes.RANKINGS) {
                        val followingList = (followingState as? UiState.Success)?.data ?: emptyList()
                        RankingsScreen(
                            creators = rankedCreators,
                            following = followingList,
                            isBusy = isBusy,
                            onWatchStreamId = { streamId ->
                                viewModel.setActiveStream(streamId)
                                navController.navigate(NavRoutes.liveRoom(streamId))
                            },
                            onToggleFollow = viewModel::toggleFollow,
                            onLaunchDemoRooms = viewModel::launchDemoStreams,
                        )
                    }

                    composable(NavRoutes.GO_LIVE) {
                        GoLiveStudioScreen(
                            isBusy = isBusy,
                            onStartBroadcast = { title, category, pkActive, partyMode, opponent ->
                                viewModel.startBroadcast(
                                    title = title,
                                    category = category,
                                    pkActive = pkActive,
                                    partyMode = partyMode,
                                    pkOpponent = opponent,
                                ) { streamId ->
                                    navController.navigate(NavRoutes.liveRoom(streamId))
                                }
                            },
                        )
                    }

                    composable(NavRoutes.WALLET) {
                        WalletAndProfileScreen(
                            profileState = profileState,
                            transactionsState = transactionsState,
                            isBusy = isBusy,
                            onClaimDailyBonus = viewModel::claimDailyBonus,
                            onRechargeCoins = viewModel::rechargeCoins,
                            onUpdateProfile = viewModel::updateProfile,
                            onSignOut = {
                                signOut(
                                    credentialManager = credentialManager,
                                    scope = scope,
                                )
                            },
                        )
                    }

                    composable(
                        route = NavRoutes.LIVE_ROOM,
                        arguments = listOf(navArgument("streamId") { type = NavType.StringType }),
                    ) { entry ->
                        val streamId = entry.arguments?.getString("streamId") ?: ""
                        DisposableEffect(streamId) {
                            viewModel.setActiveStream(streamId)
                            onDispose {
                                viewModel.setActiveStream(null)
                            }
                        }
                        LiveRoomScreen(
                            currentUserId = currentUserId,
                            profileState = profileState,
                            streamState = activeStreamState,
                            messagesState = activeMessagesState,
                            seatsState = activeSeatsState,
                            followingState = followingState,
                            onSendMessage = viewModel::sendChatMessage,
                            onSendLike = viewModel::sendLike,
                            onSendGift = viewModel::sendGift,
                            onTogglePk = viewModel::togglePk,
                            onTogglePartyMode = viewModel::togglePartyMode,
                            onToggleFollow = viewModel::toggleFollow,
                            onClaimSeat = viewModel::claimPartySeat,
                            onToggleSeatMute = viewModel::toggleSeatMute,
                            onLeaveSeat = { sid, seat -> viewModel.leavePartySeat(sid, seat.id) },
                            onEndBroadcast = viewModel::endBroadcast,
                            onAdjustViewer = viewModel::adjustViewerCount,
                            onGoToWallet = { navigateTopLevel(NavRoutes.WALLET) },
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
