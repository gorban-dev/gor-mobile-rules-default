# BaseSharedViewModel — API reference

## Signature

```kotlin
abstract class BaseSharedViewModel<State : Any, Action : Any, Event : Any>(
    initialState: State,
) : ViewModel()
```

**Generics:**
- `State` — immutable screen state (data class). Holds errors and transient messages too (`Content.Error`, `UserMessage`).
- `Action` — one-off events for the UI: navigation, system intents (sealed interface). NOT errors, NOT snackbars.
- `Event` — user intents from the UI: clicks, input, scroll, `MessageShown` (sealed interface)

The base class does not implement `KoinComponent` and does not look anything up from DI: every dependency comes through the subclass constructor.

## Public API (for Screen/View)

| Method | Returns | Purpose |
|-------|-----------|------------|
| `viewStates()` | `StateFlow<State>` | Subscribe to the screen state |
| `viewActions()` | `Flow<Action>` | Subscribe to one-off actions |
| `handleEvent(event)` | `Unit` | Send an event from the UI |

## Protected API (for ViewModel subclasses)

| Method / property | Type | Purpose |
|------------------|-----|------------|
| `viewState` | `State` (get-only) | Current state (read-only) |
| `updateState(reducer)` | `(State) -> State` | Atomic state update (`MutableStateFlow.update`) |
| `sendAction(action)` | `Action` | Send a one-off action to the UI |

## Internal implementation

- The state is kept in a `MutableStateFlow` and exposed as an immutable `StateFlow`
- Actions go through `Channel(BUFFERED)` → `receiveAsFlow()` — every action is delivered once; equal actions are not merged
- `onCleared()` closes the action channel

## Usage in a ViewModel

```kotlin
class {Feature}ViewModel(
    private val getData: Get{Feature}DataUseCase,
) : BaseSharedViewModel<{Feature}ViewState, {Feature}ViewAction, {Feature}ViewEvent>(
    initialState = {Feature}ViewState()
) {

    init {
        loadData()
    }

    override fun handleEvent(event: {Feature}ViewEvent) {
        when (event) {
            is {Feature}ViewEvent.LoadData -> loadData()
            is {Feature}ViewEvent.MessageShown -> updateState { it.copy(message = null) }
            is {Feature}ViewEvent.OnItemClicked -> sendAction({Feature}ViewAction.NavigateToDetail(event.itemId))
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            updateState { it.copy(content = Content.Loading) }

            getData()
                .onSuccess { data ->
                    updateState { it.copy(content = Content.Data(data)) }
                }
                .onFailure { error ->
                    updateState { it.copy(content = Content.Error(error.toUiText())) }
                }
        }
    }
}
```

- Results are `ApiResult<T>` — `onSuccess {}.onFailure {}`; cancellation never reaches `onFailure`.
- A failed action or reload with data on screen → `updateState { it.copy(message = UserMessage(error.toUiText())) }`.
- Full error rules: `rules/error-handling.md`.

## Usage in a Screen

```kotlin
@Composable
fun {Feature}Screen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: {Feature}ViewModel = koinViewModel(),
) {
    val state by viewModel.viewStates().collectAsStateWithLifecycle()

    viewModel.viewActions().CollectWithLifecycle { action ->
        when (action) {
            is {Feature}ViewAction.NavigateToDetail -> onNavigateToDetail(action.itemId)
        }
    }

    {Feature}View(
        viewState = state,
        eventHandler = viewModel::handleEvent
    )
}
```

**Important:**
- State: `collectAsStateWithLifecycle()` (NOT `collectAsState()`, NOT custom `observeAsState()`) — stops collecting automatically when the lifecycle drops below STARTED
- Actions: `CollectWithLifecycle` — project extension, collects the Flow lifecycle-aware. Never collect actions as state (`observeAsState`, `collectAsState`): equal actions in a row get merged and lost
- The snackbar for `viewState.message` is shown in View (`LaunchedEffect(message.id)`), not in Screen

## CollectWithLifecycle extension

Project extension for lifecycle-aware collection of one-off actions. Must exist in the project (usually in `common/extensions/` or `common/ui/`). If missing — create it.

```kotlin
@Composable
fun <T> Flow<T>.CollectWithLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    collector: (T) -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(minActiveState) {
            collect { collector(it) }
        }
    }
}
```
