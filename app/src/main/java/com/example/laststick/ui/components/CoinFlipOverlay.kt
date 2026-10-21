package com.example.laststick.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.laststick.R
import com.example.laststick.viewmodel.Phase
import kotlin.math.PI
import kotlin.random.Random
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val GoldLight = Color(0xFFFFE9A8)
private val GoldMid = Color(0xFFF2B84B)
private val GoldDark = Color(0xFFB87A14)
private val GoldDeep = Color(0xFF8A560C)
private val GoldInk = Color(0xFF6B4509)

/** Each flip makes a random number of full turns in this range (inclusive). */
private const val MIN_SPINS = 3
private const val MAX_SPINS = 8
private const val EDGE_SLICES = 18

/**
 * Full-screen coin toss. READY waits for a tap (coin hovers, "Tap to flip" pulses); FLIP tosses the
 * coin up with several spins and a slight wobble; RESULT lets it land with a small bounce and
 * announces who starts. Angle 0° shows "YOU" (heads), 180° shows "AI" (tails).
 */
@Composable
fun CoinFlipOverlay(
    phase: Phase,
    humanStarts: Boolean,
    spinMs: Int,
    onTap: () -> Unit,
) {
    val ready = phase == Phase.COIN_READY
    val progress = remember { Animatable(0f) }
    // Rolled again each time the coin is (re)armed, i.e. fresh for every flip, then fixed while it spins.
    val spins = remember(ready) { Random.nextInt(MIN_SPINS, MAX_SPINS + 1) }
    val bounce = remember { Animatable(0f) }
    LaunchedEffect(phase) {
        when (phase) {
            Phase.COIN_FLIP -> progress.animateTo(1f, tween(spinMs, easing = LinearEasing))
            Phase.COIN_RESULT -> {
                progress.snapTo(1f)
                // 1 -> 0 drives a short settle: decaying wobble of the flat coin plus a tiny hop.
                bounce.snapTo(1f)
                bounce.animateTo(0f, tween(650, easing = LinearEasing))
            }
            else -> {
                progress.snapTo(0f)
                bounce.snapTo(0f)
            }
        }
    }

    val idle = rememberInfiniteTransition(label = "coinIdle")
    val hover by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "hover",
    )
    val pulse by idle.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )

    val p = progress.value
    val settle = bounce.value
    // Landing: the coin rocks around its resting angle with fading amplitude and hops a few dp.
    val wobble = 16f * settle * settle * sin((1f - settle) * 5f * PI.toFloat())
    val hopDp = 9f * settle * abs(sin((1f - settle) * 3f * PI.toFloat()))
    val totalAngle = 360f * spins + if (humanStarts) 0f else 180f
    // The spin decelerates like a real coin: fast at launch, slowing as it nears the table.
    val spinEase = CubicBezierEasing(0.2f, 0.6f, 0.3f, 1f).transform(p)
    val angle = if (ready) 0f else spinEase * totalAngle + wobble
    // Toss height: rises quickly, hangs, then falls (0 at both ends, 1 at the apex).
    val rise = if (ready) 0f else sin(PI.toFloat() * p)
    val airborne = rise
    // Loose wobble of the coin's axis while airborne, calming as it lands.
    val tilt = if (ready) 0f else sin(p * 2f * PI.toFloat() * 2.5f) * 9f * (1f - p)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        // Shrink the whole coin stage on short screens (landscape, split-screen) so nothing is clipped.
        val k = ((maxHeight - 150.dp) / 360.dp).coerceIn(0.4f, 1f)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            // Fixed-size stage so the text below never moves while the coin flies.
            Box(Modifier.size(width = 260.dp * k, height = 360.dp * k), contentAlignment = Alignment.BottomCenter) {
                // Soft shadow on the table: shrinks and fades the higher the coin is.
                Canvas(
                    Modifier
                        .size(width = 150.dp * k, height = 26.dp * k)
                        .graphicsLayer {
                            val s = 1f - 0.5f * airborne.coerceAtMost(1f)
                            scaleX = s
                            scaleY = s
                            alpha = (0.6f - 0.4f * airborne).coerceAtLeast(0.1f)
                        },
                ) {
                    drawOval(brush = Brush.radialGradient(listOf(Color.Black, Color.Transparent)), size = size)
                }
                Box(
                    Modifier
                        .size(width = 190.dp * k, height = 190.dp * k)
                        .graphicsLayer {
                            translationY = k * (-(airborne * 230.dp.toPx()) - hover * (if (ready) 9.dp.toPx() else 0f) - hopDp.dp.toPx() - 46.dp.toPx())
                            val s = 1f + 0.25f * airborne.coerceAtMost(1f)
                            scaleX = s
                            scaleY = s
                            rotationZ = tilt
                        }
                        .clickable(
                            enabled = ready,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = stringResource(R.string.coin_click_label),
                            role = Role.Button,
                            onClick = onTap,
                        ),
                ) {
                    Coin(angle = angle, frontText = stringResource(R.string.coin_face_you), backText = stringResource(R.string.coin_face_ai))
                }
            }

            Text(
                text = stringResource(
                    when (phase) {
                        Phase.COIN_READY -> R.string.coin_tap_to_flip
                        Phase.COIN_FLIP -> R.string.coin_flipping
                        else -> if (humanStarts) R.string.you_start else R.string.ai_starts
                    },
                ),
                style = MaterialTheme.typography.headlineSmall,
                color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(if (ready) pulse else 1f),
            )
            Text(
                text = if (ready) stringResource(R.string.coin_hint) else " ",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.height(24.dp),
            )
        }
    }
}

