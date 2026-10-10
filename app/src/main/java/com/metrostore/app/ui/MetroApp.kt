package com.metrostore.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metrostore.app.StoreViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

@Composable
fun MetroApp(vm: StoreViewModel) {
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    var forward by remember { mutableStateOf(true) }
    val go: (Screen) -> Unit = { forward = true; stack.add(it) }
    BackHandler(enabled = stack.size > 1) {
        forward = false
        stack.removeAt(stack.lastIndex)
    }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) { vm.refreshInstalled(); vm.refreshDownloads() }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    CompositionLocalProvider(LocalAccent provides vm.accent) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AnimatedContent(
                targetState = stack.last(),
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    if (forward) {
                        (fadeIn(tween(260, 140)) + scaleIn(tween(320, 140), 0.9f)) togetherWith
                            (fadeOut(tween(140)) + scaleOut(tween(200), 1.12f))
                    } else {
                        (fadeIn(tween(260, 100)) + scaleIn(tween(320, 100), 1.12f)) togetherWith
                            (fadeOut(tween(140)) + scaleOut(tween(200), 0.9f))
                    }
                },
                label = "navigation"
            ) { screen ->
                when (screen) {
                    Screen.Home -> HomeScreen(vm, go)
                    is Screen.Listing -> ListingScreen(vm, screen, go)
                    is Screen.Detail -> DetailScreen(vm, screen.appId, go)
                    Screen.MyApps -> MyAppsScreen(vm)
                    Screen.Downloads -> DownloadsScreen(vm)
                    Screen.Settings -> SettingsScreen(vm)
                    Screen.Search -> SearchScreen(vm, go)
                }
            }
            MetroToast(vm, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun MetroToast(vm: StoreViewModel, modifier: Modifier) {
    val msg = vm.message
    var shown by remember { mutableStateOf("") }
    LaunchedEffect(msg) {
        if (msg != null) {
            shown = msg
            delay(3800)
            if (vm.message == msg) vm.message = null
        }
    }
    Box(modifier) {
        AnimatedVisibility(
            visible = msg != null,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 92.dp),
            enter = fadeIn(tween(200)), exit = fadeOut(tween(300))
        ) {
            Box(Modifier.background(Color(0xFF1F1F1F)).border(1.dp, LocalAccent.current).padding(14.dp)) {
                BasicText(shown, style = MetroText.body.copy(color = Color.White))
            }
        }
    }
}
