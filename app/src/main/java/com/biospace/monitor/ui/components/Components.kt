package com.biospace.monitor.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biospace.monitor.ui.theme.*
import kotlin.math.*

// ── Card container ────────────────────────────────────────────────────────────
@Composable
fun BioCard(
    modifier: Modifier = Modifier,
    glowColor: Color = CyanColor,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardColor)
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
            .padding(14.dp),
        content = content
    )
}

// ── Card title ────────────────────────────────────────────────────────────────
@Composable
fun CardTitle(text: String, suffix: String = "") {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            color = DimColor,
            fontSize = 9.sp,
            letterSpacing = 4.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal
        )
        if (suffix.isNotEmpty()) {
            Text(
                text = " $suffix",
                color = CyanColor.copy(alpha = 0.6f),
                fontSize = 9.sp,
                letterSpacing = 3.sp
            )
        }
    }
    Spacer(Modifier.height(12.dp))
}

// ── Live pulsing dot ──────────────────────────────────────────────────────────
@Composable
fun LiveDot(color: Color = CyanColor, size: Dp = 7.dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

// ── Metric row ────────────────────────────────────────────────────────────────
@Composable
fun MetricRow(label: String, value: String, valueColor: Color = TextColor) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = DimColor, fontSize = 10.sp, letterSpacing = 2.sp, fontFamily = FontFamily.SansSerif)
        Text(value, color = valueColor, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}

// ── Progress bar ─────────────────────────────────────────────────────────────
@Composable
fun BioProgressBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp
) {
    val animFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(1200), label = "bar"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF0A1222))
            .border(1.dp, BorderColor, RoundedCornerShape(3.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animFraction)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(color.copy(alpha = 0.8f), color)
                    )
                )
        )
    }
}

// ── Scale box ─────────────────────────────────────────────────────────────────
@Composable
fun ScaleBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF040910))
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(label, color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp, fontFamily = FontFamily.SansSerif)
        }
    }
}

// ── Sparkline canvas ─────────────────────────────────────────────────────────
@Composable
fun Sparkline(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    fillAlpha: Float = 0.2f,
    strokeWidth: Float = 2f,
    isBipolar: Boolean = false
) {
    if (values.size < 2) return
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minV: Double
        val maxV: Double
        val zeroY: Float

        if (isBipolar) {
            val absMax = maxOf(10.0, values.map { abs(it) }.maxOrNull() ?: 10.0)
            minV = -absMax; maxV = absMax
            zeroY = h / 2f
        } else {
            minV = values.minOrNull() ?: 0.0
            maxV = values.maxOrNull() ?: 1.0
            zeroY = h
        }

        val range = (maxV - minV).let { if (it < 0.001) 1.0 else it }
        val pts = values.mapIndexed { i, v ->
            val x = (i.toFloat() / (values.size - 1)) * w
            val y = h - ((v - minV) / range * (h - 4) - 2).toFloat()
            Offset(x, y.coerceIn(0f, h))
        }

        // Fill area
        val fillPath = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(pts.last().x, zeroY)
            lineTo(0f, zeroY)
            close()
        }
        drawPath(fillPath, color.copy(alpha = fillAlpha))

        // Line
        val linePath = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(linePath, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

        // End dot
        drawCircle(color, radius = 3f, center = pts.last(),
            style = androidx.compose.ui.graphics.drawscope.Fill)
    }
}

// ── Circular gauge ────────────────────────────────────────────────────────────
@Composable
fun CircularGauge(
    fraction: Float,
    color: Color,
    size: Dp = 90.dp,
    strokeWidth: Float = 7f,
    content: @Composable BoxScope.() -> Unit
) {
    val animFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(1500), label = "gauge"
    )
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val cx = size.value / 2f * density
            val cy = size.value / 2f * density
            val radius = (size.value / 2f - strokeWidth) * density
            // Track
            drawArc(
                color = BorderColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(strokeWidth * density, cap = StrokeCap.Round),
                topLeft = Offset(cx - radius, cy - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
            // Fill
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animFraction,
                useCenter = false,
                style = Stroke(strokeWidth * density, cap = StrokeCap.Round),
                topLeft = Offset(cx - radius, cy - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
        }
        content()
    }
}

// ── Wind param box ────────────────────────────────────────────────────────────
@Composable
fun WindParamBox(value: String, label: String, status: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFF040A18))
            .border(1.dp, BorderColor, RoundedCornerShape(7.dp))
            .padding(9.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(label, color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp, fontFamily = FontFamily.SansSerif)
            Text(status, color = color.copy(alpha = 0.8f), fontSize = 7.sp, letterSpacing = 1.sp, fontFamily = FontFamily.SansSerif)
        }
    }
}

// ── Section divider ───────────────────────────────────────────────────────────
@Composable
fun SectionDivider() {
    Canvas(modifier = Modifier.fillMaxWidth().height(1.dp).padding(vertical = 6.dp)) {
        drawLine(
            Brush.horizontalGradient(
                listOf(Color.Transparent, BorderColor, Color.Transparent)
            ),
            start = Offset(0f, 0f), end = Offset(size.width, 0f), strokeWidth = 1f
        )
    }
    Spacer(Modifier.height(6.dp))
}

// ── Small metric box ──────────────────────────────────────────────────────────
@Composable
fun SmallMetricBox(
    value: String,
    unit: String,
    label: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF040A18))
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = color, fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Text(unit, color = DimColor, fontSize = 8.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(2.dp))
        Text(label, color = DimColor, fontSize = 8.sp, letterSpacing = 1.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(2.dp))
        Text(status, color = color, fontSize = 7.sp, letterSpacing = 1.sp)
    }
}
