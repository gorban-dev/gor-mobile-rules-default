# Android Feature Slice — Full Architecture Rules

The project is split by feature (`feature/{featureName}/`); inside a feature — by Clean Architecture layer (`presentation/`, `domain/`, `data/`, `di/`). The folder structure is strict: every file below has exactly one place.

```
feature/{featureName}/
    presentation/screen/{Feature}Screen.kt
    presentation/view/{Feature}View.kt
    presentation/viewmodel/{Feature}ViewModel.kt, {Feature}ViewState.kt, {Feature}ViewEvent.kt, {Feature}ViewAction.kt
    domain/model/{Name}.kt
    domain/usecase/{Feature}{Action}UseCase.kt
    domain/repository/I{Feature}Repository.kt
    data/datasource/{Feature}RemoteDataSource.kt, {Feature}LocalDataSource.kt
    data/model/Ktor{Name}.kt
    data/mapper/{Feature}Mapper.kt
    data/repository/{Feature}Repository.kt
    di/{Feature}DiModule.kt
```

Shared code outside features: `core/network/` (`ApiClient`, `ApiResult`, `ApiError`), `common/domain/` (`UseCase`), `common/presentation/` (`UiText`, `Content`, `UserMessage`, `toUiText()`, `AppMessageQueue`), `ui/theme/`, `ui/preview/`.

## 1. Screen (STRICT)

File: `presentation/screen/{Feature}Screen.kt`

Screen is a thin adapter between the ViewModel and the View.

**Contains:**
- Getting the ViewModel (`koinViewModel()`)
- Reading viewState via `collectAsStateWithLifecycle()`
- Subscribing to viewActions via `CollectWithLifecycle { }` (project extension)
- Passing viewState to View
- Passing eventHandler (`viewModel::handleEvent`) to View

**FORBIDDEN in Screen:**
- Business logic
- Navigation decisions (Screen only executes a `ViewAction` it received)
- State (remember)
- Computations
- Side-effects (`LaunchedEffect`, `LifecycleStartEffect`, `DisposableEffect`) — the initial load starts in the ViewModel (`init`)

## 2. View (STRICT)

File: `presentation/view/{Feature}View.kt`

View is a pure UI component.

**Contains:**
- A composable with parameters `(viewState: {Feature}ViewState, eventHandler: ({Feature}ViewEvent) -> Unit)`
- Layout based on viewState
- eventHandler calls for user actions
- UI state: `rememberLazyListState`, `rememberPagerState`, `rememberScrollState`, `SnackbarHostState`
- `LaunchedEffect` / `snapshotFlow` that sync UI state with eventHandler (e.g. show `viewState.message`, then send `MessageShown`)
- Mandatory Preview: `{Feature}View_Preview` (`private fun`, wrapped in `{App}Theme { }`, annotated `@LightDarkPreview` when the project has light and dark themes)

**FORBIDDEN in View:**
- Any business logic (if/else for business decisions)
- Direct ViewModel calls
- Navigation
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
- One-off actions via `sendAction(...)` — navigation only
- Errors → ViewState (`Content.Error`, `UserMessage`), see `rules/error-handling.md`
- Navigation via the project's navigation mechanism

**ViewModel dependencies (constructor / DI):**
- Use cases
- A Repository directly — when a UseCase would only forward the call
- Platform classes (if common, via DI)

**FORBIDDEN in ViewModel:**
- Compose imports (`androidx.compose.*`)
- UI logic
- `KoinComponent` / `get()` / `inject()`

## 4. ViewState / ViewEvent / ViewAction

**ViewState** — `presentation/viewmodel/{Feature}ViewState.kt`
```kotlin
data class {Feature}ViewState(
    val content: Content<{Model}> = Content.Loading,
    val message: UserMessage? = null,
    // other screen state
)
```

**ViewEvent** — `presentation/viewmodel/{Feature}ViewEvent.kt`
```kotlin
sealed interface {Feature}ViewEvent {
    // events from the UI (click, input, scroll, MessageShown)
}
```

**ViewAction** — `presentation/viewmodel/{Feature}ViewAction.kt`
```kotlin
sealed interface {Feature}ViewAction {
    // one-off actions: navigation, system intents. NOT errors, NOT snackbars.
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
- UseCase → `factory`; DataSource, Repository, `ApiClient`, `AppMessageQueue` → `single`; ViewModel → `viewModel`.

## 9. Code rules
- Kotlinx Serialization only (not Gson, not Moshi)
- Each class (including enum, sealed) — separate file
- `ViewEvent` / `ViewAction` — `sealed interface`
- No god-files
