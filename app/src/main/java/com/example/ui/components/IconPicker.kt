package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

data class HabitIconItem(val name: String, val icon: ImageVector, val label: String)

val HabitIconsList = listOf(
    HabitIconItem("check_circle", Icons.Default.CheckCircle, "Check"),
    HabitIconItem("self_improvement", Icons.Default.SelfImprovement, "Meditation"),
    HabitIconItem("water_drop", Icons.Default.WaterDrop, "Water"),
    HabitIconItem("menu_book", Icons.Default.MenuBook, "Reading"),
    HabitIconItem("fitness_center", Icons.Default.FitnessCenter, "Workout"),
    HabitIconItem("directions_run", Icons.Default.DirectionsRun, "Running"),
    HabitIconItem("directions_walk", Icons.Default.DirectionsWalk, "Walking"),
    HabitIconItem("code", Icons.Default.Code, "Coding"),
    HabitIconItem("bed", Icons.Default.Bed, "Sleep"),
    HabitIconItem("favorite", Icons.Default.Favorite, "Health"),
    HabitIconItem("local_fire_department", Icons.Default.LocalFireDepartment, "Streak/Energy"),
    HabitIconItem("psychology", Icons.Default.Psychology, "Brain/Mind"),
    HabitIconItem("spa", Icons.Default.Spa, "Wellness"),
    HabitIconItem("brush", Icons.Default.Brush, "Art/Creativity"),
    HabitIconItem("music_note", Icons.Default.MusicNote, "Music"),
    HabitIconItem("school", Icons.Default.School, "Study"),
    HabitIconItem("work", Icons.Default.Work, "Work"),
    HabitIconItem("payments", Icons.Default.Payments, "Savings/Finance"),
    HabitIconItem("sports_esports", Icons.Default.SportsEsports, "Play"),
    HabitIconItem("task_alt", Icons.Default.TaskAlt, "Task")
)

fun getHabitIcon(iconName: String): ImageVector {
    return HabitIconsList.find { it.name == iconName }?.icon ?: Icons.Default.CheckCircle
}
