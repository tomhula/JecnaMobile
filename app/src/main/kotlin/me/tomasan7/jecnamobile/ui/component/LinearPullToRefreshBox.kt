package me.tomasan7.jecnamobile.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.PositionalThreshold
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun LinearPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
)
{
    val state = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            LinearIndicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        },
        content = content
    )
}

@Composable
private fun LinearIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.pullToRefresh(
            state = state,
            isRefreshing = isRefreshing,
            threshold = PositionalThreshold,
            onRefresh = {}
        ),
        contentAlignment = Alignment.Center
    ) {
        if (isRefreshing)
            LinearProgressIndicator(Modifier.fillMaxWidth())
        else if (state.distanceFraction != 0f)
            LinearProgressIndicator(
                drawStopIndicator = {},
                progress = { 1f },
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = easeInOutQuint(state.distanceFraction).coerceIn(0f, 1f)
                    }
            )
    }
}

// https://easings.net/#easeInOutQuint
private fun easeInOutQuint(x: Float) = if (x < 0.5f)
    16f * x * x * x * x * x
else
    1f - (-2f * x + 2f).toDouble().pow(5.0).toFloat() / 2f
