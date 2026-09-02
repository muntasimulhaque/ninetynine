package io.github.muntasimulhaque.ninetynine

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.muntasimulhaque.ninetynine.daily.DailyNameWidget
import io.github.muntasimulhaque.ninetynine.daily.DailyScheduler
import io.github.muntasimulhaque.ninetynine.data.Prefs
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
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalDarkTheme
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalMotionScale
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalPureBlackTheme
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.Names99Theme
import io.github.muntasimulhaque.ninetynine.ui.theme.components.FitText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.FloatingBar
import io.github.muntasimulhaque.ninetynine.ui.theme.components.LocalBottomBarOverlay
import io.github.muntasimulhaque.ninetynine.ui.theme.components.barMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.tabLabelStyle
import io.github.muntasimulhaque.ninetynine.util.DailyName
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    private var startNumber by mutableIntStateOf(-1)

    /** The launcher-shortcut deep link, if this launch came from one. */
    private var startRoute by mutableStateOf<String?>(null)

    /** False until Compose's first composition has committed; holds the splash. */
    private var contentReady = false

    /**
     * The local day the widget last got its onResume nudge for. The nudge
     * exists so a phone opened across midnight never shows the widget
     * yesterday's name; within one day, re-rendering on every resume only
     * repeated a full Glance composition on the main thread for a card that
     * already shows today. The daily-name worker at 00:05 remains the real
     * refresher, and process death resets this, so the first resume after a
     * cold start always renders.
     */
    private var widgetNudgeDay: Int = Int.MIN_VALUE

    /**
     * The one-time ask for POST_NOTIFICATIONS. Registered at construction —
     * the contract requires that to happen before the activity is STARTED.
     * The reminder itself is on by default (see [maybeAskForNotifications]);
     * a denial writes the pref off, so the Settings switch, the scheduler and
     * the worker all agree that nothing should be posted.
     */
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) onReminderPermissionDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        // Hold the system splash until the app's first frame is committed.
        // Without this, a slow device can dismiss the splash a frame or two
        // before Compose draws — a flash of bare window background between
        // splash and app. The condition is polled every frame on the main
        // thread, so a plain boolean is enough; SideEffect fires exactly when
        // the first composition has been applied, releasing the splash as the
        // real content lands.
        splashScreen.setKeepOnScreenCondition { !contentReady }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Re-anchor here, not in Application.onCreate. This runs only when a
        // person actually opens the app, so it cannot cancel a worker that
        // WorkManager just started the process to run. Doze deferrals still get
        // corrected — every launch pins both schedules back to their times.
        // The running-check keeps even the seconds-wide window from cancelling
        // a worker that is mid-run at the instant the app opens.
        lifecycleScope.launch { DailyScheduler.reanchorSchedules(this@MainActivity) }

        // The reminder is on by default; on 13+ its consent is the system
        // permission dialog, so a fresh install is asked once, here. Details
        // in [maybeAskForNotifications].
        lifecycleScope.launch { maybeAskForNotifications() }

        // Process death replays the ORIGINAL launch intent (the removal below
        // never propagates to the system's ActivityRecord), so a restored
        // activity would force-navigate to the deep-linked name again. The
        // extra was already consumed and navigated before the death.
        if (savedInstanceState == null) {
            startNumber = consumeNameNumber(intent)
            startRoute = consumeStartRoute(intent)
        }
        setContent {
            SideEffect { contentReady = true }
            App(
                startNumber,
                startRoute,
                onStartNumberConsumed = { startNumber = -1 },
                onStartRouteConsumed = { startRoute = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startNumber = consumeNameNumber(intent)
        startRoute = consumeStartRoute(intent)
    }

    /**
     * The home screen recomputes the daily name on every resume; the widget
     * only refreshes when its worker runs. Nudging it here keeps the two
     * surfaces from showing different names after midnight — gated to a real
     * day change, so an ordinary return to the app costs no render (see
     * [widgetNudgeDay]).
     */
    override fun onResume() {
        super.onResume()
        val today = DailyName.numberFor(System.currentTimeMillis())
        if (today == widgetNudgeDay) return
        widgetNudgeDay = today
        // Glance's updateAll can race its own initialisation on a cold start;
        // a throw here would kill the process the reader just opened. The
        // widget refreshes on its own worker run anyway, so a failed nudge is
        // a skipped refresh, never a crash.
        lifecycleScope.launch {
            runCatching { DailyNameWidget().updateAll(this@MainActivity) }
        }
    }

    /**
     * Asks for POST_NOTIFICATIONS once, on the first launch of a reader who
     * wants the reminder (it is on by default). The ask only fires when all
     * of these hold:
     *
     * - API 33+: below that there is no runtime permission to ask for, and
     *   the reminder simply works — the platform's own default for every
     *   installed app.
     * - the permission is not already granted (via system settings or a
     *   restore).
     * - it has never been asked before, on any launch — the flag is written
     *   before the dialog opens, so a process death mid-dialog never nags.
     * - the reminder is actually wanted: a reader whose pref says off
     *   (toggled off, or a denial from an earlier ask) is never asked.
     *
     * A grant needs no wiring: the schedule re-anchors on every launch and
     * the worker re-checks the permission at post time. A denial writes the
     * pref off and cancels the scheduled work, so the Settings switch shows
     * the truth and no worker wakes to no-op.
     */
    private suspend fun maybeAskForNotifications() {
        if (Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return
        val prefs = Prefs(applicationContext)
        if (prefs.notificationsAsked.first()) return
        if (!prefs.dailyEnabled.first()) return
        prefs.setNotificationsAsked()
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /** The reader said no to the one ask: the reminder follows them to off. */
    private fun onReminderPermissionDenied() {
        lifecycleScope.launch {
            Prefs(applicationContext).setDailyEnabled(false)
            DailyScheduler.cancelNotification(applicationContext)
        }
    }

    /** Reads the extra, then removes it so a configuration change can't replay the navigation. */
    private fun consumeNameNumber(intent: Intent?): Int = runCatching {
        // This activity is exported, so any app on the device can launch it
        // with arbitrary extras. Reading one unparcels the whole bundle, and a
        // crafted extra naming a class this app does not have throws
        // BadParcelableException right here in onCreate.
        val number = intent?.getIntExtra(EXTRA_NAME_NUMBER, -1) ?: -1
        intent?.removeExtra(EXTRA_NAME_NUMBER)
        number
    }.getOrDefault(-1)

    /** Same consume-and-remove discipline as [consumeNameNumber], for shortcut routes. */
    private fun consumeStartRoute(intent: Intent?): String? = runCatching {
        val route = intent?.getStringExtra(EXTRA_START_ROUTE)
        intent?.removeExtra(EXTRA_START_ROUTE)
        route
    }.getOrNull()

    companion object {
        const val EXTRA_NAME_NUMBER = "nameNumber"

        /** Carried by the launcher shortcuts (res/xml/shortcuts.xml). */
        const val EXTRA_START_ROUTE = "startRoute"
    }
}

/** Which list a name page pages through. Absent means all 99. */
private const val SCOPE_ALL = "all"
private const val SCOPE_BOOKMARKS = "bookmarks"

/** The routes a launcher shortcut may name in its [MainActivity.EXTRA_START_ROUTE]. */
private const val ROUTE_FLASHCARDS = "flashcards"
private const val ROUTE_QUIZ = "quiz"

private data class TopLevelRoute(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    /** The quieter outline a tab wears whenever ANOTHER tab is selected. */
    val iconResting: ImageVector,
)

/*
 * Four destinations: read the names, practise them, keep the ones you turn to,
 * and set the book. Settings joined the bar as a full tab (owner decision,
 * 1.18), taking the rightmost, quietest slot; the corner gear every bar
 * carried is gone, so the top bars hold content only — Home's title renders
 * larger in the freed corner, still fitted by FitText. The measured cost the
 * owner accepted: at a 2.0 system font scale on a 320dp phone the longest
 * label ("BOOKMARKS") renders at FitText scale ~0.49 — about 8.9sp, the 9sp
 * base — above the 0.40 floor and never clipping; at the default scale on
 * every phone all four labels render whole.
 */
private val topLevelRoutes = listOf(
    TopLevelRoute("names", R.string.nav_names, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    TopLevelRoute("memorize", R.string.memorize, Icons.Filled.School, Icons.Outlined.School),
    TopLevelRoute("bookmarks", R.string.bookmarks, Icons.Filled.Bookmark, Icons.Outlined.Bookmark),
    TopLevelRoute("settings", R.string.settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
private fun App(
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
        // grow their bottom content padding by the bar's occupied height —
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
        // and the two scroll-driven tabs (Memorize, Settings — Columns that
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
                // Without this, tapping the widget on successive mornings —
                // without pressing Back in between — stacks a name page on top
                // of a name page, and Back then lands on an identical screen.
                navController.navigate("detail/$startNumber") { launchSingleTop = true }
            }
        }

        // A launcher shortcut lands mid-book: long-pressing the icon offers
        // the practice screens that otherwise sit two taps in. The memorize
        // tab goes on the stack first, so Back from the shortcut's screen
        // arrives where a reader who walked there would be.
        LaunchedEffect(startRoute) {
            val route = startRoute ?: return@LaunchedEffect
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
            // (shipped that way once — 1.15/1.16 hid name 99 behind the bar).
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
                    // entry point — the names list, the widget, the notification —
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

/** Tab switches crossfade — only pushed detail screens use the rising motion. */
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

/**
 * The bottom bar's vessel: the four tabs themselves ([BottomBarTabs]) inside
 * the floating, scroll-under plate — [FloatingBar]'s capsule, halo and
 * transparent gesture strip (shared with the name page's capsule, in
 * PageParts).
 */
@Composable
private fun QuietBottomBar(
    navController: NavHostController,
    currentRoute: String?,
    listStateFor: (String) -> LazyListState?,
    scrollStateFor: (String) -> ScrollState?,
    modifier: Modifier = Modifier,
) {
    FloatingBar(modifier = modifier) {
        BottomBarTabs(navController, currentRoute, listStateFor, scrollStateFor)
    }
}

/**
 * The three tabs, shared verbatim between the two vessels. The Row's own
 * modifier is the group semantics plus the padding the vessels agree on;
 * [barMeasure] keeps the page's column cap so screens may keep sizing against
 * the bar — the vessel above it owns the inset, the plate and the
 * gesture-strip spacing.
 */
@Composable
private fun BottomBarTabs(
    navController: NavHostController,
    currentRoute: String?,
    listStateFor: (String) -> LazyListState?,
    scrollStateFor: (String) -> ScrollState?,
) {
    val scope = rememberCoroutineScope()
    val motionScale = LocalMotionScale.current
    Row(
        // The bar takes its height from the tabs rather than fixing it, so a
        // large font grows the bar instead of clipping the labels. The
        // minimum lives on each tab: an unweighted child of a Column is
        // measured against all the remaining space, so a heightIn here would
        // let fillMaxHeight swallow the screen.
        modifier = Modifier
            .barMeasure()
            .padding(vertical = 4.dp, horizontal = 6.dp)
            .selectableGroup(),
    ) {
        topLevelRoutes.forEach { item ->
            val selected = currentRoute == item.route
            val tint by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = Motion.tween(Motion.QUICK),
                label = "tabTint",
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 54.dp)
                    // A tap's highlight wears the bar's own capsule register:
                    // the selectable's ripple is bounded to the tab's
                    // rectangle, and this clip crops it to a stadium before it
                    // flashes — nothing on a tab reaches the corners, so the
                    // clip costs no content. Same 50% corner as the plate.
                    .clip(RoundedCornerShape(50))
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = {
                            // Already here: go back to the top of the
                            // page instead of navigating nowhere.
                            // restoreState would otherwise restore the
                            // scroll position, so re-tapping did
                            // literally nothing. Snapped instantly at
                            // animator scale 0, like every Motion.*
                            // animation in the app. Every tab answers:
                            // the two lazy lists scroll to item 0, the
                            // two scroll-driven Columns to offset 0.
                            val here = listStateFor(item.route)
                            val scrollHere = scrollStateFor(item.route)
                            if (selected && (here != null || scrollHere != null)) {
                                scope.launch {
                                    if (here != null) {
                                        if (motionScale == 0f) here.scrollToItem(0)
                                        else here.animateScrollToItem(0)
                                    } else {
                                        if (motionScale == 0f) scrollHere?.scrollTo(0)
                                        else scrollHere?.animateScrollTo(0)
                                    }
                                }
                                return@selectable
                            }
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Icon(
                    // Shape carries selection beside colour and weight:
                    // the chosen tab's glyph fills, the resting ones
                    // stand open — Google's own bar grammar, still no
                    // pill and no motion. Greyscale screens and the
                    // ~8% who cannot trust hue get a third channel.
                    if (selected) item.icon else item.iconResting,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.height(3.dp))
                // Chrome, not reading matter: the reader's slider
                // cannot move it — 9sp × the device factor only —
                // so it can never clip. The device factor DOES
                // apply: a 7-inch bar gives each tab 200dp and a
                // 10-inch 427dp, room enough for the wider labels,
                // while a phone stays at 9sp. System font scaling
                // does still apply, so the labels fit themselves
                // rather than ellipsizing to "MEM…".
                //
                // "BOOKMARKS" is the longest label the bar carries —
                // 6.681 em in Spectral Medium, so 141.9dp at a system
                // font scale of 2.0, against MEMORIZE's 121.5dp. At
                // four tabs a 320dp phone gives each one 70.0dp, so
                // the worst case is ~0.49 — about 8.9sp, the 9sp base
                // — and the 0.40 floor is never approached. At the
                // default font scale every phone, down to 320dp,
                // renders all four labels whole. On the 7-inch class
                // the labels start 1.125× wider and the tab slots are
                // 200dp; on the 10-inch class 1.25× wider against
                // 427dp — the room grows faster than the ink on every
                // device the bar serves.
                //
                // That worst case is the measured cost the owner
                // accepted when Settings joined as the fourth tab
                // (owner decision, 1.18); see the note on
                // topLevelRoutes. Selection is carried by weight as
                // well as colour.
                // Colour alone failed WCAG 1.4.1: primary against
                // onSurfaceVariant is 1.22:1 in light and 1.20:1 in
                // dark — the two differ almost purely in hue, so a
                // deuteranope or protanope (~8% of men), a greyscale
                // screen or a high-contrast mode could not tell which
                // tab was active at all.
                FitText(
                    text = stringResource(item.labelRes).uppercase(),
                    style = tabLabelStyle().copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                    color = tint,
                    modifier = Modifier.padding(horizontal = 2.dp),
                    minScale = 0.40f,
                )
            }
        }
    }
}
