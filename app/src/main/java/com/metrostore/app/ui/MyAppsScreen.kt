package com.metrostore.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrostore.app.InstalledApp
import com.metrostore.app.StoreViewModel

/**
 * Shows only the apps that are really installed on the device (store catalog apps + apps installed
 * through this store). Tap an app to open it. The round button installs an APK picked from the phone.
 */
@Composable
fun MyAppsScreen(vm: StoreViewModel) {
    val ctx = LocalContext.current
    val selected = remember { mutableStateListOf<String>() }
    LaunchedEffect(Unit) { vm.refreshInstalled() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importApk(ctx, uri)
    }

    MetroScaffold(bar = { open, setOpen ->
        MetroBottomBar(
            glyph = Glyph.Download, label = "install apk",
            onCenter = { picker.launch(arrayOf("*/*")) },
            items = listOf(
                MenuItem("uninstall selected") {
                    val items = vm.installedApps.filter { it.packageName in selected }
                    when (items.size) {
                        1 -> { vm.uninstall(ctx, items[0].packageName); selected.clear() }
                        0 -> vm.message = "select an app first"
                        else -> vm.message = "android removes one app at a time: select a single app"
                    }
                },
                MenuItem("select all") { selected.clear(); selected.addAll(vm.installedApps.map { it.packageName }) },
                MenuItem("clear selection") { selected.clear() }
            ),
            open = open, setOpen = setOpen
        )
    }) {
        Column(Modifier.fillMaxSize()) {
            PivotHeader("store", "my apps")
            if (vm.installedApps.isEmpty()) {
                BasicText(
                    "no installed apps yet. apps you install from the store appear here.",
                    style = MetroText.body, modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(vm.installedApps, key = { it.packageName }) { app ->
                        val isSel = app.packageName in selected
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(30.dp)
                                    .border(2.dp, Color.White)
                                    .background(if (isSel) Color.White else Color.Transparent)
                                    .metroPress { if (isSel) selected.remove(app.packageName) else selected.add(app.packageName) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) BasicText("✓", style = MetroText.listTitle.copy(color = Color.Black, fontFamily = SegoeSymbol, fontSize = 18.sp))
                            }
                            Spacer(Modifier.width(12.dp))
                            InstalledIcon(app)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f).metroPress { vm.openApp(ctx, app.packageName) }) {
                                BasicText(app.label, style = MetroText.listItem, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                BasicText(
                                    "version ${app.versionName.ifBlank { "-" }}",
                                    style = MetroText.caption.copy(color = Color(0xFFD0D0D0))
                                )
                                BasicText("tap to open", style = MetroText.caption)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstalledIcon(app: InstalledApp) {
    Box(Modifier.size(56.dp).background(LocalAccent.current), contentAlignment = Alignment.Center) {
        if (app.icon != null) Image(app.icon, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else BasicText(app.label.take(1).uppercase(), style = MetroText.tile)
    }
}
