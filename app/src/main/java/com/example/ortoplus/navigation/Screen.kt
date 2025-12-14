package com.example.ortoplus.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Master : Screen("master")
    object Detail : Screen("detail/{itemId}") {
        fun createRoute(itemId: String) = "detail/$itemId"
    }
}