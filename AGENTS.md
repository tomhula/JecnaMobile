# Coding guidelines

# Kotlin
- Use data class where possible
- Use `val` for properties and local variables by default. Use `var` only when mutation is strictly necessary
- Prefer `internal` visibility for classes and functions that don't need to be exposed outside the module

# Formatting
- Opening braces are placed on new line except for lambdas

# Compose
- If a composable function declaration or call has more than one parameter, the parameters must be on seperate lines. Opening parentheses on the same line as function name and closing parentheses below last parameter
- If a composable function call has more than one parameter, the prameters must be named.
- Any compose code repeated more than once should be extracted to its own @Composable function
- Declare all user facing strings as string resources (strings.xml)
- Prefer `@StringRes` or `@DrawableRes` annotations for parameters that expect a resource ID
- If a composable is only used in one other composable (and has no potential to be used elsewhere - usually elements of a specific screen) it should be placed in the same file with private visibility 
- Any calculations inside a composable must be inside a `remember` or similar mechanisms.
- If a composable function accepts a modifier, it should be the first optional parameter.
- Prefer stateless composables by hoisting state to the caller and providing callbacks.
- Use the `MaterialTheme` object instead of hardcoding colors, styles etc.

# Comments
- Don't comment any code that is self-explanatory. Only comment pieces of code that don't seem logical at first sight.

# Gradle
- Any dependencies or plugins must be declared in a Gradle version catalog (gradle/libs.versions.toml) and then referenced from the build scripts.

# Project specific
- Any composables that could be reused in multiple places must be placed in the `ui/component` package.
- For screen-level composables, obtain the ViewModel using `koinViewModel()`.
