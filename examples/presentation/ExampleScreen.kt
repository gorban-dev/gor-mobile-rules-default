package com.company.app.feature.example.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.app.common.extensions.CollectWithLifecycle
import com.company.app.feature.example.presentation.view.ExampleView
import com.company.app.feature.example.presentation.viewmodel.ExampleViewAction
import com.company.app.feature.example.presentation.viewmodel.ExampleViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExampleScreen(
    onNavigateToDetail: (itemId: String) -> Unit,
    viewModel: ExampleViewModel = koinViewModel(),
) {
    val viewState by viewModel.viewStates().collectAsStateWithLifecycle()

    viewModel.viewActions().CollectWithLifecycle { action ->
        when (action) {
            is ExampleViewAction.NavigateToDetail -> onNavigateToDetail(action.itemId)
        }
    }

    ExampleView(
        viewState = viewState,
        eventHandler = viewModel::handleEvent
    )
}
