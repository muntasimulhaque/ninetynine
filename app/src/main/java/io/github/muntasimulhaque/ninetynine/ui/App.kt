package io.github.muntasimulhaque.ninetynine.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.muntasimulhaque.ninetynine.ui.NamesViewModel
import io.github.muntasimulhaque.ninetynine.ui.about.AboutScreen
import io.github.muntasimulhaque.ninetynine.ui.bookmarks.BookmarksScreen
import io.github.muntasimulhaque.ninetynine.ui.detail.DetailScreen
import io.github.muntasimulhaque.ninetynine.ui.home.HomeScreen
import io.github.muntasimulhaque.ninetynine.ui.memorize.FlashcardsScreen
import io.github.muntasimulhaque.ninetynine.ui.memorize.LearnedScreen
import io.github.muntasimulhaque.ninetynine.ui.memorize.MemorizeScreen
import io.github.muntasimulhaque.ninetynine.ui.memorize.QuizScreen
import io.github.muntasimulhaque.ninetynine.ui.settings.SettingsScreen
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalMotionScale
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.Names99Theme
import io.github.muntasimulhaque.ninetynine.ui.theme.components.LocalBottomBarOverlay

/** Which list a name page pages through. Absent means all 99. */
private const val SCOPE_ALL = "all"
private const val SCOPE_BOOKMARKS = "bookmarks"

/** The routes a launcher shortcut may name in its [MainActivity.EXTRA_START_ROUTE]. */
private const val ROUTE_FLASHCARDS = "flashcards"
private const val ROUTE_QUIZ = "quiz"

