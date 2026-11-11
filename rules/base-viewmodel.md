# BaseSharedViewModel — API reference

## Signature

```kotlin
abstract class BaseSharedViewModel<State : Any, Action : Any, Event : Any>(
    initialState: State,
) : ViewModel()
```

**Generics:**
- `State` — immutable screen state (data class). Holds errors and transient messages too (`Content.Error`, `UserMessage`).
- `Action` — one-off system intents for the UI (sealed interface). NOT navigation (→ ViewState field), NOT errors, NOT snackbars.
- `Event` — user intents from the UI: clicks, input, scroll, `MessageShown` (sealed interface)

The base class does not implement `KoinComponent` and does not look anything up from DI: every dependency comes through the subclass constructor.

## Public API (for Screen/View)

| Method | Returns | Purpose |
|-------|-----------|------------|
| `viewStates()` | `StateFlow<State>` | Subscribe to the screen state — the same instance on every call |
| `viewActions()` | `Flow<Action>` | Subscribe to one-off actions — the same instance on every call |
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
- `viewStates()` / `viewActions()` return flows created once per ViewModel (`asStateFlow()` / `receiveAsFlow()` stored in fields). A new instance per call restarts `collectAsStateWithLifecycle` and `CollectWithLifecycle` on every recomposition, because they key their effects on the flow.
- `onCleared()` closes the action channel; no `super.onCleared()` — `ViewModel.onCleared()` is empty

## Usage in a ViewModel

```kotlin
class {Feature}ViewModel(
    private val getData: Get{Feature}DataUseCase,
    private val repository: I{Feature}Repository,
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
            is {Feature}ViewEvent.OnSaveClicked -> save()
            is {Feature}ViewEvent.SavedHandled -> updateState { it.copy(isSaved = false) }
            is {Feature}ViewEvent.OnCallClicked -> sendAction({Feature}ViewAction.OpenDialer(event.phone))
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

    private fun save() {
        viewModelScope.launch {
            repository.save()
                .onSuccess { updateState { it.copy(isSaved = true) } }
                .onFailure { error -> updateState { it.copy(message = UserMessage(error.toUiText())) } }
        }
    }
}
```

- Results are `ApiResult<T>` — `onSuccess {}.onFailure {}`; cancellation never reaches `onFailure`.
- A failed action or reload with data on screen → `updateState { it.copy(message = UserMessage(error.toUiText())) }`.
- Navigation after logic is a state field (`isSaved = true`), reset by `SavedHandled` once Screen has navigated. Pure clicks (open detail) never reach the ViewModel. Keys and the host: `rules/architecture.md`, section 3a.
- Full error rules: `rules/error-handling.md`.

## Usage in a Screen

```kotlin
@Composable
fun {Feature}Screen(
    onBack: () -> Unit,
    onNavigateToDetail: (itemId: String) -> Unit,
    viewModel: {Feature}ViewModel = koinViewModel(),
) {
    val state by viewModel.viewStates().collectAsStateWithLifecycle()
    val context = LocalContext.current

    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            currentOnBack()
            viewModel.handleEvent({Feature}ViewEvent.SavedHandled)
        }
    }

    viewModel.viewActions().CollectWithLifecycle { action ->
        when (action) {
            is {Feature}ViewAction.OpenDialer ->
                context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${action.phone}".toUri()))
        }
    }

    {Feature}View(
        viewState = state,
        eventHandler = viewModel::handleEvent,
        onOpenDetail = onNavigateToDetail,
    )
}
```

**Important:**
- State: `collectAsStateWithLifecycle()` (NOT `collectAsState()`, NOT custom `observeAsState()`) — stops collecting automatically when the lifecycle drops below STARTED
- Actions: `CollectWithLifecycle` — project extension, collects the Flow lifecycle-aware. Only system intents arrive here; a screen without actions does not call it. Never collect actions as state (`observeAsState`, `collectAsState`): equal actions in a row get merged and lost
- The snackbar for `viewState.message` is shown in View (`LaunchedEffect(message.id)`), not in Screen

## CollectWithLifecycle extension

Project extension for lifecycle-aware collection of one-off actions. Must exist in the project (usually in `common/extensions/` or `common/ui/`). If missing — create it.

```kotlin
/**
 * Collects one-off ViewModel actions while the screen is at least [minActiveState].
 *
 * Collects on `Dispatchers.Main.immediate`: an action taken from the channel is handled in the same
 * frame and is not lost if the screen goes to background between receive and handling. [collector]
 * is read fresh through `rememberUpdatedState`, without restarting collection.
 */
@Composable
fun <T> Flow<T>.CollectWithLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    collector: (T) -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentCollector by rememberUpdatedState(collector)

    LaunchedEffect(this, lifecycle, minActiveState) {
        lifecycle.repeatOnLifecycle(minActiveState) {
            withContext(Dispatchers.Main.immediate) {
                collect { currentCollector(it) }
            }
        }
    }
}
```

- Keys `this, lifecycle, minActiveState`: with `LaunchedEffect(Unit)` a new flow or a new lifecycle is never picked up. This works only because `viewActions()` returns the same flow on every call.
- `rememberUpdatedState`: without it the collector from the first composition keeps stale lambdas (an old `context`, old parameters).
