# Safe feature modification rules

## Principle: extend, don't break

When changing an existing feature, the priority is not to break working functionality.

## Typical modification scenarios

### Adding a new field to the screen
1. Add the field to `ViewState` with a default value
2. Render the field in `View`
3. If the data comes from the server → add a UseCase if there is logic (mapping, combining sources); otherwise the ViewModel calls the Repository directly
4. Update DI if a UseCase was added

### Adding a new user action
1. Add a case to `ViewEvent`
2. Handle it in `ViewModel.handleEvent()`
3. Call `eventHandler(NewEvent)` from `View`
4. Create a UseCase to handle it if needed

### Adding navigation
1. Add a case to `ViewAction`
2. From the ViewModel: `sendAction(ViewAction.NavigateTo(...))`
3. Handle the ViewAction in the Screen or in the project's navigation layer

### Adding a new API call
1. Add a method to the `DataSource` that calls `ApiClient` and returns `ApiResult<KtorX>`
2. Add a method to `IRepository` (interface) and `Repository` (implementation), returning `ApiResult<KtorX>`
3. Add a `KtorX.toDomain()` mapper in `data/mapper/`
4. Create a `UseCase` if there is logic; otherwise call the Repository from the ViewModel
5. Update the DI module
6. Inject the UseCase (or Repository) into the ViewModel
7. Handle `Failure`: `Content.Error` if the screen has no data yet, `UserMessage` if data is already shown

### Adding loading/pagination
1. Add fields to `ViewState` (isLoadingMore, currentPage, hasMore)
2. Add a `LoadMore` event to `ViewEvent`
3. Handle it in the ViewModel with the correct state

## What NOT to do when modifying
- Add logic to View or Screen
- Add remember to Screen
- Create god-files (merge classes into one file)
- Change the signature of an existing UseCase `invoke()` if it is used elsewhere
- Remove ViewState fields without checking that they are unused
