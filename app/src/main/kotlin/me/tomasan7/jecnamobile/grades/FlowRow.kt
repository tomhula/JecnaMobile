package me.tomasan7.jecnamobile.grades

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 5.dp,
    verticalSpacing: Dp = 5.dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier,
        measurePolicy = remember(horizontalSpacing, verticalSpacing) {
            object : MeasurePolicy {
                override fun MeasureScope.measure(
                    measurables: List<Measurable>,
                    constraints: Constraints
                ): MeasureResult {
                    val hSpace = horizontalSpacing.roundToPx()
                    val vSpace = verticalSpacing.roundToPx()
                    val maxWidth = constraints.maxWidth

                    val placeables = measurables.map {
                        it.measure(constraints.copy(minWidth = 0))
                    }

                    val rows = mutableListOf<MutableList<Placeable>>()
                    var currentRow = mutableListOf<Placeable>()
                    var currentWidth = 0

                    placeables.forEach { placeable ->
                        val requiredWidth = placeable.width
                        val potentialNewWidth = if (currentRow.isEmpty()) requiredWidth else currentWidth + hSpace + requiredWidth

                        if (potentialNewWidth <= maxWidth) {
                            currentWidth = potentialNewWidth
                            currentRow.add(placeable)
                        } else {
                            if (currentRow.isNotEmpty()) rows.add(currentRow)
                            currentRow = mutableListOf(placeable)
                            currentWidth = requiredWidth
                        }
                    }
                    if (currentRow.isNotEmpty()) rows.add(currentRow)

                    val rowHeights = rows.map { row -> row.maxOfOrNull { it.height } ?: 0 }
                    val height = if (rows.isEmpty()) 0 else rowHeights.sum() + vSpace * (rows.size - 1)

                    return layout(constraints.maxWidth, height) {
                        var y = 0
                        rows.forEachIndexed { index, row ->
                            var x = 0
                            val rowHeight = rowHeights[index]
                            row.forEach { placeable ->
                                placeable.placeRelative(x, y)
                                x += placeable.width + hSpace
                            }
                            y += rowHeight + if (index < rows.lastIndex) vSpace else 0
                        }
                    }
                }

                override fun IntrinsicMeasureScope.minIntrinsicHeight(
                    measurables: List<IntrinsicMeasurable>,
                    width: Int
                ): Int {
                    val hSpace = horizontalSpacing.roundToPx()
                    val vSpace = verticalSpacing.roundToPx()

                    var currentRowWidth = 0
                    var currentRowHeight = 0
                    var totalHeight = 0
                    var isFirstRow = true

                    measurables.forEach { child ->
                        val childWidth = child.maxIntrinsicWidth(Double.POSITIVE_INFINITY.toInt())
                        val childHeight = child.minIntrinsicHeight(childWidth)

                        val potentialNewWidth =
                            if (currentRowWidth == 0)
                                childWidth
                            else
                                currentRowWidth + hSpace + childWidth

                        if (potentialNewWidth <= width) {
                            // Fits in current row
                            currentRowWidth = potentialNewWidth
                            currentRowHeight = maxOf(currentRowHeight, childHeight)
                        } else {
                            // Needs new row
                            if (!isFirstRow) totalHeight += vSpace
                            totalHeight += currentRowHeight

                            // Reset for new row
                            currentRowWidth = childWidth
                            currentRowHeight = childHeight
                            isFirstRow = false
                        }
                    }
                    if (!isFirstRow) totalHeight += vSpace
                    totalHeight += currentRowHeight

                    return totalHeight
                }
            }
        }
    )
}
