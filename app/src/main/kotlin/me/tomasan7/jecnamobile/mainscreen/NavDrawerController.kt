package me.tomasan7.jecnamobile.mainscreen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
class DrawerState(
    val anchoredDraggableState: AnchoredDraggableState<DrawerValue>,
)
{
    val offset: Float get() = if (anchoredDraggableState.offset.isNaN()) -1000f else anchoredDraggableState.offset
    val currentValue: DrawerValue get() = anchoredDraggableState.currentValue
    val isClosed: Boolean get() = currentValue == DrawerValue.Closed

    suspend fun open() = anchoredDraggableState.animateTo(DrawerValue.Open)

    suspend fun close() = anchoredDraggableState.animateTo(DrawerValue.Closed)

    fun dispatchRawDelta(delta: Float) = anchoredDraggableState.dispatchRawDelta(delta)

    suspend fun settle(velocity: Float) = anchoredDraggableState.settle(velocity)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberDrawerState(
    initialValue: DrawerValue = DrawerValue.Closed
): DrawerState
{
    val density = LocalDensity.current
    val drawerWidthPx = with(density) { 320.dp.toPx() }
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()

    return remember(drawerWidthPx, decayAnimationSpec, initialValue) {
        val anchoredDraggableState = AnchoredDraggableState(
            initialValue = initialValue,
            anchors = DraggableAnchors {
                DrawerValue.Closed at -drawerWidthPx
                DrawerValue.Open at 0f
            },
            positionalThreshold = { distance -> distance * 0.5f },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            snapAnimationSpec = SpringSpec(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            decayAnimationSpec = decayAnimationSpec
        )
        DrawerState(anchoredDraggableState)
    }
}

class NavDrawerController(
    private val drawerState: DrawerState,
    private val coroutineScope: CoroutineScope
)
{
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
): NavDrawerController
{
    return remember { NavDrawerController(drawerState, coroutineScope) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModalNavigationDrawer(
    drawerState: DrawerState,
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit
)
{
    val density = LocalDensity.current
    val drawerWidthPx = with(density) { 320.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()

    val nestedScrollConnection = remember(drawerState, drawerWidthPx) {
        object : NestedScrollConnection
        {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset
            {
                if (source == NestedScrollSource.Drag && available.x > 0 && drawerState.isClosed)
                {
                    drawerState.dispatchRawDelta(available.x)
                    return available
                }
                if (source == NestedScrollSource.Drag && available.x < 0 && !drawerState.isClosed)
                {
                    drawerState.dispatchRawDelta(available.x)
                    return available
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity
            {
                if (drawerState.offset > -drawerWidthPx && drawerState.offset < 0f)
                {
                    drawerState.settle(available.x)
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .anchoredDraggable(
                state = drawerState.anchoredDraggableState,
                orientation = Orientation.Horizontal
            )
    ) {
        content()
        val offset = drawerState.offset
        val progress = ((offset + drawerWidthPx) / drawerWidthPx).coerceIn(0f, 1f)

        if (progress > 0f)
        {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f * progress))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        coroutineScope.launch {
                            drawerState.close()
                        }
                    }
            )
        }

        if (drawerState.isClosed)
        {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(20.dp)
                    .anchoredDraggable(
                        state = drawerState.anchoredDraggableState,
                        orientation = Orientation.Horizontal
                    )
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offset.roundToInt().coerceAtMost(0), 0) }
                .anchoredDraggable(
                    state = drawerState.anchoredDraggableState,
                    orientation = Orientation.Horizontal
                )
                .fillMaxHeight()
                .width(320.dp)
        ) {
            drawerContent()
        }
    }
}
