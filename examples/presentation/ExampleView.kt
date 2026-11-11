package com.company.app.feature.example.presentation.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.company.app.common.presentation.Content
import com.company.app.common.presentation.UiText
import com.company.app.feature.example.domain.model.Example
import com.company.app.feature.example.presentation.model.ExampleViewEvent
import com.company.app.feature.example.presentation.model.ExampleViewState
import com.company.app.generated.resources.Res
import com.company.app.generated.resources.retry
import com.company.app.generated.resources.save
import com.company.app.ui.preview.LightDarkPreview
import com.company.app.ui.text.asString
import com.company.app.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExampleView(
    viewState: ExampleViewState,
    eventHandler: (ExampleViewEvent) -> Unit,
    onOpenDetail: (itemId: String) -> Unit,
) {
    // UI state and the effect that syncs it with eventHandler live in View, not in Screen.
    val snackbarHostState = remember { SnackbarHostState() }
    viewState.message?.let { message ->
        val text = message.text.asString()
        LaunchedEffect(message.id) {
            snackbarHostState.showSnackbar(text)
            eventHandler(ExampleViewEvent.MessageShown)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val content = viewState.content) {
                is Content.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is Content.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = content.text.asString(),
                            style = AppTheme.typography.body,
                            color = AppTheme.colors.grey900
                        )
                        Button(onClick = { eventHandler(ExampleViewEvent.LoadData) }) {
                            Text(text = stringResource(Res.string.retry), style = AppTheme.typography.bodyBold)
                        }
                    }
                }
                is Content.Data -> {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Pure navigation click: no ViewModel; dropUnlessResumed ignores taps during transitions.
                        Text(
                            text = content.value.title,
                            style = AppTheme.typography.h5,
                            color = AppTheme.colors.grey900,
                            modifier = Modifier.clickable(onClick = dropUnlessResumed { onOpenDetail(content.value.title) })
                        )
                        // Navigation after logic: the ViewModel saves, then sets isSaved.
                        Button(onClick = { eventHandler(ExampleViewEvent.OnSaveClicked) }) {
                            Text(text = stringResource(Res.string.save), style = AppTheme.typography.bodyBold)
                        }
                    }
                }
            }
        }
    }
}

@LightDarkPreview
@Composable
private fun ExampleView_Preview() {
    AppTheme {
        ExampleView(
            viewState = ExampleViewState(content = Content.Data(Example(title = "Example Title"))),
            eventHandler = {},
            onOpenDetail = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun ExampleViewError_Preview() {
    AppTheme {
        ExampleView(
            viewState = ExampleViewState(content = Content.Error(UiText.Raw("Something went wrong"))),
            eventHandler = {},
            onOpenDetail = {},
        )
    }
}