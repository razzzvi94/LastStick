package com.example.laststick.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.laststick.R
import com.example.laststick.ui.board.BoardLayout
import com.example.laststick.ui.board.PlacedStick
import com.example.laststick.ui.board.Pt
import com.example.laststick.ui.board.Selection
import com.example.laststick.ui.board.StickBoard
import com.example.laststick.ui.board.SwipeSelection
import com.example.laststick.ui.theme.AmberDeep
import com.example.laststick.ui.theme.AmberLight
import com.example.laststick.ui.theme.Coral
import com.example.laststick.ui.theme.Mint
import com.example.laststick.viewmodel.StickVisual
import kotlin.math.roundToInt

/** Space kept clear at the left of each row for its number and stick count. */
private val LABEL_GUTTER = 54.dp

private val RowTints = listOf(Color(0xFF3B82F6), Color(0xFF14B8A6), Color(0xFFA78BFA))

/**
 * Draws the three rows of sticks and turns swipes into [Selection]s.
 *
 * Rendering is a pure function of [board], [visuals] and [selection]; the gesture handler only
 * reports what the user swiped via [onSelectionChange] / [onCommit]. Layout geometry comes from
 * [BoardLayout], the same function the hit-testing uses, so what you see is what you hit.
 */
@Composable
fun StickBoardView(
    board: StickBoard,
    visuals: Map<Int, StickVisual>,
    selection: Selection?,
    inputEnabled: Boolean,
    removeAnimationMs: Int,
    onSelectionChange: (Selection?) -> Unit,
    onCommit: (Selection) -> Unit,
    onAccessibleMove: (row: Int, count: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val width = size.width.toFloat()
    val height = size.height.toFloat()
    val gutter = with(LocalDensity.current) { LABEL_GUTTER.toPx() }
    val placed = remember(board, size, gutter) { BoardLayout.place(board, width, height, gutter) }
    val rowCount = board.rows.size

    val currentOnSelectionChange by rememberUpdatedState(onSelectionChange)
    val currentOnCommit by rememberUpdatedState(onCommit)

    val description = board.counts.mapIndexed { i, n ->
        stringResource(R.string.row_description, i + 1, pluralStringResource(R.plurals.sticks_count, n, n))
    }.joinToString(". ") + ". " + stringResource(R.string.board_hint)
    val actionLabels = board.counts.flatMapIndexed { row, n ->
        (1..n).map { count -> Triple(row, count, stringResource(R.string.remove_from_row_action, count, row + 1)) }
    }

    Box(
        modifier = modifier
            .onSizeChanged { size = it }
            .semantics {
                contentDescription = description
                if (inputEnabled) {
                    customActions = actionLabels.map { (row, count, label) ->
                        CustomAccessibilityAction(label) {
                            onAccessibleMove(row, count)
                            true
                        }
                    }
                }
            }
            .pointerInput(inputEnabled, placed, size) {
                if (!inputEnabled || placed.isEmpty()) return@pointerInput
                val slopX = BoardLayout.slopX(width, gutter)
                val slopY = BoardLayout.slopY(height, rowCount)
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val swipe = SwipeSelection(placed, slopX, slopY)
                    val first = swipe.begin(Pt(down.position.x, down.position.y))
                    if (first == null) {
                        // Not on a stick: ignore this gesture until the finger lifts.
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                        return@awaitEachGesture
                    }
                    var finished = false
                    try {
                        currentOnSelectionChange(first)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null) break // pointer vanished: treat as cancel
                            if (!change.pressed) {
                                finished = true
                                val result = swipe.finish()
                                currentOnSelectionChange(null)
                                if (result != null) currentOnCommit(result)
                                break
                            }
                            val updated = swipe.moveTo(Pt(change.position.x, change.position.y))
                            currentOnSelectionChange(updated)
                            change.consume()
                        }
                    } finally {
                        if (!finished) {
                            // Cancelled or interrupted gesture: drop the selection, change nothing.
                            swipe.cancel()
                            currentOnSelectionChange(null)
                        }
                    }
                }
            },
    ) {
        // Row trays.
        Canvas(Modifier.fillMaxSize()) {
            if (rowCount == 0) return@Canvas
            val rowH = this.size.height / rowCount
            val inset = 6.dp.toPx()
            repeat(rowCount) { r ->
                val tint = RowTints[r % RowTints.size]
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(tint.copy(alpha = 0.16f), tint.copy(alpha = 0.06f)),
                        startY = r * rowH,
                        endY = (r + 1) * rowH,
                    ),
                    topLeft = Offset(0f, r * rowH + inset),
                    size = Size(this.size.width, rowH - 2 * inset),
                    cornerRadius = CornerRadius(22.dp.toPx()),
                )
                drawRoundRect(
                    color = tint.copy(alpha = 0.55f),
                    topLeft = Offset(0f, r * rowH + inset),
                    size = Size(this.size.width, rowH - 2 * inset),
                    cornerRadius = CornerRadius(22.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
        }

        // Row labels (number + remaining count) so rows are told apart by more than colour.
        // Short rows get one compact line instead of two.
        val density = LocalDensity.current
        if (rowCount > 0 && height > 0f) {
            val rowH = height / rowCount
            val compact = rowH < with(density) { 96.dp.toPx() }
            board.counts.forEachIndexed { r, n ->
                val tint = RowTints[r % RowTints.size]
                if (compact) {
                    Text(
                        text = "${r + 1}  ×$n",
                        style = MaterialTheme.typography.labelLarge,
                        color = tint,
                        modifier = Modifier.offset {
                            IntOffset(with(density) { 12.dp.roundToPx() }, (r * rowH + with(density) { 8.dp.toPx() }).roundToInt())
                        },
                    )
                } else {
                    Text(
                        text = "${r + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        color = tint,
                        modifier = Modifier.offset {
                            IntOffset(with(density) { 14.dp.roundToPx() }, (r * rowH + with(density) { 12.dp.toPx() }).roundToInt())
                        },
                    )
                    Text(
                        text = "×$n",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.offset {
                            IntOffset(with(density) { 14.dp.roundToPx() }, (r * rowH + with(density) { 32.dp.toPx() }).roundToInt())
                        },
                    )
                }
            }
        }

        placed.forEach { stick ->
            key(stick.id) {
                StickItem(
                    stick = stick,
                    visual = visuals[stick.id] ?: StickVisual.NORMAL,
                    selected = selection?.ids?.contains(stick.id) == true,
                    removeAnimationMs = removeAnimationMs,
                )
            }
        }
    }
}

