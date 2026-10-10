package com.metrostore.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrostore.app.StoreViewModel
import com.metrostore.app.data.filterApps

@Composable
fun ListingScreen(vm: StoreViewModel, spec: Screen.Listing, go: (Screen) -> Unit) {
    val apps = remember(vm.catalog, spec) { filterApps(vm.catalog, spec.kind, spec.arg) }
    Column(Modifier.fillMaxSize()) {
        PivotHeader("store", spec.title.lowercase())
        if (apps.isEmpty()) {
            BasicText("nothing here yet.", style = MetroText.body, modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(apps, key = { _, a -> a.id }) { i, app ->
                    AppRow(app, i.coerceAtMost(8), Modifier.padding(horizontal = 16.dp)) {
                        go(Screen.Detail(app.id))
                    }
                }
            }
        }
    }
}