/**
 * A coin with real thickness, seen from the front and rotated by [angle] around its horizontal axis.
 *
 * Geometry: the visible face is an ellipse squashed by |cos angle|; the rim is a stack of ellipses
 * between the back and front faces, shifted by ∓(thickness/2)·sin angle, so the edge widens as the
 * coin turns edge-on and the face never just "pops" from one side to the other.
 */
@Composable
private fun Coin(angle: Float, frontText: String, backText: String) {
    val measurer = rememberTextMeasurer()
    Canvas(Modifier.fillMaxSize()) {
        val radius = size.width / 2f * 0.9f
        val thickness = radius * 0.2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val theta = angle * PI.toFloat() / 180f
        val c = cos(theta)
        val s = sin(theta)
        val squash = abs(c)
        val frontFacing = c >= 0f

        // Rim: slices from the far face to the near face.
        for (i in 0..EDGE_SLICES) {
            val z = thickness / 2f - thickness * i / EDGE_SLICES // +t/2 (front) … −t/2 (back)
            val y = center.y - z * s
            val shade = i / EDGE_SLICES.toFloat()
            val tone = androidx.compose.ui.graphics.lerp(GoldMid, GoldDeep, if (frontFacing) shade else 1f - shade)
            val rimHeight = (2f * radius * squash).coerceAtLeast(1f)
            drawOval(
                brush = Brush.horizontalGradient(
                    listOf(GoldDeep, tone, GoldLight.copy(alpha = 0.9f), tone, GoldDeep),
                    startX = center.x - radius,
                    endX = center.x + radius,
                ),
                topLeft = Offset(center.x - radius, y - rimHeight / 2f),
                size = Size(2f * radius, rimHeight),
            )
        }

        // Visible face on top.
        val zFace = if (frontFacing) thickness / 2f else -thickness / 2f
        val faceCenter = Offset(center.x, center.y - zFace * s)
        withTransform({ scale(1f, squash.coerceAtLeast(0.001f), pivot = faceCenter) }) {
            drawFace(faceCenter, radius, frontFacing, measurer, if (frontFacing) frontText else backText)
        }
    }
}

