package com.company.app.feature.example.presentation.screen

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.app.common.extensions.CollectWithLifecycle
import com.company.app.feature.example.presentation.model.ExampleViewAction
import com.company.app.feature.example.presentation.model.ExampleViewEvent
import com.company.app.feature.example.presentation.view.ExampleView
import com.company.app.feature.example.presentation.viewmodel.ExampleViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExampleScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (itemId: String) -> Unit,
    viewModel: ExampleViewModel = koinViewModel(),
) {
    val viewState by viewModel.viewStates().collectAsStateWithLifecycle()
    val context = LocalContext.current

    // The only effect allowed in Screen: a navigation field of the state → a navigation callback.
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(viewState.isSaved) {
        if (viewState.isSaved) {
            currentOnBack()
            viewModel.handleEvent(ExampleViewEvent.SavedHandled)
        }
    }

    viewModel.viewActions().CollectWithLifecycle { action ->
        when (action) {
            is ExampleViewAction.OpenDialer ->
                context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${action.phone}".toUri()))
        }
    }

    ExampleView(
        viewState = viewState,
        eventHandler = viewModel::handleEvent,
        onOpenDetail = onNavigateToDetail,
    )
}
