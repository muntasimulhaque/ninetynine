package io.github.muntasimulhaque.ninetynine.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import io.github.muntasimulhaque.ninetynine.R
import io.github.muntasimulhaque.ninetynine.ui.theme.LocalMotionScale
import io.github.muntasimulhaque.ninetynine.ui.theme.Motion
import io.github.muntasimulhaque.ninetynine.ui.theme.components.FitText
import io.github.muntasimulhaque.ninetynine.ui.theme.components.FloatingBar
import io.github.muntasimulhaque.ninetynine.ui.theme.components.barMeasure
import io.github.muntasimulhaque.ninetynine.ui.theme.components.tabLabelStyle
import kotlinx.coroutines.launch

internal data class TopLevelRoute(
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
 * carried is gone, so the top bars hold content only: Home's title renders
 * larger in the freed corner, still fitted by FitText. The labels went mixed
 * case with the two-voice register (owner decision, 1.22), which also
 * softened the measured cost the owner accepted in 1.18: at a 2.0 system
 * font scale on a 320dp phone the longest label ("Bookmarks") renders at
 * FitText scale ~0.69 (caps measured ~0.49) above the 0.40 floor and
 * never clipping; at the default scale on every phone all four labels
 * render whole.
 */
internal val topLevelRoutes = listOf(
    TopLevelRoute("names", R.string.nav_names, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    TopLevelRoute("memorize", R.string.memorize, Icons.Filled.School, Icons.Outlined.School),
    TopLevelRoute("bookmarks", R.string.bookmarks, Icons.Filled.Bookmark, Icons.Outlined.Bookmark),
    TopLevelRoute("settings", R.string.settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

/**
 * The bottom bar's vessel: the four tabs themselves ([BottomBarTabs]) inside
 * the floating, scroll-under plate: [FloatingBar]'s capsule, halo and
 * transparent gesture strip (shared with the name page's capsule, in
 * PageParts).
 */
@Composable
internal fun QuietBottomBar(
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
 * the bar: the vessel above it owns the inset, the plate and the
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
                    // flashes, nothing on a tab reaches the corners, so the
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
                                    } else if (scrollHere != null) {
                                        // Both calls are Float-typed, and that is
                                        // the point: a suspend callee's declared
                                        // result is what lands on this lambda's
                                        // resumption, and the resumption is cast
                                        // to the declared type of the call.
                                        // animateScrollTo is declared Unit but
                                        // delegates to animateScrollBy passing its
                                        // own completion straight through, so a
                                        // real animation delivers a Float to a
                                        // Unit-expecting frame, ClassCastException,
                                        // app killed (shipped 1.19: re-tapping
                                        // Memorize or Settings here crashed on
                                        // some devices). The Float-typed pair
                                        // cannot mismatch.
                                        if (motionScale == 0f) scrollHere.scrollTo(0)
                                        else scrollHere.animateScrollBy((0 - scrollHere.value).toFloat())
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
                    // stand open, Google's own bar grammar, still no
                    // pill and no motion. Greyscale screens and the
                    // ~8% who cannot trust hue get a third channel.
                    if (selected) item.icon else item.iconResting,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.height(3.dp))
                // Chrome, not reading matter: the reader's slider
                // cannot move it (9sp × the device factor only)
                // so it can never clip. The device factor DOES
                // apply: a 7-inch bar gives each tab 200dp and a
                // 10-inch 427dp, room enough for the wider labels,
                // while a phone stays at 9sp. System font scaling
                // does still apply, so the labels fit themselves
                // rather than ellipsizing to "MEM…".
                //
                // Mixed case, the chrome-you-tap voice of the two-voice
                // register (owner decision, 1.22); tracked caps stay with
                // the overlines. "Bookmarks" is the longest label the bar
                // carries, 5.178 em in Spectral SemiBold (the selected
                // weight, the wider one), so 93.2dp of ink at a system
                // font scale of 2.0, plus 8 tracking gaps × 0.5sp = 8dp,
                // ~101dp, against Memorize's ~90dp. At four tabs a
                // 320dp phone gives each one 70.0dp, so the worst case
                // is ~0.69, and the 0.40 floor is never approached. At
                // the default font scale every phone, down to 320dp,
                // renders all four labels whole. On the 7-inch class
                // the labels start 1.125× wider and the tab slots are
                // 200dp; on the 10-inch class 1.25× wider against
                // 427dp, the room grows faster than the ink on every
                // device the bar serves.
                //
                // That worst case is the measured cost the owner
                // accepted when Settings joined as the fourth tab
                // (owner decision, 1.18); see the note on
                // topLevelRoutes. Selection is carried by weight as
                // well as colour.
                // Colour alone failed WCAG 1.4.1: primary against
                // onSurfaceVariant is 1.22:1 in light and 1.20:1 in
                // dark, the two differ almost purely in hue, so a
                // deuteranope or protanope (~8% of men), a greyscale
                // screen or a high-contrast mode could not tell which
                // tab was active at all.
                FitText(
                    text = stringResource(item.labelRes),
                    style = tabLabelStyle().copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                    color = tint,
                    // The gutter is what keeps four labels four labels. FitText
                    // shrinks a label into its own slot, and at a 2.0 system
                    // font scale "Bookmarks" fills that slot edge to edge,
                    // two 2dp paddings then left the row reading "Memorize
                    // Bookmarks Settings" as one run of words. 6dp a side
                    // (12dp between neighbours) is a gutter the eye can find
                    // at any scale, and costs nothing at the default one,
                    // where every label is a third of its slot wide.
                    modifier = Modifier.padding(horizontal = 6.dp),
                    minScale = 0.40f,
                )
            }
        }
    }
}
