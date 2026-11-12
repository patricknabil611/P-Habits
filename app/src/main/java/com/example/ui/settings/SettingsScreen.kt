package com.example.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit
import com.example.data.model.HabitEntry
import com.example.data.model.HabitType
import com.example.data.model.UserSettings
import com.example.sync.BackupItem
import com.example.sync.SyncStatus
import com.example.ui.home.HabitCell
import com.example.ui.theme.DefaultHabitPalette
import com.example.ui.theme.parseHexColor
import com.example.util.BackupExportUtil
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    habits: List<Habit>,
    entries: List<HabitEntry>,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    isLoadingAuth: Boolean,
    syncStatus: SyncStatus = SyncStatus.DISABLED,
    lastSyncTime: Long = 0L,
    syncErrorMessage: String? = null,
    onTriggerSync: () -> Unit = {},
    onBackupNow: () -> Unit = {},
    onListBackups: ((List<BackupItem>) -> Unit) -> Unit = {},
    onRestoreCloudBackup: (BackupItem) -> Unit = {},
    onSignInWithGoogle: () -> Unit,
    onSignOutWithChoice: (removeLocalData: Boolean) -> Unit,
    onUpdateSettings: (UserSettings) -> Unit,
    onReloadSampleData: () -> Unit,
    onClearAllData: () -> Unit,
    onRestoreBackup: (List<Habit>, List<HabitEntry>) -> Unit,
    isBackingUp: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showSignOutConfirmDialog by remember { mutableStateOf(false) }
    var showBackupsListDialog by remember { mutableStateOf(false) }
    var availableBackups by remember { mutableStateOf<List<BackupItem>>(emptyList()) }
    var isLoadingBackups by remember { mutableStateOf(false) }
    var backupToRestore by remember { mutableStateOf<BackupItem?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var showTestNotificationSent by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings & Appearance", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Account & Google Sign-In Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentUser != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val initialLetter = currentUser?.displayName?.take(1)?.uppercase()
                            ?: currentUser?.email?.take(1)?.uppercase()
                            ?: "G"
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initialLetter,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            if (currentUser != null) {
                                Text(
                                    text = currentUser.displayName ?: "Google User",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = currentUser.email ?: "Signed in",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Google Account",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Sync habits & backup to cloud",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (currentUser != null) {
                            OutlinedButton(
                                onClick = { showSignOutConfirmDialog = true },
                                modifier = Modifier.testTag("google_sign_out_button")
                            ) {
                                Text("Sign Out")
                            }
                        } else {
                            Button(
                                onClick = onSignInWithGoogle,
                                enabled = !isLoadingAuth,
                                modifier = Modifier.testTag("google_login_button")
                            ) {
                                if (isLoadingAuth) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Sign In")
                                }
                            }
                        }
                    }

                    if (currentUser != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Sync Status Indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                when (syncStatus) {
                                    SyncStatus.SYNCED -> {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Synced",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val timeStr = if (lastSyncTime > 0L) {
                                            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(lastSyncTime))
                                        } else "Just now"
                                        Text(
                                            text = "Synced ($timeStr)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    SyncStatus.SYNCING -> {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Syncing with cloud...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    SyncStatus.OFFLINE -> {
                                        Icon(
                                            imageVector = Icons.Default.CloudDownload,
                                            contentDescription = "Offline",
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Offline (changes saved locally)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFF59E0B)
                                        )
                                    }
                                    SyncStatus.ERROR -> {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Error",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = syncErrorMessage ?: "Sync error",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            maxLines = 1
                                        )
                                    }
                                    SyncStatus.DISABLED -> {
                                        Text(
                                            text = "Cloud sync disabled",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            TextButton(
                                onClick = onTriggerSync,
                                enabled = syncStatus != SyncStatus.SYNCING,
                                modifier = Modifier.testTag("manual_sync_button")
                            ) {
                                Text("Sync Now", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Google Drive & Cloud Backup Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google Drive & Cloud Backup",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val lastBackupStr = if (settings.lastBackupTimestamp > 0L) {
                        SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(settings.lastBackupTimestamp))
                    } else "Never backed up"
                    Text(
                        text = "Last successful backup: $lastBackupStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Daily auto backup toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Automatic Daily Backup", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Text("Saves snapshot every 24h (keeps last 7 backups)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.autoBackupEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(autoBackupEnabled = it)) },
                            modifier = Modifier.testTag("auto_backup_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onBackupNow,
                            enabled = !isBackingUp && currentUser != null,
                            modifier = Modifier.weight(1f).testTag("backup_now_button")
                        ) {
                            if (isBackingUp) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backing up...")
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Back Up Now")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (currentUser != null) {
                                    isLoadingBackups = true
                                    onListBackups { list ->
                                        availableBackups = list
                                        isLoadingBackups = false
                                        showBackupsListDialog = true
                                    }
                                } else {
                                    statusMessage = "Please sign in with Google to view cloud backups."
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("restore_cloud_backup_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore")
                        }
                    }
                }
            }

            // Live Preview Card
            Text(
                text = "Live Appearance Preview",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Previewing home screen cells with current settings:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val sampleYesNo = Habit(
                        id = 991,
                        name = "Meditation (Yes/No)",
                        type = HabitType.YES_NO.name,
                        colorHex = settings.accentColor
                    )
                    val sampleCount = Habit(
                        id = 992,
                        name = "Water (Count 8x)",
                        type = HabitType.COUNT.name,
                        target = 8f,
                        unit = "glasses",
                        colorHex = "#0284C7"
                    )

                    // Row 1: Yes/No sample
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sampleYesNo.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HabitCell(
                                habit = sampleYesNo,
                                date = LocalDate.now(),
                                value = 1f,
                                isToday = false,
                                settings = settings,
                                onClick = {},
                                onLongClick = {}
                            )
                            HabitCell(
                                habit = sampleYesNo,
                                date = LocalDate.now().minusDays(1),
                                value = 0f,
                                isToday = false,
                                settings = settings,
                                onClick = {},
                                onLongClick = {}
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 2: Count sample
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sampleCount.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HabitCell(
                                habit = sampleCount,
                                date = LocalDate.now(),
                                value = 5f, // partial
                                isToday = true,
                                settings = settings,
                                onClick = {},
                                onLongClick = {}
                            )
                            HabitCell(
                                habit = sampleCount,
                                date = LocalDate.now().minusDays(1),
                                value = 8f, // completed
                                isToday = false,
                                settings = settings,
                                onClick = {},
                                onLongClick = {}
                            )
                        }
                    }
                }
            }

            // Section: Theme & Theme Mode
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Theme Mode", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "SYSTEM" to "System",
                            "LIGHT" to "Light",
                            "DARK" to "Dark",
                            "AMOLED" to "AMOLED Black"
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { onUpdateSettings(settings.copy(themeMode = mode)) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("theme_mode_$mode")
                            )
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Dynamic Color (Material You)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                Text("Matches Android 12+ wallpaper palette", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = settings.dynamicColor,
                                onCheckedChange = { onUpdateSettings(settings.copy(dynamicColor = it)) },
                                modifier = Modifier.testTag("dynamic_color_switch")
                            )
                        }
                    }

                    if (!settings.dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("App Accent Color", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(
                                "#10B981" to "Emerald",
                                "#0284C7" to "Sky",
                                "#8B5CF6" to "Violet",
                                "#F59E0B" to "Amber",
                                "#F43F5E" to "Rose",
                                "#6366F1" to "Indigo"
                            ).forEach { (hex, _) ->
                                val color = parseHexColor(hex)
                                val isSelected = settings.accentColor.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { onUpdateSettings(settings.copy(accentColor = hex)) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section: Table & Cell Style
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cell Styling", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Cell Shape", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ROUNDED_SQUARE" to "Rounded", "CIRCLE" to "Circle", "SQUARE" to "Square").forEach { (shape, lbl) ->
                            FilterChip(
                                selected = settings.cellShape == shape,
                                onClick = { onUpdateSettings(settings.copy(cellShape = shape)) },
                                label = { Text(lbl) },
                                modifier = Modifier.testTag("shape_$shape")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Cell Size", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("COMPACT" to "Compact", "NORMAL" to "Normal", "LARGE" to "Large").forEach { (sz, lbl) ->
                            FilterChip(
                                selected = settings.cellSize == sz,
                                onClick = { onUpdateSettings(settings.copy(cellSize = sz)) },
                                label = { Text(lbl) },
                                modifier = Modifier.testTag("size_$sz")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Done Style", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("FILLED" to "Filled Color", "CHECK_ICON" to "Check Icon").forEach { (st, lbl) ->
                            FilterChip(
                                selected = settings.doneStyle == st,
                                onClick = { onUpdateSettings(settings.copy(doneStyle = st)) },
                                label = { Text(lbl) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Not-Done Style", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("FADED" to "Faded Dot", "EMPTY" to "Empty", "X_MARK" to "X Mark").forEach { (st, lbl) ->
                            FilterChip(
                                selected = settings.notDoneStyle == st,
                                onClick = { onUpdateSettings(settings.copy(notDoneStyle = st)) },
                                label = { Text(lbl) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Count Habits Display Style", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "PROGRESS_RING" to "Progress Ring",
                            "NUMBER_ONLY" to "Number Only",
                            "NUMBER_TARGET" to "Current / Target (5/8)"
                        ).forEach { (st, lbl) ->
                            FilterChip(
                                selected = settings.countDisplayStyle == st,
                                onClick = { onUpdateSettings(settings.copy(countDisplayStyle = st)) },
                                label = { Text(lbl) },
                                modifier = Modifier.testTag("count_style_$st")
                            )
                        }
                    }
                }
            }

            // Section: Calendar & Charts Preferences
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Calendar & Chart Preferences", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("First Day of the Week", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("MONDAY" to "Monday", "SUNDAY" to "Sunday", "SATURDAY" to "Saturday").forEach { (day, lbl) ->
                            FilterChip(
                                selected = settings.firstDayOfWeek == day,
                                onClick = { onUpdateSettings(settings.copy(firstDayOfWeek = day)) },
                                label = { Text(lbl) },
                                modifier = Modifier.testTag("first_day_$day")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Streaks on Home Rows", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = settings.showStreaksOnHome,
                            onCheckedChange = { onUpdateSettings(settings.copy(showStreaksOnHome = it)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Grid Lines on History Charts", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = settings.showGridLines,
                            onCheckedChange = { onUpdateSettings(settings.copy(showGridLines = it)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Value Numbers on Chart Bars", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = settings.showBarValues,
                            onCheckedChange = { onUpdateSettings(settings.copy(showBarValues = it)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Archived Habits", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = settings.showArchived,
                            onCheckedChange = { onUpdateSettings(settings.copy(showArchived = it)) }
                        )
                    }
                }
            }

            // Section: Notifications & Reminders
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Notifications", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Trigger a test notification to verify habit reminder delivery on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            NotificationHelper.showReminderNotification(
                                context = context,
                                habitName = "Daily Habits Check-in",
                                message = "Don't forget to mark your habits today in P Habits!"
                            )
                            showTestNotificationSent = true
                        },
                        modifier = Modifier.testTag("test_notification_button")
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (showTestNotificationSent) "Notification Triggered!" else "Send Test Reminder")
                    }
                }
            }

            // Section: Backup & Export
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Backup & Data Export", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Export your habit data to CSV or JSON backup, or restore from a backup string.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val csv = BackupExportUtil.generateCsv(habits, entries)
                                BackupExportUtil.shareText(context, csv, "P_Habits_Export.csv")
                            },
                            modifier = Modifier.weight(1f).testTag("export_csv_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export CSV")
                        }

                        OutlinedButton(
                            onClick = {
                                val json = BackupExportUtil.generateBackupJson(habits, entries)
                                BackupExportUtil.shareText(context, json, "P_Habits_Backup.json")
                            },
                            modifier = Modifier.weight(1f).testTag("export_json_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export JSON")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("import_json_button")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Backup JSON")
                    }
                }
            }

            // Section: Sample Data & Reset
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                val hasSampleData = habits.any { it.uuid.startsWith("sample-habit-") && !it.isDeleted }
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sample Data & Reset", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (hasSampleData)
                            "Sample demo data is currently active. Creating your first habit will automatically delete all sample data and start auto-syncing. You can also clear or reset sample data here to begin auto-syncing immediately."
                        else
                            "Manage sample data or reset your habits. Clearing or resetting triggers auto-sync.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReloadSampleData,
                            modifier = Modifier.weight(1f).testTag("reload_sample_data_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Sample")
                        }

                        Button(
                            onClick = { showClearConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).testTag("clear_all_data_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (hasSampleData) "Clear Sample" else "Clear All")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "P Habits",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Version ${com.example.BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showClearConfirmDialog) {
        val hasSample = habits.any { it.uuid.startsWith("sample-habit-") && !it.isDeleted }
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(if (hasSample) "Clear Sample Data?" else "Clear All Habit Data?") },
            text = {
                Text(
                    if (hasSample)
                        "This will delete all sample habits and records, and start auto-syncing your real data to your Google account."
                    else
                        "This will permanently delete all your habits, records, and streak history. This cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (hasSample) "Clear Sample Data" else "Clear Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                importError = null
            },
            title = { Text("Restore from Backup JSON") },
            text = {
                Column {
                    Text("Paste your backup JSON below to restore habits and entries:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = {
                            importJsonText = it
                            importError = null
                        },
                        label = { Text("Backup JSON") },
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (importError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(importError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val (restoredHabits, restoredEntries) = BackupExportUtil.parseBackupJson(importJsonText)
                            if (restoredHabits.isNotEmpty()) {
                                onRestoreBackup(restoredHabits, restoredEntries)
                                showImportDialog = false
                            } else {
                                importError = "No valid habits found in JSON"
                            }
                        } catch (e: Exception) {
                            importError = "Invalid JSON format: ${e.message}"
                        }
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSignOutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmDialog = false },
            title = { Text("Sign Out from Google Account") },
            text = { Text("Do you want to keep your habit data on this device or remove it?\n\nYour data remains safely stored in your Google cloud account either way.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirmDialog = false
                        onSignOutWithChoice(false)
                    }
                ) {
                    Text("Keep on Device")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showSignOutConfirmDialog = false }) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            showSignOutConfirmDialog = false
                            onSignOutWithChoice(true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Remove from Device")
                    }
                }
            }
        )
    }

    if (showBackupsListDialog) {
        AlertDialog(
            onDismissRequest = { showBackupsListDialog = false },
            title = { Text("Available Backups") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isLoadingBackups) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.CircularProgressIndicator()
                        }
                    } else if (availableBackups.isEmpty()) {
                        Text("No backups found yet. Tap 'Back Up Now' to create one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableBackups.size) { idx ->
                                val backup = availableBackups[idx]
                                val dateStr = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault()).format(Date(backup.timestamp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(dateStr, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                            Text("${backup.habitCount} habits • ${backup.entryCount} check-ins", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Button(
                                            onClick = { backupToRestore = backup },
                                            modifier = Modifier.testTag("restore_backup_item_${backup.id}")
                                        ) {
                                            Text("Restore")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupsListDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (backupToRestore != null) {
        val b = backupToRestore!!
        val dateStr = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault()).format(Date(b.timestamp))
        AlertDialog(
            onDismissRequest = { backupToRestore = null },
            title = { Text("Confirm Restore") },
            text = {
                Text("Restore backup from $dateStr?\n\nThis will replace current local habits with ${b.habitCount} habits and ${b.entryCount} check-ins.\n\nSAFETY: A local emergency backup of your current habits will be created automatically before restoring.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreCloudBackup(b)
                        backupToRestore = null
                        showBackupsListDialog = false
                    }
                ) {
                    Text("Confirm Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (statusMessage != null) {
        AlertDialog(
            onDismissRequest = { statusMessage = null },
            title = { Text("Notice") },
            text = { Text(statusMessage!!) },
            confirmButton = {
                TextButton(onClick = { statusMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}
