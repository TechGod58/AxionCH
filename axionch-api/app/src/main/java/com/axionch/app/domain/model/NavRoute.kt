package com.axionch.app.domain.model

sealed class NavRoute(val route: String) {
    data object Dashboard : NavRoute("dashboard")
    data object Accounts : NavRoute("accounts")
    data object Composer : NavRoute("composer")
    data object Results : NavRoute("results")
    data object DryRunHistory : NavRoute("dryrun_history")
    data object Vault : NavRoute("vault")
    data object VideoFilters : NavRoute("video_filters")
    data object CaptureStudio : NavRoute("capture_studio")
}
