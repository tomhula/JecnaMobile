package me.tomasan7.jecnamobile.mainscreen

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface NavDrawerHandle
{
    fun close()
    fun open()
}

class NavDrawerHandleImpl(
    private val drawerState: DrawerState,
    private val coroutineScope: CoroutineScope
) : NavDrawerHandle
{
    override fun close()
    {
        coroutineScope.launch { drawerState.close() }
    }

    override fun open()
    {
        coroutineScope.launch { drawerState.open() }
    }
}

object NoOpNavDrawerHandle : NavDrawerHandle
{
    override fun close() = Unit
    override fun open() = Unit
}

@Composable
fun rememberNavDrawerHandle(
    drawerState: DrawerState,
    coroutineScope: CoroutineScope
): NavDrawerHandleImpl
{
    return remember { NavDrawerHandleImpl(drawerState, coroutineScope) }
}

val LocalNavDrawerHandle = staticCompositionLocalOf<NavDrawerHandle> { NoOpNavDrawerHandle }
