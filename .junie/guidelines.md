# Coding guidelines

# Kotlin
- Use data class where possible

# Android
- Declare all user facing strings as string resources (strings.xml)


# Compose
- Any compose code repeated more than once should be extracted to its own @Composable function
- If a composable is only used in one other composable (and has no potential to be used elsewhere - usually elements of a specific screen) it should be placed in the same file with private visibility 
- Any calculations inside a composable must be inside a remember or similar mechanisms.
- If a composable function accepts a modifier, it should be the first optional parameter.
