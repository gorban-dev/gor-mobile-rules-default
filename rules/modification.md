# Safe feature modification rules

## Principle: extend, don't break

When changing an existing feature, the priority is not to break working functionality.

## Typical modification scenarios

### Adding a new field to the screen
1. Add the field to `ViewState` with a default value
2. Render the field in `View`
3. If the data comes from the server → create a UseCase → update the ViewModel
4. Update DI if a UseCase was added

### Adding a new user action
1. Add a case to `ViewEvent`
2. Handle it in `ViewModel.obtainEvent()`
3. Call `eventHandler(NewEvent)` from `View`
4. Create a UseCase to handle it if needed

### Adding navigation
1. Add a case to `ViewAction`
2. From the ViewModel: `viewAction = ViewAction.NavigateTo(...)`
3. Handle the ViewAction in the Screen or in the project's navigation layer

### Adding a new API call
1. Add a method to the `DataSource`
2. Add a method to `IRepository` (interface) and `Repository` (implementation)
3. Create a `UseCase` for the new call
4. Update the DI module
5. Inject the UseCase into the ViewModel

### Adding loading/pagination
1. Add fields to `ViewState` (isLoadingMore, currentPage, hasMore)
2. Add a `LoadMore` event to `ViewEvent`
3. Handle it in the ViewModel with the correct state

## What NOT to do when modifying
- Add logic to View or Screen
- Add remember to Screen
- Access the Repository directly from the ViewModel (only through a UseCase)
- Create god-files (merge classes into one file)
- Change the signature of an existing UseCase.execute() if it is used elsewhere
- Remove ViewState fields without checking that they are unused
