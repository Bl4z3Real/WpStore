package com.metrostore.app.ui

import com.metrostore.app.data.Kind

sealed interface Screen {
    data object Home : Screen
    data class Listing(val title: String, val kind: Kind, val arg: String = "") : Screen
    data class Detail(val appId: String) : Screen
    data object MyApps : Screen
    data object Downloads : Screen
    data object Settings : Screen
    data object Search : Screen
}
