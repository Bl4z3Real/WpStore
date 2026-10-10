package com.metrostore.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.metrostore.app.StoreViewModel
import com.metrostore.app.data.StoreApp
import kotlinx.coroutines.launch

/** App page with "details / related" pivot (reference image 4) and a bottom action bar. */
@Composable
fun DetailScreen(vm: StoreViewModel, appId: String, go: (Screen) -> Unit) {
    val app = vm.catalog.firstOrNull { it.id == appId } ?: return
    val ctx = LocalContext.current
    val accent = LocalAccent.current
    val pager = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    val actions = buildList {
        if (app.apkUrl != null || app.apkAsset != null) add(ActionButton(Glyph.Download, "install") { vm.installCatalogApp(ctx, app) })
        if (app.packageName != null) add(ActionButton(Glyph.Play, "play store") { vm.openPlay(ctx, app) })
        add(ActionButton(Glyph.Share, "share") { vm.share(ctx, app) })
    }

    MetroScaffold(bar = { _, _ -> MetroActionBar(actions) }) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(start = 16.dp, top = 20.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                listOf("details", "related").forEachIndexed { i, title ->
                    val a by animateFloatAsState(if (pager.currentPage == i) 1f else 0.4f, label = "pivot")
                    BasicText(
                        title, style = MetroText.pivotTitle,
                        modifier = Modifier.alpha(a).metroPress { scope.launch { pager.animateScrollToPage(i) } }
                    )
                }
            }
            HorizontalPager(pager, Modifier.weight(1f)) { page ->
                if (page == 0) DetailsPage(vm, app) else RelatedPage(vm, app, go)
            }
        }
    }
}

@Composable
private fun DetailsPage(vm: StoreViewModel, app: StoreApp) {
    val accent = LocalAccent.current
    val p = vm.progress[app.id]
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app, 110.dp, Modifier.metroAppear(0))
            Spacer(Modifier.width(16.dp))
            Column {
                BasicText(app.name, style = MetroText.menuItem)
                BasicText(app.publisher, style = MetroText.caption)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(app.price, style = MetroText.caption)
                    Spacer(Modifier.width(8.dp))
                    Stars(app.rating)
                }
            }
        }
        if (p != null) {
            Spacer(Modifier.height(14.dp))
            BasicText("downloading ${(p * 100).toInt()}%", style = MetroText.caption)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).background(MetroDim)) {
                Box(Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).fillMaxHeight().background(accent))
            }
        }
        Spacer(Modifier.height(22.dp))
        SectionHeader("description")
        BasicText(app.description.ifBlank { "no description." }, style = MetroText.body)
        Spacer(Modifier.height(22.dp))
        SectionHeader("information")
        InfoRow("Current version", app.version)
        InfoRow("Category", app.category)
        app.size?.let { InfoRow("Size", it) }
        InfoRow("Source", if (app.apkAsset != null) "bundled apk" else if (app.apkUrl != null) "authorised apk source" else "Google Play Store")
        InfoRow("Publisher", app.publisher.ifBlank { "-" })
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(Modifier.padding(bottom = 12.dp)) {
        BasicText(label, style = MetroText.caption)
        BasicText(value, style = MetroText.listTitle)
    }
}

@Composable
private fun RelatedPage(vm: StoreViewModel, app: StoreApp, go: (Screen) -> Unit) {
    val related = vm.catalog.filter { it.category == app.category && it.id != app.id }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        if (related.isEmpty()) BasicText("nothing related yet.", style = MetroText.body)
        related.forEachIndexed { i, a -> AppRow(a, i) { go(Screen.Detail(a.id)) } }
    }
}
