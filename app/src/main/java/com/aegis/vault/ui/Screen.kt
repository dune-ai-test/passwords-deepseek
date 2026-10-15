package com.aegis.vault.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import com.aegis.vault.data.ItemType

/** All destinations in the app. Simple sealed hierarchy — no navigation library. */
sealed interface Screen {
    data object Onboarding : Screen
    data object CreateMaster : Screen
    data object Unlock : Screen
    data object Vault : Screen
    data class Category(val mode: CategoryMode, val type: ItemType) : Screen
    data class Detail(val id: String) : Screen
    data class Edit(val id: String? = null, val presetType: ItemType? = null) : Screen
    data object Generator : Screen
    data object Security : Screen
    data object Settings : Screen
    data object Search : Screen
    data object Trash : Screen
    data object DevGallery : Screen
}

enum class CategoryMode { ALL, RECENT, WEAK, REUSED, OLD }

enum class Tab { PASSWORDS, GENERATOR, SECURITY, SETTINGS }

/** Tiny back-stack: push / pop / tab switch, plus a direction for transitions. */
class Navigator {
    val stack = mutableStateListOf<Screen>(Screen.Vault)

    var lastDirection by mutableIntStateOf(0)
        private set

    val current: Screen get() = stack.last()
    val depth: Int get() = stack.size

    fun push(screen: Screen) {
        lastDirection = 1
        stack.add(screen)
    }

    fun pop() {
        if (stack.size > 1) {
            lastDirection = -1
            stack.removeAt(stack.lastIndex)
        }
    }

    fun switchTab(screen: Screen) {
        lastDirection = 0
        stack.clear()
        stack.add(screen)
    }
}

/** Which dock tab (if any) should highlight for a screen. */
fun tabOf(screen: Screen): Tab? = when (screen) {
    Screen.Vault, is Screen.Category, is Screen.Detail -> Tab.PASSWORDS
    Screen.Generator -> Tab.GENERATOR
    Screen.Security -> Tab.SECURITY
    Screen.Settings, Screen.Trash, Screen.DevGallery -> Tab.SETTINGS
    else -> null
}

fun rootFor(tab: Tab): Screen = when (tab) {
    Tab.PASSWORDS -> Screen.Vault
    Tab.GENERATOR -> Screen.Generator
    Tab.SECURITY -> Screen.Security
    Tab.SETTINGS -> Screen.Settings
}
