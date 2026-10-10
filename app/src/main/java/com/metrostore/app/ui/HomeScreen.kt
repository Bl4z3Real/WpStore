package com.metrostore.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.metrostore.app.StoreViewModel
import com.metrostore.app.data.Kind

/** Panorama home: big "store" title that scrolls slower than the columns (parallax). */
@Composable
fun HomeScreen(vm: StoreViewModel, go: (Screen) -> Unit) {
    val accent = LocalAccent.current
    MetroScaffold(bar = { open, setOpen ->
        MetroBottomBar(
            glyph = Glyph.Search, label = "search", onCenter = { go(Screen.Search) },
            items = listOf(
                MenuItem("my apps") { go(Screen.MyApps) },
                MenuItem("downloads") { go(Screen.Downloads) },
                MenuItem("settings") { go(Screen.Settings) }
            ),
            open = open, setOpen = setOpen
        )
    }) {
        val scroll = rememberScrollState()
        Box(Modifier.fillMaxSize()) {
            BasicText(
                "store", style = MetroText.pageTitle, maxLines = 1, softWrap = false,
                modifier = Modifier.padding(start = 10.dp).graphicsLayer { translationX = -scroll.value / 3f }
            )
            Row(
                Modifier.padding(top = 112.dp).horizontalScroll(scroll).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // 1) featured grid
                val featured = (vm.catalog.filter { it.isMine } + vm.catalog.filter { it.featured })
                    .distinct().ifEmpty { vm.catalog }.take(6)
                Column(Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    featured.chunked(2).forEachIndexed { r, pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEachIndexed { c, app ->
                                AppCard(app, r * 2 + c) { go(Screen.Detail(app.id)) }
                            }
                        }
                    }
                }

                // 2) quick links
                Column(Modifier.width(300.dp)) {
                    SectionHeader("quick links")
                    val tiles = listOf(
                        "Featured Apps" to Screen.Listing("featured apps", Kind.FEATURED),
                        "Top Free Apps" to Screen.Listing("top free apps", Kind.FREE),
                        "New + Rising Apps" to Screen.Listing("new + rising", Kind.NEW),
                        "Top Paid Apps" to Screen.Listing("top paid apps", Kind.PAID),
                        "All Apps" to Screen.Listing("all apps", Kind.ALL),
                        "My Apps" to Screen.MyApps
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        tiles.chunked(2).forEachIndexed { r, pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                pair.forEachIndexed { c, (label, target) ->
                                    MetroTile(Modifier.size(144.dp, 136.dp), accent, r * 2 + c, onClick = { go(target) }) {
                                        BasicText(label, style = MetroText.tile, modifier = Modifier.padding(10.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // 3) categories
                Column(Modifier.width(280.dp)) {
                    SectionHeader("categories")
                    vm.categories.forEach { cat ->
                        BasicText(
                            cat, style = MetroText.listItem,
                            modifier = Modifier.fillMaxWidth()
                                .metroPress { go(Screen.Listing(cat, Kind.CATEGORY, cat)) }
                                .padding(vertical = 6.dp)
                        )
                    }
                }

                // 4) new + rising
                Column(Modifier.width(300.dp)) {
                    SectionHeader("new + rising")
                    (vm.catalog.filter { it.isMine } + vm.catalog.filter { it.isNew }).distinct().take(5).forEachIndexed { i, app ->
                        AppRow(app, i) { go(Screen.Detail(app.id)) }
                    }
                    BasicText(
                        "all apps", style = MetroText.listItem,
                        modifier = Modifier.metroPress { go(Screen.Listing("all apps", Kind.ALL)) }.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}