@Composable
internal fun App(
    startNumber: Int,
    startRoute: String?,
    onStartNumberConsumed: () -> Unit,
    onStartRouteConsumed: () -> Unit,
) {
    val viewModel: NamesViewModel = viewModel()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val textScale by viewModel.textScale.collectAsStateWithLifecycle()

    Names99Theme(themeMode = themeMode, textScale = textScale) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val showBottomBar = currentRoute in topLevelRoutes.map { it.route }

        // The floating bar overlays the lists (scroll-under), so the screens
        // grow their bottom content padding by the bar's occupied height,
        // measured, not guessed: the bar's own height follows the system font
        // scale and the gesture strip follows the device's navigation mode
        // (24dp gesture, 48dp three-button), so no constant fits every
        // device. The bar reports its laid-out size from below; a pushed
        // screen hides the bar and the clearance collapses to zero.
        val density = LocalDensity.current
        var bottomBarHeightPx by remember { mutableIntStateOf(0) }
        val bottomBarClearance = with(density) { bottomBarHeightPx.toDp() }

        // Re-tapping the tab you are already on returns to the top of its
        // list. Scroll to name 80 and the only way back to the daily card
        // used to be to fling through 79 rows; there is no fast-scroller, by
        // an earlier and correct decision. The contract is one contract, so
        // every tab answers it: the two lazy lists hoist their LazyListState,
        // and the two scroll-driven tabs (Memorize, Settings, Columns that
        // do scroll at large font scales and on short phones) hoist their
        // ScrollState the same way. All hoisted here so the bar can reach
        // them.
        val namesListState = rememberLazyListState()
        val bookmarksListState = rememberLazyListState()
        val memorizeScrollState = rememberScrollState()
        val settingsScrollState = rememberScrollState()

        LaunchedEffect(startNumber) {
            if (startNumber in 1..99) {
                onStartNumberConsumed()
                // Without this, tapping the widget on successive mornings
                // (without pressing Back in between) stacks a name page on top
                // of a name page, and Back then lands on an identical screen.
                navController.navigate("detail/$startNumber") { launchSingleTop = true }
            }
        }

        // A launcher shortcut lands mid-book: long-pressing the icon offers
        // the practice screens that otherwise sit two taps in. The memorize
        // tab goes on the stack first, so Back from the shortcut's screen
        // arrives where a reader who walked there would be. Unknown routes
        // are ignored entirely: the extra is exported, so any app can send
        // any string, and only the two shortcut routes may move the reader.
        LaunchedEffect(startRoute) {
            val route = startRoute ?: return@LaunchedEffect
            if (route != ROUTE_FLASHCARDS && route != ROUTE_QUIZ) {
                onStartRouteConsumed()
                return@LaunchedEffect
            }
            onStartRouteConsumed()
            navController.navigate("memorize") {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            when (route) {
                ROUTE_FLASHCARDS -> navController.navigate("flashcards") {
                    launchSingleTop = true
                }
                ROUTE_QUIZ -> navController.navigate("quiz") {
                    launchSingleTop = true
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            val motionScale = LocalMotionScale.current
            // The provider MUST reach the screens: scoped to the bar alone it
            // tells them nothing, and the last rows then sit behind the plate
            // (shipped that way once, 1.15/1.16 hid name 99 behind the bar).
            CompositionLocalProvider(
                LocalBottomBarOverlay provides if (showBottomBar) bottomBarClearance else 0.dp,
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "names",
                    modifier = Modifier.fillMaxSize(),
                    // Pushed screens rise gently into place; pops sink away.
                    enterTransition = {
                        fadeIn(Motion.spec(motionScale, Motion.GENTLE, easing = Motion.Settle)) +
                            slideInVertically(Motion.spec(motionScale, Motion.GENTLE, easing = Motion.Settle)) { it / 24 }
                    },
                    exitTransition = { fadeOut(Motion.spec(motionScale, Motion.QUICK)) },
                    popEnterTransition = { fadeIn(Motion.spec(motionScale, Motion.GENTLE)) },
                    popExitTransition = {
                        fadeOut(Motion.spec(motionScale, Motion.GENTLE)) +
                            slideOutVertically(Motion.spec(motionScale, Motion.GENTLE, easing = Motion.Settle)) { it / 24 }
                    },
                ) {
                    composable("names", enterTransition = tabFade(motionScale), exitTransition = tabFadeOut(motionScale)) {
                        HomeScreen(
                            viewModel = viewModel,
                            onNameClick = { number -> navController.navigate("detail/$number") },
                            listState = namesListState,
                        )
                    }
                    // The scope says which list the reader arrived from, and so
                    // which list the chevrons walk. Optional, so every existing
                    // entry point (the names list, the widget, the notification)
                    // keeps landing on all 99 without saying anything.
                    composable(
                        "detail/{number}?scope={scope}",
                        arguments = listOf(
                            navArgument("number") { type = NavType.IntType },
                            navArgument("scope") {
                                type = NavType.StringType
                                defaultValue = SCOPE_ALL
                            },
                        ),
                    ) { entry ->
                        DetailScreen(
                            viewModel = viewModel,
                            startNumber = entry.arguments?.getInt("number") ?: 1,
                            bookmarksOnly =
                                entry.arguments?.getString("scope") == SCOPE_BOOKMARKS,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable("memorize", enterTransition = tabFade(motionScale), exitTransition = tabFadeOut(motionScale)) {
                        MemorizeScreen(
                            viewModel = viewModel,
                            onFlashcards = { navController.navigate("flashcards") },
                            onQuiz = { navController.navigate("quiz") },
                            onLearned = { navController.navigate("learned") },
                            scrollState = memorizeScrollState,
                        )
                    }
                    // The two list tabs offer their empties the same way out: the
                    // names list, where a bookmark or a learned tick begins.
                    val browseNames: () -> Unit = {
                        navController.navigate("names") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    composable("bookmarks", enterTransition = tabFade(motionScale), exitTransition = tabFadeOut(motionScale)) {
                        BookmarksScreen(
                            viewModel = viewModel,
                            // Paged within the kept names, not across all 99: the
                            // chevrons should walk the list you are looking at.
                            onNameClick = { number ->
                                navController.navigate("detail/$number?scope=$SCOPE_BOOKMARKS")
                            },
                            onBrowseNames = browseNames,
                            listState = bookmarksListState,
                        )
                    }
                    composable("flashcards") {
                        FlashcardsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable("learned") {
                        LearnedScreen(
                            viewModel = viewModel,
                            onNameClick = { number -> navController.navigate("detail/$number") },
                            onBrowseNames = browseNames,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable("quiz") {
                        QuizScreen(
                            viewModel = viewModel,
                            onNameClick = { number -> navController.navigate("detail/$number") },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable("settings", enterTransition = tabFade(motionScale), exitTransition = tabFadeOut(motionScale)) {
                        SettingsScreen(
                            viewModel = viewModel,
                            onAbout = { navController.navigate("about") },
                            scrollState = settingsScrollState,
                        )
                    }
                    composable("about") {
                        AboutScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
            AnimatedVisibility(
                visible = showBottomBar,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(Motion.tween(Motion.QUICK)) + slideInVertically(Motion.tween(Motion.GENTLE)) { it },
                exit = fadeOut(Motion.tween(Motion.QUICK)) + slideOutVertically(Motion.tween(Motion.GENTLE)) { it },
            ) {
                QuietBottomBar(
                    navController = navController,
                    currentRoute = currentRoute,
                    modifier = Modifier.onSizeChanged { bottomBarHeightPx = it.height },
                    listStateFor = { route ->
                        when (route) {
                            "names" -> namesListState
                            "bookmarks" -> bookmarksListState
                            else -> null
                        }
                    },
                    scrollStateFor = { route ->
                        when (route) {
                            "memorize" -> memorizeScrollState
                            "settings" -> settingsScrollState
                            else -> null
                        }
                    },
                )
            }
        }
    }
}

/** Tab switches crossfade, only pushed detail screens use the rising motion. */
private fun tabFade(motionScale: Float):
    (androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() ->
    androidx.compose.animation.EnterTransition?) = {
    fadeIn(Motion.spec(motionScale, Motion.GENTLE))
}

private fun tabFadeOut(motionScale: Float):
    (androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() ->
    androidx.compose.animation.ExitTransition?) = {
    fadeOut(Motion.spec(motionScale, Motion.QUICK))
}
