package me.tomasan7.jecnamobile.grades

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 5.dp,
    verticalSpacing: Dp = 5.dp,
    content: @Composable FlowRowScope.() -> Unit
) {
    Layout(
        content = { FlowRowScopeInstance.content() },
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
                        val placeable = it.measure(constraints.copy(minWidth = 0))
                        val align = (it.parentData as? VerticalAlignModifier)?.vertical ?: Alignment.Top
                        placeable to align
                    }

                    val rows = mutableListOf<MutableList<Pair<Placeable, Alignment.Vertical>>>()
                    var currentRow = mutableListOf<Pair<Placeable, Alignment.Vertical>>()
                    var currentWidth = 0

                    placeables.forEach { item ->
                        val placeable = item.first
                        val requiredWidth = placeable.width
                        val potentialNewWidth =
                            if (currentRow.isEmpty())
                                requiredWidth
                            else
                                currentWidth + hSpace + requiredWidth

                        if (potentialNewWidth <= maxWidth) {
                            currentWidth = potentialNewWidth
                            currentRow.add(item)
                        } else {
                            if (currentRow.isNotEmpty()) rows.add(currentRow)
                            currentRow = mutableListOf(item)
                            currentWidth = requiredWidth
                        }
                    }
                    if (currentRow.isNotEmpty()) rows.add(currentRow)

                    val rowHeights = rows.map { row -> row.maxOfOrNull { it.first.height } ?: 0 }
                    val height = if (rows.isEmpty()) 0 else rowHeights.sum() + vSpace * (rows.size - 1)

                    return layout(constraints.maxWidth, height) {
                        var y = 0
                        rows.forEachIndexed { index, row ->
                            var x = 0
                            val rowHeight = rowHeights[index]
                            row.forEach { (placeable, alignment) ->
                                val offset = alignment.align(placeable.height, rowHeight)
                                placeable.placeRelative(x, y + offset)
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

@Immutable
interface FlowRowScope {
    @Stable fun Modifier.align(alignment: Alignment.Vertical): Modifier
}

internal object FlowRowScopeInstance : FlowRowScope {
    override fun Modifier.align(alignment: Alignment.Vertical): Modifier =
        this.then(VerticalAlignModifier(alignment))
}

private data class VerticalAlignModifier(
    val vertical: Alignment.Vertical
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = this@VerticalAlignModifier
}
