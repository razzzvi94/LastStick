package com.example.laststick.ui.tutorial

import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.composed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.laststick.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** One tooltip in a tour. [targetId] null means a centred message without a spotlight. */
data class CoachStep(
    val targetId: String?,
    @StringRes val title: Int,
    @StringRes val body: Int,
    /** Draw an animated "finger" sweeping across the highlighted area. */
    val swipeDemo: Boolean = false,
)

/** Collects the on-screen bounds of tutorial targets. */
class CoachRegistry {
    internal val bounds = mutableStateMapOf<String, Rect>()
    internal val requesters = mutableMapOf<String, BringIntoViewRequester>()
}

@Composable
fun rememberCoachRegistry() = remember { CoachRegistry() }

/** Marks this composable as a tutorial target named [id]. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.coachTarget(registry: CoachRegistry, id: String): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    registry.requesters[id] = requester
    this
        .bringIntoViewRequester(requester)
        .onGloballyPositioned { registry.bounds[id] = it.boundsInRoot() }
}

/**
 * Dimmed overlay that spotlights the current step's target and shows a tooltip bubble with
 * Next / Skip. Taps outside the bubble are swallowed so the screen underneath stays untouched.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoachMarkOverlay(
    steps: List<CoachStep>,
    registry: CoachRegistry,
    onFinish: () -> Unit,
) {
    var index by rememberSaveable { mutableStateOf(0) }
    val step = steps[index.coerceIn(0, steps.lastIndex)]
    var host by remember { mutableStateOf(IntSize.Zero) }
    var hostOrigin by remember { mutableStateOf(Offset.Zero) }
    var bubbleHeight by remember { mutableStateOf(0) }

    // Scroll the target into view if it is partly off screen, then let its bounds update.
    LaunchedEffect(index) {
        step.targetId?.let { id ->
            delay(60)
            registry.requesters[id]?.bringIntoView()
        }
    }

    val target: Rect? = step.targetId?.let { registry.bounds[it] }?.translate(-hostOrigin)
    val density = LocalDensity.current
    val pad = with(density) { 8.dp.toPx() }
    val spot = target?.inflate(pad)
    val isLast = index == steps.lastIndex

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { host = it }
            .onGloballyPositioned { hostOrigin = it.positionInRoot() }
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        // Scrim with a transparent cut-out around the target.
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            drawRect(Color.Black.copy(alpha = 0.78f))
            if (spot != null) {
                val radius = CornerRadius(18.dp.toPx())
                drawRoundRect(
                    color = Color.Black,
                    topLeft = spot.topLeft,
                    size = spot.size,
                    cornerRadius = radius,
                    blendMode = BlendMode.Clear,
                )
                drawRoundRect(
                    color = Color(0xFFF2B84B),
                    topLeft = spot.topLeft,
                    size = spot.size,
                    cornerRadius = radius,
                    style = Stroke(2.5.dp.toPx()),
                )
            }
        }

        if (step.swipeDemo && spot != null) SwipeDemo(spot)

        // Bubble above or below the target (whichever side has more room), kept inside the safe area.
        val insetTop = WindowInsets.safeDrawing.getTop(density).toFloat()
        val insetBottom = WindowInsets.safeDrawing.getBottom(density).toFloat()
        val gap = with(density) { 16.dp.toPx() }
        val minTop = insetTop + gap / 2f
        val maxTop = (host.height - insetBottom - bubbleHeight - gap / 2f).coerceAtLeast(minTop)
        val bubbleTop = when {
            spot == null -> (host.height - bubbleHeight) / 2f
            spot.center.y < host.height / 2f -> spot.bottom + gap
            else -> spot.top - bubbleHeight - gap
        }.coerceIn(minTop, maxTop)
        val bubbleModifier = Modifier
            .align(Alignment.TopCenter)
            .offset { IntOffset(0, bubbleTop.roundToInt()) }
            .onSizeChanged { bubbleHeight = it.height }
        Column(
            bubbleModifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(22.dp))
                .padding(18.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(step.title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(step.body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.tutorial_step_counter, index + 1, steps.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isLast) {
                        TextButton(onClick = onFinish, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.tutorial_skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    TextButton(
                        onClick = { if (isLast) onFinish() else index++ },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(
                            stringResource(if (isLast) R.string.tutorial_done else R.string.tutorial_next),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

/** A looping "finger" that sweeps left to right through the spotlight to show the swipe gesture. */
@Composable
private fun SwipeDemo(spot: Rect) {
    val transition = rememberInfiniteTransition(label = "swipeDemo")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )
    Canvas(Modifier.fillMaxSize()) {
        // Bottom row of the board; the finger moves across the middle 70% and fades at both ends.
        val y = spot.top + spot.height * 0.84f
        val startX = spot.left + spot.width * 0.15f
        val endX = spot.right - spot.width * 0.15f
        val eased = (t * 1.25f).coerceAtMost(1f)
        val x = startX + (endX - startX) * eased
        val alpha = when {
            t < 0.1f -> t / 0.1f
            t > 0.85f -> (1f - t) / 0.15f
            else -> 1f
        }.coerceIn(0f, 1f)
        // Trail.
        drawLine(
            color = Color(0xFF5EEAD4).copy(alpha = 0.6f * alpha),
            start = Offset(startX, y),
            end = Offset(x, y),
            strokeWidth = 10.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawCircle(Color.White.copy(alpha = 0.95f * alpha), 20.dp.toPx(), Offset(x, y))
        drawCircle(Color(0xFF5EEAD4).copy(alpha = alpha), 20.dp.toPx(), Offset(x, y), style = Stroke(3.dp.toPx()))
    }
}
