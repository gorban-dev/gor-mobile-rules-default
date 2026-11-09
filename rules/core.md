# Android Architecture Core Rules

These rules ALWAYS apply when working with Android code in the project.

## Screen/View Separation
- **Screen** — thin adapter: reads viewState via `collectAsStateWithLifecycle()`, subscribes to actions via `CollectWithLifecycle {}`, passes viewState and eventHandler to View. No logic, no remember, no computations, no side-effects (including `LaunchedEffect` / `LifecycleStartEffect` that send events). No UI state (PagerState, ScrollState, LazyListState — these belong in View).
- **View** — pure UI: only layout based on viewState and calling eventHandler. No business logic. UI state (`rememberPagerState`, `rememberScrollState`, `rememberLazyListState`, `SnackbarHostState`) is allowed here. `LaunchedEffect`/`snapshotFlow` for syncing UI state with eventHandler is allowed here (e.g. show `viewState.message`, then send `MessageShown`).

## ViewModel
- Single source of logic. Extends `BaseSharedViewModel<State, Action, Event>`.
- Holds state, processes events via `handleEvent()`, executes use cases.
- State updates: `updateState { it.copy(...) }`. One-off actions: `sendAction(...)` — navigation only; errors and messages go into the state.
- May call a Repository directly when a UseCase would only forward the call. Logic, combining several sources or reuse → UseCase.
- Navigation — only within ViewModel through the project's navigation mechanism.
- Compose dependencies in ViewModel are FORBIDDEN.

## UseCase
- Separate class: `{Feature}{Action}UseCase.kt`. Implements `UseCase<Params, Result>`.
- Single function `suspend operator fun invoke(params: Params): Result`. Call site: `getExampleData()` / `getUser(id)`. `execute()` is NOT used.
- `Params` — a plain type for one parameter (`String`, `Unit`); a `{Feature}{Action}Params` class only for 2+ parameters.
- Network results are `ApiResult<T>`: pass through or `map` (e.g. `toDomain()`). No try/catch, no showing errors.
- No navigation, no UI side-effects. Stateless, registered as `factory`.
- Depends only on Repository.

## Repository
- Interface `I{Feature}Repository` + implementation `{Feature}Repository`.
- Depends only on DataSources. Network methods return `ApiResult<KtorX>` (the data model). Cache/DB-only methods return plain values.
- Combining calls: `val a = dataSource.a().getOrElse { return ApiResult.Failure(it) }`.

## DataSource
- `{Feature}LocalDataSource.kt`, `{Feature}RemoteDataSource.kt`.
- Remote DataSource talks to the network only through `ApiClient` and returns `ApiResult<T>`. `HttpClient` / `body()` directly is FORBIDDEN.
- Depends on: `ApiClient` (Ktor), SQLDelight, FileSystem, platform APIs.

## Error Handling
- Errors are values: `ApiResult<T>` = `Success(value)` | `Failure(ApiError)`; `ApiError` is a sealed interface. Classification happens once, in `ApiClient`.
- `CancellationException` is always rethrown, never turned into a failure.
- A failed load → `Content.Error(error.toUiText())` in ViewState. A failed action / reload → `UserMessage` in ViewState, cleared by the `MessageShown` event.
- `ApiError` → text only via `ApiError.toUiText()` in presentation. UseCase and Repository never show anything.
- Full rules: `rules/error-handling.md`.

## Naming Conventions
- `{Feature}Screen.kt`, `{Feature}View.kt`, `{Feature}ViewModel.kt`
- `{Feature}ViewState.kt`, `{Feature}ViewEvent.kt`, `{Feature}ViewAction.kt`
- `{Feature}{Action}UseCase.kt`, `I{Feature}Repository.kt`, `{Feature}Repository.kt`
- Data models: `Ktor{Name}.kt` (`@Serializable`, `@SerialName` on every field). Domain models: `{Name}.kt`. Mappers: `{Feature}Mapper.kt` with top-level `fun KtorX.toDomain()`.

## Package Structure
The project is split by feature; inside a feature — by Clean Architecture layer. The structure is STRICT: every file goes to its folder, no files in the feature root.
```
feature/{featureName}/
    presentation/screen/  presentation/view/  presentation/viewmodel/
    domain/model/  domain/usecase/  domain/repository/
    data/datasource/  data/model/  data/mapper/  data/repository/
    di/
```
- `ViewState` / `ViewEvent` / `ViewAction` — `presentation/viewmodel/`.
- Repository interface — `domain/repository/`, implementation — `data/repository/`.

## File Rules
- Each class (including enum, sealed) — separate file. No god-files.
- `ViewEvent` and `ViewAction` are `sealed interface` (not `sealed class`).

## DI
- Koin. New modules — Koin Compiler Plugin DSL (`single<Impl>() bind I::class`, `factory<UseCase>()`, `viewModel<VM>()`), graph verified at build time. Kotlin < 2.3.20 — constructor DSL (`singleOf`, `factoryOf`, `viewModelOf`). Don't mix the two in one project.
- Constructor injection only: `KoinComponent`, `get()`, `inject()` outside entry points are FORBIDDEN.
- Details: `rules/architecture.md` → DI Module.

## Theme and Styles
- Project theme (`{App}Theme`) — determine the name from existing code (`object *Theme` in `ui/theme/`).
- Colors: `{App}Theme.colors.*`. NOT `MaterialTheme.colorScheme`, NOT hardcoded `Color(0xFF...)`.
- Typography: `{App}Theme.typography.*`. NOT `MaterialTheme.typography`, NOT hardcoded `TextStyle(fontSize = ...)`.

## View and Preview
- Each View — in a separate file `{Feature}View.kt` with a mandatory Preview.
- Preview naming: `{Feature}View_Preview` (with underscore, `private fun`).
- Preview **always** wrapped in `{App}Theme { }`.
- Light + dark theme in the project → one multipreview annotation `@LightDarkPreview` instead of two `@Preview`. If the project has none, create it in `ui/preview/LightDarkPreview.kt`.

## Prohibited
- Compose dependencies in ViewModel
- Business logic in View
- remember / side-effects in Screen (UI state belongs in View)
- God-files (multiple classes in one file)
- `execute()` in UseCase — use `operator fun invoke`
- try/catch around network calls, `catch (e: Exception)` / `runCatching` that swallow `CancellationException`
- Navigation or showing errors in UseCase / Repository
- Errors as one-off actions — they go into ViewState
- MaterialTheme for colors and typography — only `{App}Theme`
- `KoinComponent` / service-locator lookups outside entry points
