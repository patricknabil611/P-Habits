package com.example.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit
import com.example.data.model.UserSettings
import com.example.ui.theme.parseHexColor
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitCell(
    habit: Habit,
    date: LocalDate,
    value: Float,
    isToday: Boolean,
    settings: UserSettings,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habitColor = parseHexColor(habit.colorHex)
    val target = if (habit.isCountType) habit.target.coerceAtLeast(1f) else 1f
    val isCompleted = value >= target
    val fraction = (value / target).coerceIn(0f, 1f)
    val hasPartial = !isCompleted && value > 0f

    val cellDimension: Dp = when (settings.cellSize.uppercase()) {
        "COMPACT" -> 34.dp
        "LARGE" -> 46.dp
        else -> 40.dp
    }

    val shape: Shape = when (settings.cellShape.uppercase()) {
        "CIRCLE" -> CircleShape
        "SQUARE" -> RectangleShape
        else -> RoundedCornerShape(8.dp) // ROUNDED_SQUARE
    }

    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(300),
        label = "cell_fraction"
    )

    val isDue = habit.isFrequencyDay(date.dayOfWeek.value)

    Box(
        modifier = modifier
            .size(cellDimension)
            .clip(shape)
            .then(
                if (isCompleted) {
                    if (settings.doneStyle == "CHECK_ICON") {
                        Modifier
                            .background(habitColor.copy(alpha = 0.2f))
                            .border(2.dp, habitColor, shape)
                    } else {
                        Modifier.background(habitColor)
                    }
                } else if (hasPartial) {
                    Modifier.background(habitColor.copy(alpha = 0.18f))
                } else {
                    when (settings.notDoneStyle.uppercase()) {
                        "EMPTY" -> Modifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), shape)
                        "X_MARK" -> Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        else -> Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)) // FADED
                    }
                }
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("habit_cell_${habit.id}_${date}"),
        contentAlignment = Alignment.Center
    ) {
        // Today outline ring if today
        if (isToday) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = shape
                    )
            )
        }

        if (habit.isCountType) {
            // Count Habit display
            if (settings.countDisplayStyle == "PROGRESS_RING") {
                // Circular progress ring inside cell
                Canvas(modifier = Modifier.fillMaxSize().size(cellDimension)) {
                    val strokeW = 3.dp.toPx()
                    val radius = (size.minDimension - strokeW) / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Track
                    drawCircle(
                        color = habitColor.copy(alpha = 0.15f),
                        radius = radius,
                        style = Stroke(strokeW)
                    )

                    // Arc
                    if (animatedFraction > 0f) {
                        drawArc(
                            color = if (isCompleted) Color.White else habitColor,
                            startAngle = -90f,
                            sweepAngle = animatedFraction * 360f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(strokeW, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // Text in count cell
            val textToDisplay = when (settings.countDisplayStyle.uppercase()) {
                "NUMBER_TARGET" -> {
                    val curStr = if (value % 1f == 0f) value.toInt().toString() else value.toString()
                    val tarStr = if (target % 1f == 0f) target.toInt().toString() else target.toString()
                    if (value > 0) "$curStr/$tarStr" else ""
                }
                else -> {
                    // NUMBER_ONLY or PROGRESS_RING
                    if (value > 0) {
                        if (value % 1f == 0f) value.toInt().toString() else value.toString()
                    } else ""
                }
            }

            if (textToDisplay.isNotEmpty()) {
                Text(
                    text = textToDisplay,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = if (textToDisplay.length > 3) 9.sp else 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isCompleted && settings.doneStyle != "CHECK_ICON") Color.White else MaterialTheme.colorScheme.onSurface
                )
            } else if (!isDue) {
                // Faint dot for non-due day
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
        } else {
            // YES_NO Habit display
            if (isCompleted) {
                if (settings.doneStyle == "CHECK_ICON") {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = habitColor,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (settings.notDoneStyle == "X_MARK" && isDue) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Not done",
                    tint = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            } else if (!isDue) {
                // Dash or dot for non-due day
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                )
            }
        }
    }
}
