# Android Feature Slice — Full Architecture Rules

The project is split by feature (`feature/{featureName}/`); inside a feature — by Clean Architecture layer (`presentation/`, `domain/`, `data/`, `di/`). The folder structure is strict: every file below has exactly one place.

```
feature/{featureName}/
    presentation/screen/{Feature}Screen.kt
    presentation/view/{Feature}View.kt
    presentation/viewmodel/{Feature}ViewModel.kt
    presentation/model/{Feature}ViewState.kt, {Feature}ViewEvent.kt, {Feature}ViewAction.kt
    domain/model/{Name}.kt
    domain/usecase/{Feature}{Action}UseCase.kt
    domain/repository/I{Feature}Repository.kt
    data/datasource/{Feature}RemoteDataSource.kt, {Feature}LocalDataSource.kt
    data/model/Ktor{Name}.kt
    data/mapper/{Feature}Mapper.kt
    data/repository/{Feature}Repository.kt
    di/{Feature}DiModule.kt
```

Shared code outside features: `navigation/` (`Navigator`, `{App}NavHost`, `{App}NavKey` and every screen key), `core/session/` (`SessionState`, `SessionRepository`), `core/network/` (`ApiClient`, `ApiResult`, `ApiError`), `common/domain/` (`UseCase`), `common/presentation/` (`UiText`, `Content`, `UserMessage`, `toUiText()`, `AppMessageQueue`), `ui/theme/`, `ui/preview/`.

## 1. Screen (STRICT)

File: `presentation/screen/{Feature}Screen.kt`

Screen is a thin adapter between the ViewModel and the View.

**Contains:**
- Getting the ViewModel (`koinViewModel()`; with screen arguments — `koinViewModel { parametersOf(itemId) }`)
- Screen arguments and navigation callbacks as parameters (`itemId: String`, `onBack: () -> Unit`, `onOpenDetail: (itemId: String) -> Unit`), passed by `{App}NavHost`
- Reading viewState via `collectAsStateWithLifecycle()`
- Subscribing to viewActions via `CollectWithLifecycle { }` (project extension) — system intents only; a screen without actions skips it
- The navigation effect: a `LaunchedEffect` on a navigation field of ViewState that calls a callback and sends the reset event (see section 3a)
- Passing viewState, eventHandler (`viewModel::handleEvent`) and the View's navigation callbacks to View

```kotlin
@Composable
fun {Feature}Screen(
    onBack: () -> Unit,
    onOpenDetail: (itemId: String) -> Unit,
    viewModel: {Feature}ViewModel = koinViewModel(),
) {
    val viewState by viewModel.viewStates().collectAsStateWithLifecycle()

    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(viewState.isSaved) {
        if (viewState.isSaved) {
            currentOnBack()
            viewModel.handleEvent({Feature}ViewEvent.SavedHandled)
        }
    }

    {Feature}View(
        viewState = viewState,
        eventHandler = viewModel::handleEvent,
        onOpenDetail = onOpenDetail,
    )
}
```

**FORBIDDEN in Screen:**
- Business logic
- `Navigator` or the back stack — Screen gets callbacks, the host owns the navigator
- State (remember) — except `rememberUpdatedState` for a callback used in the navigation effect
- Computations
- Side-effects (`LaunchedEffect`, `LifecycleStartEffect`, `DisposableEffect`) — the initial load starts in the ViewModel (`init`). The only exception is the navigation effect above

## 2. View (STRICT)

File: `presentation/view/{Feature}View.kt`

View is a pure UI component.

