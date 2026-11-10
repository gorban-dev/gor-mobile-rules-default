package com.company.app.feature.example.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.company.app.common.base.BaseSharedViewModel
import com.company.app.common.domain.invoke
import com.company.app.common.presentation.Content
import com.company.app.common.presentation.UserMessage
import com.company.app.common.presentation.toUiText
import com.company.app.core.network.onFailure
import com.company.app.core.network.onSuccess
import com.company.app.feature.example.domain.usecase.GetExampleDataUseCase
import kotlinx.coroutines.launch

class ExampleViewModel(
    private val getExampleData: GetExampleDataUseCase,
) : BaseSharedViewModel<ExampleViewState, ExampleViewAction, ExampleViewEvent>(
    initialState = ExampleViewState()
) {

    init {
        loadData()
    }

    override fun handleEvent(event: ExampleViewEvent) {
        when (event) {
            is ExampleViewEvent.LoadData -> loadData()
            is ExampleViewEvent.MessageShown -> updateState { it.copy(message = null) }
            is ExampleViewEvent.OnItemClicked -> sendAction(ExampleViewAction.NavigateToDetail(event.itemId))
        }
    }

    /** A failed first load replaces the content; a failed reload keeps the data and shows a message. */
    private fun loadData() {
        viewModelScope.launch {
            val hasData = viewState.content is Content.Data
            if (!hasData) updateState { it.copy(content = Content.Loading) }

            getExampleData()
                .onSuccess { data ->
                    updateState { it.copy(content = Content.Data(data)) }
                }
                .onFailure { error ->
                    updateState {
                        if (hasData) {
                            it.copy(message = UserMessage(error.toUiText()))
                        } else {
                            it.copy(content = Content.Error(error.toUiText()))
                        }
                    }
                }
        }
    }
}