@Composable
private fun StickItem(
    stick: PlacedStick,
    visual: StickVisual,
    selected: Boolean,
    removeAnimationMs: Int,
) {
    val density = LocalDensity.current
    val x by animateFloatAsState(
        targetValue = stick.bounds.left,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "stickX",
    )
    val removing = visual == StickVisual.REMOVING
    val removeProgress by animateFloatAsState(
        targetValue = if (removing) 1f else 0f,
        animationSpec = tween(removeAnimationMs),
        label = "stickRemove",
    )
    val raised = selected || visual == StickVisual.AI_TARGET
    val lift by animateFloatAsState(
        targetValue = if (raised) -stick.bounds.height * 0.12f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "stickLift",
    )

    val widthDp = with(density) { stick.bounds.width.toDp() }
    val heightDp = with(density) { stick.bounds.height.toDp() }
    val shape = RoundedCornerShape(widthDp / 2)

    val (top, bottom) = when {
        selected -> Mint to Color(0xFF2BB5A0)
        visual == StickVisual.AI_TARGET -> Coral to Color(0xFFD9503F)
        else -> AmberLight to AmberDeep
    }

    Box(
        Modifier
            .offset { IntOffset(x.roundToInt(), (stick.bounds.top + lift).roundToInt()) }
            .size(widthDp, heightDp)
            .graphicsLayer {
                alpha = 1f - removeProgress
                val s = 1f - 0.55f * removeProgress
                scaleX = s
                scaleY = s
                translationY = -removeProgress * stick.bounds.height * 0.35f
                rotationZ = removeProgress * 14f
            }
            .shadow(if (raised) 10.dp else 4.dp, shape, clip = false)
            .drawBehindStick(top, bottom, selected, visual == StickVisual.AI_TARGET),
    )
}

private fun Modifier.drawBehindStick(
    top: Color,
    bottom: Color,
    selected: Boolean,
    aiTarget: Boolean,
): Modifier = this.drawBehind {
        val radius = CornerRadius(this.size.width / 2f)
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(top, bottom)),
            cornerRadius = radius,
        )
        // Wood-grain style bands.
        val band = this.size.height * 0.08f
        listOf(0.18f, 0.82f).forEach { fy ->
            drawRect(
                color = Color.Black.copy(alpha = 0.16f),
                topLeft = Offset(0f, this.size.height * fy),
                size = Size(this.size.width, band * 0.35f),
            )
        }
        // Highlight along the left edge.
        drawRoundRect(
            color = Color.White.copy(alpha = 0.28f),
            topLeft = Offset(this.size.width * 0.18f, this.size.height * 0.05f),
            size = Size(this.size.width * 0.16f, this.size.height * 0.9f),
            cornerRadius = CornerRadius(this.size.width * 0.08f),
        )
        // Non-colour cues: solid outline for the user's selection, dashed outline for the AI's target.
        if (selected) {
            drawRoundRect(
                color = Color.White,
                cornerRadius = radius,
                style = Stroke(width = 2.5.dp.toPx()),
            )
        } else if (aiTarget) {
            drawRoundRect(
                color = Color.White,
                cornerRadius = radius,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                ),
            )
        }
}