**Contains:**
- A composable with parameters `(viewState: {Feature}ViewState, eventHandler: ({Feature}ViewEvent) -> Unit)`, plus one callback per pure navigation click (`onOpenDetail: (itemId: String) -> Unit`)
- Layout based on viewState
- eventHandler calls for user actions
- Pure navigation clicks (no logic) call the navigation callback, wrapped in `dropUnlessResumed { onOpenDetail(item.id) }` (`androidx.lifecycle.compose`): a tap during the exit animation or a second tap does not navigate twice
- UI state: `rememberLazyListState`, `rememberPagerState`, `rememberScrollState`, `SnackbarHostState`
- `LaunchedEffect` / `snapshotFlow` that sync UI state with eventHandler (e.g. show `viewState.message`, then send `MessageShown`)
- Mandatory Preview: `{Feature}View_Preview` (`private fun`, wrapped in `{App}Theme { }`, annotated `@LightDarkPreview` when the project has light and dark themes)

**FORBIDDEN in View:**
- Any business logic (if/else for business decisions)
- Direct ViewModel calls
- `Navigator`, the back stack, navigation decisions — a click that needs logic first (validation, a request) goes to `eventHandler`
- One wrapper callback for all navigation (`onNavigate: (Destination) -> Unit`) — one lambda per transition
- Effects that load data or decide anything — only sync of UI state

**UI Guidelines:**
- Colors: `{App}Theme.colors.*` (NOT MaterialTheme, NOT hardcoded Color)
- Typography: `{App}Theme.typography.*` (NOT MaterialTheme, NOT hardcoded TextStyle)
- Minimal composable nesting
- A component used in 5+ places → move to `common/ui/{ComponentName}.kt`

### Preview annotation

