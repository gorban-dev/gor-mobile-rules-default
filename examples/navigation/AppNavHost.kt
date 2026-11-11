package com.company.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.company.app.core.session.SessionRepository
import com.company.app.core.session.SessionState
import com.company.app.feature.example.presentation.screen.ExampleScreen
import com.company.app.feature.exampledetail.presentation.screen.ExampleDetailScreen
import com.company.app.feature.login.presentation.screen.LoginScreen
import org.koin.compose.koinInject

/**
 * Holds the back stack and the [Navigator]; only the navigator changes the stack. Screens get
 * transitions as lambdas. Events outside screens (401, logout, push) arrive here as repository state,
 * and the host rebuilds the stack.
 */
@Composable
fun AppNavHost(
    sessionRepository: SessionRepository = koinInject(),
) {
    val backStack = rememberNavBackStack(ExampleKey)
    val navigator = remember(backStack) { Navigator(backStack) }
    val session by sessionRepository.state.collectAsStateWithLifecycle()

    // Restarts after rotation: the top-of-stack check keeps the login screen from being reset.
    LaunchedEffect(session) {
        if (session == SessionState.Expired && backStack.lastOrNull() != LoginKey) {
            navigator.replaceAll(LoginKey)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = navigator::back,
        entryDecorators = listOf(
            // Keeps rememberSaveable state of entries under the top one.
            rememberSaveableStateHolderNavEntryDecorator(),
            // Own ViewModelStore per entry: without it koinViewModel() is scoped to the Activity.
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<ExampleKey> {
                ExampleScreen(
                    onBack = navigator::back,
                    onNavigateToDetail = { itemId -> navigator.navigate(ExampleDetailKey(itemId)) },
                )
            }
            // The Screen takes the key's fields; the ViewModel gets them via koinViewModel { parametersOf(...) }.
            entry<ExampleDetailKey> { key -> ExampleDetailScreen(itemId = key.itemId, onBack = navigator::back) }
            entry<LoginKey> { LoginScreen(onLoggedIn = { navigator.replaceAll(ExampleKey) }) }
        },
    )
}
