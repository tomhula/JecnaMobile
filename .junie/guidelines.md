# Coding guidelines

# Kotlin
- Use data class where possible
- Use `val` for properties and local variables by default. Use `var` only when mutation is strictly necessary.
- Prefer `internal` visibility for classes and functions that don't need to be exposed outside the module.

# Android
- Declare all user facing strings as string resources (strings.xml)
- Use Hilt for dependency injection.
- Prefer `@StringRes` or `@DrawableRes` annotations for parameters that expect a resource ID.

# Compose
- Any compose code repeated more than once should be extracted to its own @Composable function
- If a composable is only used in one other composable (and has no potential to be used elsewhere - usually elements of a specific screen) it should be placed in the same file with private visibility 
- Any calculations inside a composable must be inside a `remember` or similar mechanisms.
- If a composable function accepts a modifier, it should be the first optional parameter.
- Any composables that could be reused in multiple places must be placed in the `ui/component` package.
- Prefer stateless composables by hoisting state to the caller.
- For screen-level composables, obtain the ViewModel using `hiltViewModel()`.
- Use the `MaterialTheme` object instead of hardcoding colors, styles tc.

# Formatting
- Opening braces are placed on new line except for lambdas.
