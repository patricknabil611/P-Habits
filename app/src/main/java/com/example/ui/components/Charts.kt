package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.HabitScorePoint
import com.example.util.PeriodSummary
import java.time.DayOfWeek

@Composable
fun HabitStrengthGauge(
    scorePercent: Float,
    habitColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedScore = remember { Animatable(0f) }
    LaunchedEffect(scorePercent) {
        animatedScore.animateTo(scorePercent, animationSpec = tween(durationMillis = 800))
    }

    val score = animatedScore.value
    val statusLabel = when {
        score >= 85f -> "Excellent Strength"
        score >= 70f -> "Strong Habit"
        score >= 50f -> "Building Momentum"
        score >= 25f -> "Developing"
        else -> "Just Starting"
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                val strokeWidth = 12.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)

                // Track Arc (240 degrees from 150 to 390)
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.25f),
                    startAngle = 150f,
                    sweepAngle = 240f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Progress Arc
                val sweep = (score / 100f).coerceIn(0f, 1f) * 240f
                if (sweep > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(habitColor.copy(alpha = 0.7f), habitColor),
                            center = centerOffset
                        ),
                        startAngle = 150f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${score.toInt()}%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "Strength",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = statusLabel,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = habitColor
        )
    }
}

@Composable
fun HistoryBarChart(
    summaries: List<PeriodSummary>,
    habitColor: Color,
    showGridLines: Boolean = true,
    showValuesOnBars: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (summaries.isEmpty()) {
        Box(
            modifier = modifier.height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No history data in this period", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val maxVal = summaries.maxOfOrNull { it.totalTarget.coerceAtLeast(1f) } ?: 1f
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(top = 16.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barCount = summaries.size
            val barSpacing = canvasWidth / barCount
            val barWidth = (barSpacing * 0.55f).coerceIn(12f, 32f)

            if (showGridLines) {
                val lines = 3
                for (i in 0..lines) {
                    val y = canvasHeight * (i.toFloat() / lines)
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }
            }

            summaries.forEachIndexed { index, summary ->
                val centerX = index * barSpacing + barSpacing / 2
                val fraction = if (summary.totalTarget > 0) {
                    (summary.completedCount / summary.totalTarget).coerceIn(0f, 1f)
                } else 0f

                val barHeight = canvasHeight * fraction
                val topY = canvasHeight - barHeight

                // Draw background bar container
                drawRoundRect(
                    color = habitColor.copy(alpha = 0.12f),
                    topLeft = Offset(centerX - barWidth / 2, 0f),
                    size = Size(barWidth, canvasHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )

                // Draw filled bar
                if (barHeight > 0) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(habitColor, habitColor.copy(alpha = 0.75f)),
                            startY = topY,
                            endY = canvasHeight
                        ),
                        topLeft = Offset(centerX - barWidth / 2, topY),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                }
            }
        }

        // Labels row under chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            summaries.forEach { summary ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showValuesOnBars) {
                        Text(
                            text = "${summary.completedCount}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (summary.completedCount > 0) habitColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = summary.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun CountTrendLineChart(
    points: List<HabitScorePoint>,
    target: Float,
    habitColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val samplePoints = points.takeLast(30)
    val maxVal = maxOf(target * 1.25f, samplePoints.maxOfOrNull { it.value } ?: 1f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
        ) {
            val w = size.width
            val h = size.height

            // Target dashed line
            val targetY = h - (target / maxVal).coerceIn(0f, 1f) * h
            drawLine(
                color = Color.Red.copy(alpha = 0.6f),
                start = Offset(0f, targetY),
                end = Offset(w, targetY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )

            if (samplePoints.size >= 2) {
                val stepX = w / (samplePoints.size - 1)
                val strokePath = Path()
                val fillPath = Path()

                fillPath.moveTo(0f, h)

                samplePoints.forEachIndexed { i, p ->
                    val x = i * stepX
                    val y = h - (p.value / maxVal).coerceIn(0f, 1f) * h
                    if (i == 0) {
                        strokePath.moveTo(x, y)
                        fillPath.lineTo(x, y)
                    } else {
                        strokePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                fillPath.lineTo(w, h)
                fillPath.close()

                // Draw gradient fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        listOf(habitColor.copy(alpha = 0.35f), Color.Transparent),
                        startY = 0f,
                        endY = h
                    )
                )

                // Draw line stroke
                drawPath(
                    path = strokePath,
                    color = habitColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw small dots on points
                samplePoints.forEachIndexed { i, p ->
                    val x = i * stepX
                    val y = h - (p.value / maxVal).coerceIn(0f, 1f) * h
                    if (p.isCompleted) {
                        drawCircle(color = habitColor, radius = 3.5.dp.toPx(), center = Offset(x, y))
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Target line: ${target.toInt()}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Red.copy(alpha = 0.8f)
            )
            Text(
                text = "Last 30 days trend",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WeekdayFrequencyChart(
    weekdayCounts: Map<DayOfWeek, Int>,
    habitColor: Color,
    modifier: Modifier = Modifier
) {
    val days = listOf(
        DayOfWeek.MONDAY to "Mon",
        DayOfWeek.TUESDAY to "Tue",
        DayOfWeek.WEDNESDAY to "Wed",
        DayOfWeek.THURSDAY to "Thu",
        DayOfWeek.FRIDAY to "Fri",
        DayOfWeek.SATURDAY to "Sat",
        DayOfWeek.SUNDAY to "Sun"
    )

    val maxCount = (days.maxOfOrNull { weekdayCounts[it.first] ?: 0 } ?: 1).coerceAtLeast(1)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { (dow, label) ->
            val count = weekdayCounts[dow] ?: 0
            val fraction = (count.toFloat() / maxCount).coerceIn(0f, 1f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (count > 0) habitColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height((70.dp * fraction).coerceAtLeast(6.dp))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (count > 0) habitColor else MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