One annotation renders both themes instead of two `@Preview` per function (Google "custom multipreview": https://developer.android.com/develop/ui/compose/tooling/previews#multipreview). Example: `examples/presentation/LightDarkPreview.kt`.

```kotlin
@Preview(name = "Light", group = "theme", showBackground = true)
@Preview(name = "Dark", group = "theme", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
annotation class LightDarkPreview
```

- If the project has no such annotation — create it in `ui/preview/LightDarkPreview.kt` before the first preview.
- `{App}Theme` must pick its palette from `isSystemInDarkTheme()`; otherwise the dark preview renders light.
- The built-in `@PreviewLightDark` (`ui-tooling-preview` 1.6+) gives the same two variants without `showBackground` — acceptable when no extra parameters are needed.
- More states of one View — more preview functions (`{Feature}ViewError_Preview`) or `@PreviewParameter`, each with the same annotation.

## 3. ViewModel (STRICT)

File: `presentation/viewmodel/{Feature}ViewModel.kt`

ViewModel is the single source of logic.

**Extends:**
```kotlin
class {Feature}ViewModel(
    // use cases / repositories via the constructor
) : BaseSharedViewModel<{Feature}ViewState, {Feature}ViewAction, {Feature}ViewEvent>(
    initialState = {Feature}ViewState()
)
```

**Contains:**
- Holding and updating the state via `updateState { it.copy(...) }`
- Event handling in `handleEvent(event)`
- Running use cases in coroutines (`viewModelScope`)
- One-off actions via `sendAction(...)` — system intents only
- Errors → ViewState (`Content.Error`, `UserMessage`), see `rules/error-handling.md`
- Navigation after its own logic → a ViewState field (`isSaved`, `createdId`) and a reset event (see section 3a)

**ViewModel dependencies (constructor / DI):**
- Use cases
- Screen arguments (`itemId: String`) — passed from the nav key via `parametersOf`, see section 3a
- A Repository directly — when a UseCase would only forward the call
- Platform classes (if common, via DI)

**FORBIDDEN in ViewModel:**
- Compose imports (`androidx.compose.*`)
- UI logic
- `KoinComponent` / `get()` / `inject()`
- `Navigator`, nav keys, navigation `ViewAction`s — the ViewModel does not know how the app navigates

## 3a. Navigation (STRICT)

Jetpack Navigation 3, by the Google recipes: navigation is UI logic. The back stack and the `Navigator` live in the host; screens get transitions as lambdas; the ViewModel only exposes state.

Sources: https://developer.android.com/guide/navigation/navigation-3/recipes/conditional, https://developer.android.com/guide/navigation/navigation-3/save-state, https://developer.android.com/topic/architecture/ui-layer/events, https://developer.android.com/develop/ui/compose/state-hoisting.

```
navigation/
    {App}NavKey.kt        // @Serializable sealed interface {App}NavKey : NavKey
    {Screen}Key.kt        // one @Serializable key per screen
    Navigator.kt          // class over NavBackStack: navigate(key), back(), replaceAll(key)
    {App}NavHost.kt       // rememberNavBackStack + remember(backStack) { Navigator(backStack) } + NavDisplay
```

**Keys:**
- A screen key is a `@Serializable` subtype of `{App}NavKey`: `data object {Screen}Key` without arguments, `data class {Screen}Key(val itemId: String)` with them. Arguments are key fields; the stack from `rememberNavBackStack` is restored after rotation and process death.
- Keys live in `navigation/`, not in the feature: a sealed interface accepts subtypes only from its own package and module. The compiler then sees every screen in one place, and the host's `entryProvider` lists them all.
- Key fields — primitives and `@Serializable` value types. Domain models and large objects are FORBIDDEN in a key: pass the id, the ViewModel loads the data.

**Navigator:**
- Created in the host: `remember(backStack) { Navigator(backStack) }`. Not a singleton, not in DI, not in a ViewModel: every Activity has its own stack and its own navigator.
- Only the navigator changes the stack. The same key on top of the stack is not pushed again (double tap). `back()` does not close the root screen. `NavDisplay(onBack = navigator::back)`.
- Login → main and logout → login use `replaceAll`.
- Call it from the main thread: it is called from composition callbacks and effects only.

**Host:**
```kotlin
@Composable
fun {App}NavHost(
    sessionRepository: SessionRepository = koinInject(),
) {
    val backStack = rememberNavBackStack(StartKey)
    val navigator = remember(backStack) { Navigator(backStack) }
    val session by sessionRepository.state.collectAsStateWithLifecycle()

    LaunchedEffect(session) {
        if (session == SessionState.Expired && backStack.lastOrNull() != LoginKey) {
            navigator.replaceAll(LoginKey)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = navigator::back,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<StartKey> { StartScreen(onOpenDetail = { id -> navigator.navigate(DetailKey(id)) }) }
            entry<DetailKey> { key -> DetailScreen(itemId = key.itemId, onBack = navigator::back) }
            entry<LoginKey> { LoginScreen(onLoggedIn = { navigator.replaceAll(StartKey) }) }
        },
    )
}
```
- `rememberViewModelStoreNavEntryDecorator()` is MANDATORY: it gives every entry its own `ViewModelStore`. Without it `koinViewModel()` is scoped to the Activity — two detail screens share one ViewModel, and a closed screen's ViewModel is never cleared.
- `rememberSaveableStateHolderNavEntryDecorator()` goes first: it keeps `rememberSaveable` state of entries under the top one.
- The session effect checks the top of the stack: `LaunchedEffect` restarts after rotation, and a blind `replaceAll(LoginKey)` would reset the login screen every time.

**Three kinds of transitions:**

| Transition | Where it lives | How |
|---|---|---|
| Pure click (open detail, back arrow) | View → Screen → host | View calls `dropUnlessResumed { onOpenDetail(id) }`; the ViewModel is not involved |
| After ViewModel logic (saved → back, logged in → main) | ViewState field | `isSaved: Boolean` / `createdId: String?`; Screen runs `LaunchedEffect(field)`, calls the callback and sends the reset event (`SavedHandled`) |
| Outside screens (401, logout, push, "data updated") | State in a repository | `StateFlow` in the repository; the host collects it and rebuilds the stack or shows a dialog |

- The reset event is mandatory. Without it a screen kept in the back stack navigates again when the user returns to it.
- 401: the network layer calls `sessionRepository.expire()` and nothing else; `SessionRepository` holds `StateFlow<SessionState>`; the host moves to `LoginKey`. See `rules/error-handling.md`, "401 / session expired".
- Push with a transition: `onNewIntent` → a key for the host (state in a repository or an intent parameter the host reads) → `navigator.navigate(key)`. "No network" and "data updated" work the same way: state, not a command.
- The data layer and screen ViewModels never navigate.

**Screen arguments → ViewModel:**
```kotlin
// ViewModel: the runtime argument is marked @InjectedParam (koin-annotations)
class DetailViewModel(
    @InjectedParam private val itemId: String,
    private val getItem: GetItemUseCase,
) : BaseSharedViewModel<DetailViewState, DetailViewAction, DetailViewEvent>(DetailViewState())

// DI
viewModel<DetailViewModel>()

// Screen
@Composable
fun DetailScreen(
    itemId: String,
    onBack: () -> Unit,
    viewModel: DetailViewModel = koinViewModel { parametersOf(itemId) },
)
```
- The Screen takes the key's fields, not the key: the feature does not depend on how the host builds the stack.
- Without `@InjectedParam` the compiler plugin looks for a `String` definition in the graph and fails the build.
- Read the argument once, in the constructor. It does not change for the lifetime of the entry.

**FORBIDDEN:**
- `Navigator` in DI, as a singleton or in a ViewModel; a navigation `ViewAction`
- Touching the back stack outside the navigator (`backStack.add(...)` in a Screen)
- Navigation from the data layer (`ApiClient`, repositories) — they change state, the host navigates
- `NavController`, string routes, passing arguments through `SavedStateHandle` by hand

## 4. ViewState / ViewEvent / ViewAction

**ViewState** — `presentation/model/{Feature}ViewState.kt`
```kotlin
data class {Feature}ViewState(
    val content: Content<{Model}> = Content.Loading,
    val message: UserMessage? = null,
    val isSaved: Boolean = false,
    // other screen state
)
```
- Navigation after ViewModel logic — a field (`isSaved: Boolean`, `createdId: String?`), reset by an event (`SavedHandled`) after Screen executes it. Without the reset a screen kept in the back stack navigates again on return.

**ViewEvent** — `presentation/model/{Feature}ViewEvent.kt`
```kotlin
sealed interface {Feature}ViewEvent {
    // events from the UI (click, input, scroll, MessageShown, SavedHandled). NOT pure navigation clicks — those are View callbacks.
}
```

**ViewAction** — `presentation/model/{Feature}ViewAction.kt`
```kotlin
sealed interface {Feature}ViewAction {
    // one-off system intents (call, map, settings). NOT navigation, NOT errors, NOT snackbars.
}
```

## 5. UseCase (STRICT)

File: `domain/usecase/{Feature}{Action}UseCase.kt`

**Implements:**
```kotlin
interface UseCase<in Params, out Result> {
    suspend operator fun invoke(params: Params): Result
}

suspend operator fun <R> UseCase<Unit, R>.invoke(): R = invoke(Unit)
```

**Rules:**
- One function `suspend operator fun invoke(params): Result`. `execute()` is not used. Call site: `getExampleData()`, `getUser(id)`.
- `Params` — a plain type for one parameter, `Unit` for none, a `{Feature}{Action}Params` class only for 2+ parameters.
- Network results — `ApiResult<T>`: pass through or `map` (`toDomain()`). Flow variants (`Flow<T>`) are allowed for streaming.
- No try/catch, no showing errors, no navigation, no UI side-effects.
- Stateless; registered as `factory`.
- If it does blocking work — moves it with `withContext(injectedDispatcher)` itself.
- Depends only on Repository (rarely — on other UseCases).
- Each UseCase — a separate class in a separate file.

## 6. Repository

Files:
- `domain/repository/I{Feature}Repository.kt` — interface
- `data/repository/{Feature}Repository.kt` — implementation

**Rules:**
- Every Repository has its interface
- Depends only on DataSources and utility classes
- Network methods return `ApiResult<Ktor{Name}>` — the data model; mapping to domain is done by the UseCase / ViewModel via `toDomain()`
- Cache / DB-only methods return plain values
- No business logic

## 7. DataSource, data models, mappers

Files: `data/datasource/{Feature}RemoteDataSource.kt`, `{Feature}LocalDataSource.kt`

**DataSource rules:**
- A plain data provider
- Remote — only through `ApiClient`, every method returns `ApiResult<T>`
- Local — SQLDelight, DataStore, FileSystem, platform APIs
- No business logic

**Data model** — `data/model/Ktor{Name}.kt`:
```kotlin
@Serializable
data class KtorExample(
    @SerialName("title")
    val title: String? = null,
)
```
- `@SerialName` on every field, nullable fields with defaults for anything the backend may omit.

**Mapper** — `data/mapper/{Feature}Mapper.kt`, top-level extensions:
```kotlin
fun KtorExample.toDomain(): Example = Example(title = title.orEmpty())
```

**Domain model** — `domain/model/{Name}.kt`, no serialization annotations.

## 8. DI Module

File: `di/{Feature}DiModule.kt`

Koin with the Koin Compiler Plugin: the dependency graph is verified at build time (missing definition, wrong qualifier, cycle → build error). Docs: https://insert-koin.io/docs/setup/compiler-plugin, agent guide: https://insert-koin.io/llms.txt, Claude Code skills: `InsertKoinIO/koin-skills` (`koin-setup`, `koin`, `di-migration`, `koin-diagnose`) — use them for DI setup, migration (incl. Kodein → Koin) and graph errors.

**Compiler Plugin DSL (default, Kotlin ≥ 2.3.20):**
```kotlin
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.factory
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel

val {feature}Module = module {
    single<{Feature}RemoteDataSource>()
    single<{Feature}Repository>() bind I{Feature}Repository::class
    factory<{Feature}{Action}UseCase>()
    viewModel<{Feature}ViewModel>()
}
```

**Gradle:**
```toml
[versions]
koin = "4.2.2"
koin-plugin = "1.2.3"

[libraries]
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-annotations = { module = "io.insert-koin:koin-annotations", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }

[plugins]
koin-compiler = { id = "io.insert-koin.compiler.plugin", version.ref = "koin-plugin" }
```
```kotlin
plugins {
    alias(libs.plugins.koin.compiler)   // in every module that declares definitions
}
koinCompiler {
    compileSafety = true
}
```
- KMP: plugin in the shared module, `koin-core` / `koin-annotations` / `koin-compose-viewmodel` in `commonMain`.
- Without the Gradle plugin `single<T>()` compiles but throws `NotImplementedError` at runtime.
- The full graph is checked at the entry point (`startKoin` / `koinApplication`); feature modules leave hints for it.

**Constructor DSL (fallback for Kotlin < 2.3.20):**
```kotlin
val {feature}Module = module {
    singleOf(::{Feature}RemoteDataSource)
    singleOf(::{Feature}Repository) bind I{Feature}Repository::class
    factoryOf(::{Feature}{Action}UseCase)
    viewModelOf(::{Feature}ViewModel)
}
```
- Import from `org.koin.core.module.dsl.*`. Do not mix with the plugin DSL — the packages have functions with the same names.

**Rules:**
- One style per project.
- Constructor injection only; `KoinComponent`, `get()`, `inject()` outside entry points (Application, Activity) are FORBIDDEN.
- Lambda definitions (`single { X(get()) }`) — only for objects built by a factory function (`HttpClient`, `ApiClient`, DB driver); the plugin does not verify their body.
- UseCase → `factory`; DataSource, Repository, `ApiClient`, `AppMessageQueue`, `SessionRepository` → `single`; `Navigator` is never in DI; ViewModel → `viewModel`.

## 9. Code rules
- Kotlinx Serialization only (not Gson, not Moshi)
- Each class (including enum, sealed) — separate file
- `ViewEvent` / `ViewAction` — `sealed interface`
- No god-files
