# Error Handling — from the request to the screen

Errors are values, not exceptions. A failure is visible in every signature (`ApiResult<T>`), cannot be forgotten silently, and reaches the UI as part of the state.

Sources: Android UI events guide (https://developer.android.com/topic/architecture/ui-layer/events), Android Data layer "Expose errors" (https://developer.android.com/topic/architecture/data-layer), Kotlin coroutines best practices (https://developer.android.com/kotlin/coroutines/coroutines-best-practices), Slack EitherNet (https://github.com/slackhq/EitherNet), android/architecture-samples, Now in Android.

## 1. Network layer

Examples: `examples/network/ApiResult.kt`, `ApiError.kt`, `ApiClient.kt`.

- `ApiResult<out T>` — `sealed interface { Success(value); Failure(error: ApiError) }`. Operators: `onSuccess`, `onFailure`, `getOrNull`, `getOrElse`, `map`, `fold`. Add new ones only when a real call site needs them.
- `ApiError` — `sealed interface`:

| Subtype | Source |
|---|---|
| `Unauthorized` | 401 |
| `Banned` | project-specific blocked-account status; delete if the backend has none |
| `Http(status, serverMessage, requestId, body)` | any other non-2xx |
| `Network(cause)` | `kotlinx.io.IOException`, including every Ktor timeout |
| `NoContent` | 204 for a request that expects a typed body |
| `Parse(cause)` | `SerializationException`, `JsonConvertException`, `NoTransformationFoundException` |
| `Unknown(cause)` | anything else thrown while the request ran |

- `ApiClient` — the only way to the network. `get` / `post` / `put` / `delete` return `ApiResult<T>`. `HttpClient` is built with `expectSuccess = true`.
- `apiCall {}` — the single classification point. `CancellationException` is rethrown; everything else becomes `ApiResult.Failure`.
- Logging: every failure is logged in `apiCall`; `Parse` and `Unknown` also go to the crash reporter as non-fatals. Request/response bodies are logged by the Ktor Logging plugin, not here.
- Bugs outside requests (NPE, IllegalState in the VM) are NOT caught — the app crashes and the crash reporter gets the stack trace.

## 2. Data and domain

- DataSource: `suspend fun x(): ApiResult<KtorX> = api.get { url { path(...) } }`. A response without a body → `ApiResult<Unit>`.
- Repository returns `ApiResult<KtorX>` — the data model. Methods that only read a cache or DB return plain values.
- Combining calls in a Repository:
  ```kotlin
  val user = remote.getUser().getOrElse { return ApiResult.Failure(it) }
  val extras = remote.getExtras().getOrNull()   // best-effort part
  ```
- UseCase passes the result through or maps it: `repository.getExample().map { it.toDomain() }`. No try/catch, no showing errors, no navigation.
- Background code (services, workers) uses `getOrNull()` / `onFailure {}`; WorkManager maps `Failure` to `Result.retry()` / `Result.failure()`.

## 3. Protection from a dropped result

Recommendation, the checker is experimental (Kotlin 2.3+): https://kotlinlang.org/docs/unused-return-value-checker.html

- Compiler flag `-Xreturn-value-checker=check` in the module with the network layer.
- `@MustUseReturnValues` on `ApiClient`, DataSources, Repositories and UseCases that return `ApiResult`.
- A deliberate ignore — `val _ = repository.deletePushToken()`.
- The "Unused return value" warning on an `ApiResult` call is a real bug — fix it, don't suppress it.
- If the flag does not work on the project's Kotlin version / target — drop the flag and annotations; nothing else depends on them.

## 4. Presentation

Examples: `examples/presentation/UiText.kt`, `ApiErrorUiText.kt`, `UiTextExt.kt`, `Content.kt`, `UserMessage.kt`, `AppMessageQueue.kt`, `ExampleViewModel.kt`, `ExampleView.kt`.

### Errors live in the state
Following the Android UI events guide: a ViewModel event always ends as a state update. Errors are NOT sent as one-off actions (`sendAction(ShowError)`) — a Channel does not guarantee delivery and the error is invisible to a state assertion in tests.

| Situation | Where it goes |
|---|---|
| The screen's load failed, nothing to show | `content = Content.Error(error.toUiText())` + retry button in View |
| An action or a reload failed, data stays on screen | `message = UserMessage(error.toUiText())` → snackbar |
| A form field rejected by the backend | field error in ViewState from `(error as? ApiError.Http)?.serverMessage` |

- `Content<T>` — `Loading` / `Data(value)` / `Error(text)`.
- `UserMessage(text, id)` — kept in ViewState until View reports `MessageShown`; the ViewModel then sets `message = null`. `id` makes two equal texts two messages.
- View shows the message in `LaunchedEffect(message.id)` and then sends `MessageShown` (View may hold UI state and effects; Screen may not).

### Mapping ApiError → text
- Only in `ApiError.toUiText()` (presentation). It returns `UiText` — a resource id or a raw backend string; the UI resolves it with `asString()`.
- `Http` with `serverMessage` → `UiText.Raw(serverMessage)`; otherwise a generic server error.
- `Network` → "no connection / timeout" text, shown on the screen. A timeout can happen with a live connection, so it is never silent.
- `Unauthorized` / `Banned` → neutral text; the session layer reacts (see "401 / session expired"). The screen only resets its loading state.

### ViewModel shape
```kotlin
viewModelScope.launch {
    getExampleData()
        .onSuccess { data -> updateState { it.copy(content = Content.Data(data)) } }
        .onFailure { error -> updateState { it.copy(content = Content.Error(error.toUiText())) } }
}
```
- Cancellation never reaches `onFailure`, so no `ensureActive()` is needed.
- "Navigate on any outcome" — in both `onSuccess` and `onFailure`, not in `finally` (it would fire when the screen closes).
- Fan-out: `async { repo.a() }` + `async { repo.b() }` — each returns its own `ApiResult`, one failure does not cancel the other.
- `getOrElse { return@launch }` — only when the value feeds the next request.

### 401 / session expired
- Token refresh is the Ktor Auth plugin's job. A 401 that still reaches `ApiClient` means the session is over.
- `ApiClient` on 401 calls `sessionRepository.expire()` and returns `ApiResult.Failure(ApiError.Unauthorized)` as usual. It does not navigate and does not show anything.
- `SessionRepository` (`core/session/`, `single`) holds `StateFlow<SessionState>`: `Active` / `Expired`. Login calls `start()`, logout and 401 call `expire()`.
- The screen handles `Unauthorized` like any other failure (neutral text or nothing). The transition to login is made by `{App}NavHost` on `SessionState.Expired`, see `rules/architecture.md`, section 3a.
- Handling 401 with a transition in every screen is FORBIDDEN.

### App level
- Offline banner — once at the app root from a `NetworkMonitor` (OS connectivity), not from `ApiError.Network` of single requests (Now in Android).
- `AppMessageQueue` — app-scoped DI single for messages that must outlive the screen (a background save failed after navigation). The ViewModel calls it explicitly; the root renders `messages` and calls `messageShown(id)`. A global `object`, an event bus, or an automatic "show banner on every failure" in the base ViewModel is FORBIDDEN.

## Prohibited
- `HttpClient` / `body()` in DataSources — only `ApiClient`
- try/catch around requests, `catch (e: Exception)` / `runCatching` in suspend code (they swallow `CancellationException`)
- `kotlin.Result` for network results
- Showing errors from UseCase / Repository
- Errors as `ViewAction`
- Building user-facing text outside `toUiText()`
