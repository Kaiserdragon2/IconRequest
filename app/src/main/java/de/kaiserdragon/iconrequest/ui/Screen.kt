package de.kaiserdragon.iconrequest.ui

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object IconRequest : Screen("icon_request")
    object Settings : Screen("settings")

    object HealthCheck : Screen("health_check/{packageName}") {
        fun createRoute(packageName: String) = "health_check/$packageName"
    }

    object Compare : Screen("compare/{packA}/{packB}") {
        fun createRoute(packA: String, packB: String) = "compare/$packA/$packB"
    }
}