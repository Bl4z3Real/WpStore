package com.metrostore.app.ui

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.metrostore.app.StoreViewModel
import com.metrostore.app.data.ApkInstaller

@Composable
fun DownloadsScreen(vm: StoreViewModel) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { vm.refreshDownloads() }
    Column(Modifier.fillMaxSize()) {
        PivotHeader("store", "downloads")
        if (vm.downloads.isEmpty()) {
            BasicText("no downloaded apk files.", style = MetroText.body, modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(vm.downloads, key = { _, f -> f.path }) { _, file ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        BasicText(file.name, style = MetroText.listItem)
                        BasicText(Formatter.formatFileSize(ctx, file.length()), style = MetroText.caption)
                        Row {
                            ActionText("install") { vm.install(ctx, file) }
                            ActionText("delete") { vm.deleteDownload(file) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: StoreViewModel) {
    val ctx = LocalContext.current
    var allowed by remember { mutableStateOf(ApkInstaller.canInstall(ctx)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) allowed = ApkInstaller.canInstall(ctx)
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PivotHeader("store", "settings")
        Column(Modifier.padding(horizontal = 16.dp)) {
            SectionHeader("accent color")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetroAccents.chunked(4).forEachIndexed { r, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEachIndexed { c, color ->
                            MetroTile(Modifier.size(64.dp), color, r * 4 + c, onClick = { vm.setAccent(color) }) {
                                if (vm.accent == color) {
                                    BasicText(
                                        "✓", style = MetroText.listTitle.copy(fontFamily = SegoeSymbol),
                                        modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionHeader("install apps from this source")
            BasicText(if (allowed) "allowed" else "not allowed", style = MetroText.listItem)
            BasicText(
                "Android requires this permission before an apk can be handed to the system installer.",
                style = MetroText.caption
            )
            ActionText("open android settings") {
                if (!ApkInstaller.openPermissionSettings(ctx)) vm.message = "could not open Android settings"
            }

            Spacer(Modifier.height(28.dp))
            SectionHeader("about")
            BasicText("Store 1.0", style = MetroText.listItem)
            BasicText(
                "Play Store apps open through their official links. APK files are installed with the Android system installer.",
                style = MetroText.caption
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SearchScreen(vm: StoreViewModel, go: (Screen) -> Unit) {
    var q by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val results = if (q.isBlank()) emptyList() else vm.catalog.filter {
        it.name.contains(q, true) || it.publisher.contains(q, true) || it.category.contains(q, true)
    }
    Column(Modifier.fillMaxSize()) {
        PivotHeader("store", "search")
        Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().background(Color.White).padding(12.dp)) {
            BasicTextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                textStyle = MetroText.listTitle.copy(color = Color.Black),
                cursorBrush = SolidColor(Color.Black),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
            if (q.isEmpty()) BasicText("search", style = MetroText.listTitle.copy(color = Color(0xFF888888)))
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(results, key = { _, a -> a.id }) { i, app ->
                AppRow(app, i.coerceAtMost(6), Modifier.padding(horizontal = 16.dp)) { go(Screen.Detail(app.id)) }
            }
        }
    }
}
