package com.company.app.navigation

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * Screen transitions. Created in `AppNavHost` next to the stack, one per Activity; not in DI and not
 * in a ViewModel. Screens get transitions as lambdas in `entry<…>`. Call from the main thread.
 */
@Stable
class Navigator(private val backStack: NavBackStack<NavKey>) {

    /** Opens a screen on top of the current one. The same key on top of the stack is not pushed again. */
    fun navigate(key: AppNavKey) {
        if (backStack.lastOrNull() != key) backStack.add(key)
    }

    /** Closes the current screen. The root screen is not closed. */
    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    /** Clears the stack and opens [key]: login → main, logout → login. */
    fun replaceAll(key: AppNavKey) {
        backStack.clear()
        backStack.add(key)
    }
}
