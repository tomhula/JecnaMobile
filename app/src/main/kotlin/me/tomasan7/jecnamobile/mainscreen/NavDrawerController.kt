package me.tomasan7.jecnamobile.mainscreen

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NavDrawerController(
    private val drawerState: DrawerState,
    private val coroutineScope: CoroutineScope
) {
    fun close() = coroutineScope.launch {
        drawerState.close()
    }

    fun open() = coroutineScope.launch {
        drawerState.open()
    }
}

@Composable
fun rememberNavDrawerController(
    drawerState: DrawerState,
    coroutineScope: CoroutineScope
): NavDrawerController {
    return remember { NavDrawerController(drawerState, coroutineScope) }
}