private fun DrawScope.drawFace(c: Offset, r: Float, front: Boolean, measurer: TextMeasurer, text: String) {
    // Body.
    drawCircle(
        brush = Brush.linearGradient(
            listOf(GoldLight, GoldMid, GoldDark),
            start = Offset(c.x - r, c.y - r),
            end = Offset(c.x + r, c.y + r),
        ),
        radius = r,
        center = c,
    )
    // Milled edge ticks.
    val ticks = 80
    for (i in 0 until ticks) {
        val a = 2.0 * PI * i / ticks
        drawLine(
            color = GoldInk.copy(alpha = 0.5f),
            start = Offset(c.x + r * 0.93f * cos(a).toFloat(), c.y + r * 0.93f * sin(a).toFloat()),
            end = Offset(c.x + r * 0.995f * cos(a).toFloat(), c.y + r * 0.995f * sin(a).toFloat()),
            strokeWidth = 1.5.dp.toPx(),
        )
    }
    drawCircle(GoldDark, r * 0.93f, c, style = Stroke(2.dp.toPx()))
    drawCircle(GoldLight.copy(alpha = 0.9f), r * 0.90f, c, style = Stroke(1.5.dp.toPx()))
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.Transparent, GoldDark.copy(alpha = 0.35f)),
            center = c,
            radius = r * 0.86f,
        ),
        radius = r * 0.86f,
        center = c,
    )
    drawCircle(GoldInk.copy(alpha = 0.55f), r * 0.74f, c, style = Stroke(1.5.dp.toPx()))
    // Dotted ring between rim and emblem.
    for (i in 0 until 36) {
        val a = 2.0 * PI * i / 36
        drawCircle(
            GoldInk.copy(alpha = 0.45f),
            radius = 1.6.dp.toPx(),
            center = Offset(c.x + r * 0.8f * cos(a).toFloat(), c.y + r * 0.8f * sin(a).toFloat()),
        )
    }

    if (front) drawStar(c.copy(y = c.y - r * 0.26f), r * 0.34f) else drawChip(c.copy(y = c.y - r * 0.26f), r * 0.34f)

    val layout = measurer.measure(
        text,
        TextStyle(
            color = GoldInk,
            fontSize = (r * 0.3f / density * 1f).sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
        ),
    )
    drawText(layout, topLeft = Offset(c.x - layout.size.width / 2f, c.y + r * 0.12f))

    // Specular highlight.
    drawArc(
        brush = Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.8f), Color.Transparent),
            start = Offset(c.x - r, c.y - r),
            end = c,
        ),
        startAngle = 200f,
        sweepAngle = 75f,
        useCenter = false,
        topLeft = Offset(c.x - r * 0.84f, c.y - r * 0.84f),
        size = Size(r * 1.68f, r * 1.68f),
        style = Stroke(5.dp.toPx(), cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawStar(center: Offset, radius: Float) {
    val path = Path()
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) radius else radius * 0.42f
        val a = -PI / 2 + i * PI / 5
        val x = center.x + rad * cos(a).toFloat()
        val y = center.y + rad * sin(a).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, GoldInk.copy(alpha = 0.88f))
}

/** Tails emblem: a small chip with pins, nodding to "AI". */
private fun DrawScope.drawChip(center: Offset, radius: Float) {
    val body = radius * 1.15f
    val top = center.y - body / 2f
    val ink = GoldInk.copy(alpha = 0.88f)
    drawRect(ink, Offset(center.x - body / 2f, top), Size(body, body), style = Stroke(3.dp.toPx()))
    drawRect(ink, Offset(center.x - body / 4f, top + body / 4f), Size(body / 2f, body / 2f))
    for (i in 0 until 3) {
        val x = center.x - body / 2f + body * (i + 0.5f) / 3f
        drawLine(ink, Offset(x, top - 7.dp.toPx()), Offset(x, top), 2.5.dp.toPx())
        drawLine(ink, Offset(x, top + body), Offset(x, top + body + 7.dp.toPx()), 2.5.dp.toPx())
    }
}
