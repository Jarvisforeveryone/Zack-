package com.example.ui

import android.media.AudioManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.Action
import com.example.data.Command
import com.example.data.Reminder
import com.example.data.VoiceCommandManager
import com.example.data.AppDatabase
import kotlinx.coroutines.launch

@Composable
fun SystemControlScreen(
    viewModel: AiraViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "automation_home",
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable("automation_home") {
            AutomationHomeScreen(
                viewModel = viewModel,
                onNavigateToSmartAuto = { navController.navigate("smart_auto_screen") },
                onNavigateToMyActions = { navController.navigate("my_actions_screen") },
                onNavigateToVoiceCommands = { navController.navigate("voice_command_screen") }
            )
        }
        composable("smart_auto_screen") {
            SmartAutoScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("my_actions_screen") {
            MyActionsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("voice_command_screen") {
            VoiceCommandScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun AutomationHomeScreen(
    viewModel: AiraViewModel,
    onNavigateToSmartAuto: () -> Unit,
    onNavigateToMyActions: () -> Unit,
    onNavigateToVoiceCommands: () -> Unit
) {
    val reminders by viewModel.reminders.collectAsState()
    val scrollState = rememberScrollState()

    // Screen-level state for quick controls
    var alarmHour by remember { mutableStateOf("07") }
    var alarmMinute by remember { mutableStateOf("00") }
    var reminderTitle by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf("09:00 AM") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Device Tools",
                fontWeight = FontWeight.Light,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
            )
        }

        // ================== SYSTEM UTILS ==================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Flashlight card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Flashlight",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.toggleFlashlight(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f).testTag("flash_on_btn"),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.FlashOn, "ON", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("TurnOn", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                        }
                        Button(
                            onClick = { viewModel.toggleFlashlight(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray.copy(alpha = 0.2f)),
                            modifier = Modifier.weight(1f).testTag("flash_off_btn"),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.FlashOff, "OFF", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("TurnOff", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.LightGray)
                        }
                    }
                }
            }

            // Audio Mode selector card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SoundProfile",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { viewModel.setSoundMode(android.media.AudioManager.RINGER_MODE_NORMAL) }) {
                            Icon(Icons.Default.VolumeUp, "Normal Sound", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.setSoundMode(android.media.AudioManager.RINGER_MODE_VIBRATE) }) {
                            Icon(Icons.Default.Notifications, "Vibrate State", tint = Color.White.copy(alpha = 0.8f))
                        }
                        IconButton(onClick = { viewModel.setSoundMode(android.media.AudioManager.RINGER_MODE_SILENT) }) {
                            Icon(Icons.Default.VolumeMute, "Silent State", tint = Color.Red.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }

        // ALARM SYSTEM SCHEDULER
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "► Alarm",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = alarmHour,
                        onValueChange = { alarmHour = it.take(2) },
                        modifier = Modifier.weight(1f).testTag("alarm_hour_input"),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Light),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.10f)
                        ),
                        label = { Text("Hour24", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Light) }
                    )
                    OutlinedTextField(
                        value = alarmMinute,
                        onValueChange = { alarmMinute = it.take(2) },
                        modifier = Modifier.weight(1f).testTag("alarm_min_input"),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Light),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.10f)
                        ),
                        label = { Text("Minutes", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Light) }
                    )

                    Button(
                        onClick = {
                            val h = alarmHour.toIntOrNull() ?: 7
                            val m = alarmMinute.toIntOrNull() ?: 0
                            viewModel.setSystemAlarm(h, m, "Aira Scheduled Alert")
                            viewModel.speakText("Scheduled Chronos Alarm for $h:$m successfully")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1.2f).testTag("set_alarm_btn")
                    ) {
                        Text("SetAlarm", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // SAVE REMINDERS BLOCK
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "► Reminders",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = reminderTitle,
                    onValueChange = { reminderTitle = it },
                    modifier = Modifier.fillMaxWidth().testTag("reminder_title_input"),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Light),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.10f)
                    ),
                    label = { Text("TaskMessage", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Light) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = reminderTime,
                        onValueChange = { reminderTime = it },
                        modifier = Modifier.weight(1.5f).testTag("reminder_time_input"),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Light),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.10f)
                        ),
                        label = { Text("TimeLabel", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Light) }
                    )

                    Button(
                        onClick = {
                            if (reminderTitle.isNotEmpty()) {
                                viewModel.addReminder(reminderTitle, reminderTime)
                                viewModel.speakText("Commit memory log: $reminderTitle.")
                                reminderTitle = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f).testTag("add_reminder_btn")
                    ) {
                        Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("SaveTask", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                // List of Reminders
                if (reminders.isEmpty()) {
                    Text(
                        text = "NoRemindersYet",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    reminders.forEach { reminder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.02f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reminder.title.uppercase(),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = 1.5.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Alarm point: " + reminder.timeLabel,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.deleteReminder(reminder) },
                                modifier = Modifier.testTag("delete_reminder_${reminder.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Reminder",
                                    tint = Color.Red.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ================== REDESIGNED SECTION: 3 NAVIGATION OPTIONS ==================
        Text(
            text = "► Automation",
            fontSize = 11.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 1.5.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Option 1: Smart Auto
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSmartAuto() }
                .testTag("smart_auto_nav_card"),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Smart Suggestions",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "RoutinesByAiAnalyzer",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Go to Smart Auto",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Option 2: My Actions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToMyActions() }
                .testTag("my_actions_nav_card"),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MyActions",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "YourCustomActions",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Go to My Actions",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Option 3: Voice Commands
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToVoiceCommands() }
                .testTag("voice_commands_nav_card"),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Voice Commands",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "SetupVoiceTriggers",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Go to Voice Commands",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Voice Command Execution Log Section
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "► Voice Activity",
                fontSize = 11.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
            
            val logs by viewModel.voiceCommandLogs.collectAsState()
            if (logs.isNotEmpty()) {
                Text(
                    text = "CLEAR ALL",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Light,
                    color = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clickable { viewModel.clearVoiceCommandLogs() }
                        .testTag("clear_voice_logs_btn")
                        .padding(4.dp)
                )
            }
        }

        val logs by viewModel.voiceCommandLogs.collectAsState()
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("voice_command_logs_card"),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "No recent voice commands logged.",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Light,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                } else {
                    logs.forEach { log ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Mic,
                                        contentDescription = "Command Speech Input",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = log.command.uppercase(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = log.timestamp,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }

                            if (!log.matchedTrigger.isNullOrEmpty()) {
                                Text(
                                    text = "Recognized Phrase: ${log.matchedTrigger}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = log.details,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )

                                val (statusText, statusColor) = when (log.status) {
                                    "SUCCESS" -> "Completed" to Color(0xFF00E5FF)
                                    "ABORTED" -> "Cancelled" to Color(0xFFFFB300)
                                    else -> "Failed" to Color(0xFFE53935)
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(statusColor.copy(alpha = 0.08f))
                                        .border(0.5.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = statusColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAutoScreen(
    viewModel: AiraViewModel,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // States for Smart Auto suggestions
    val showsNightInstalled = remember { mutableStateOf(false) }
    val showsLensInstalled = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Suggested Actions",
                        fontWeight = FontWeight.Light,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("smart_auto_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "► Recommended Actions",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Suggested for You",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.1.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Suggestion 1: Night routine
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.01f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = "SleepChain",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                            Text(
                                text = "Trigger: GoodNight",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Light,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val dbInstance = VoiceCommandManager.getInstance(viewModel.getApplication())
                                    val dao = AppDatabase.getDatabase(viewModel.getApplication()).voiceCommandDao()
                                    val acts = dao.getAllActions()
                                    val silId = acts.find { it.name.contains("Silent") }?.id ?: 7L
                                    val flOffId = acts.find { it.name.contains("Flashlight Off") }?.id ?: 2L
                                    viewModel.insertCommand(Command(
                                        triggerPhrase = "good night",
                                        actionIdsJson = "[$silId, $flOffId]",
                                        priority = 6
                                    ))
                                    showsNightInstalled.value = true
                                    viewModel.speakText("Installed Sleep Routine")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            modifier = Modifier.height(26.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            enabled = !showsNightInstalled.value
                        ) {
                            Text(
                                text = if (showsNightInstalled.value) "Installed" else "Install",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Suggestion 2: Max sight
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.01f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = "VisionBooster",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                            Text(
                                text = "Trigger: Boost",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Light,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val dbInstance = VoiceCommandManager.getInstance(viewModel.getApplication())
                                    val dao = AppDatabase.getDatabase(viewModel.getApplication()).voiceCommandDao()
                                    val acts = dao.getAllActions()
                                    val flOnId = acts.find { it.name.contains("Flashlight On") }?.id ?: 1L
                                    val bId = acts.find { it.name.contains("Set Brightness") }?.id ?: 10L
                                    viewModel.insertCommand(Command(
                                        triggerPhrase = "boost",
                                        actionIdsJson = "[$flOnId, $bId]",
                                        priority = 9
                                    ))
                                    showsLensInstalled.value = true
                                    viewModel.speakText("Installed Max Vision booster")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            modifier = Modifier.height(26.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            enabled = !showsLensInstalled.value
                        ) {
                            Text(
                                text = if (showsLensInstalled.value) "Installed" else "Install",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyActionsScreen(
    viewModel: AiraViewModel,
    onBack: () -> Unit
) {
    val allActions by viewModel.allActions.collectAsState()
    val allCommands by viewModel.allCommands.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Add Action Screen overlay/card expanded panel states
    var showAddActionPanel by remember { mutableStateOf(false) }
    var newActionName by remember { mutableStateOf("") }
    var newActionType by remember { mutableStateOf("SYSTEM_API") } // SYSTEM_API, INTENT, SHELL, DELAY
    var newActionParams by remember { mutableStateOf("{\"action\":\"flashlight_on\"}") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MyActions",
                        fontWeight = FontWeight.Light,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("my_actions_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "► My Actions",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 1.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Button(
                            onClick = { showAddActionPanel = !showAddActionPanel },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                            modifier = Modifier.height(26.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(if (showAddActionPanel) Icons.Default.Close else Icons.Default.Add, "Toggle Add Action", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (showAddActionPanel) "Close" else "NewAction", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Expansion panel for creating Actions
                    if (showAddActionPanel) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("CreateActionChain", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)

                            OutlinedTextField(
                                value = newActionName,
                                onValueChange = { newActionName = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Light),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                label = { Text("ActionName", fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                            )

                            // Type dropdown toggle (simulated via horizontal row selections)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("ActionType", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.6f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("SYSTEM_API", "INTENT", "SHELL", "DELAY").forEach { type ->
                                        val isSelected = newActionType == type
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f))
                                                .border(0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.10f), RoundedCornerShape(4.dp))
                                                .clickable {
                                                    newActionType = type
                                                    newActionParams = when (type) {
                                                        "SYSTEM_API" -> "{\"action\":\"flashlight_on\"}"
                                                        "INTENT" -> "{\"action\":\"open_camera\"}"
                                                        "SHELL" -> "{\"command\":\"ls -la\"}"
                                                        "DELAY" -> "{\"duration\":500}"
                                                        else -> "{}"
                                                    }
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val displayName = when(type) {
                                                "SYSTEM_API" -> "SystemApi"
                                                "INTENT" -> "Intent"
                                                "SHELL" -> "Shell"
                                                "DELAY" -> "Delay"
                                                else -> type
                                            }
                                            Text(displayName, fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White)
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = newActionParams,
                                onValueChange = { newActionParams = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Light),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                label = { Text("JsonParams", fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                            )

                            Button(
                                onClick = {
                                    if (newActionName.isNotEmpty()) {
                                        viewModel.insertAction(Action(
                                            name = newActionName,
                                            type = newActionType,
                                            paramsJson = newActionParams
                                        ))
                                        newActionName = ""
                                        showAddActionPanel = false
                                        viewModel.speakText("Action registered successfully")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("SaveAction", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }

                    // List of registered Actions with tap listener & voice alias
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        allActions.forEach { action ->
                            val associatedCommand = allCommands.find { cmd ->
                                val actionIds = try {
                                    org.json.JSONArray(cmd.actionIdsJson).let { arr ->
                                        (0 until arr.length()).map { arr.getLong(it) }
                                    }
                                } catch (e: Exception) {
                                    cmd.actionIdsJson.replace("[", "").replace("]", "").split(",").mapNotNull { it.trim().toLongOrNull() }
                                }
                                actionIds.contains(action.id)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.01f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                    .clickable(enabled = associatedCommand != null) {
                                        associatedCommand?.let { cmd ->
                                            coroutineScope.launch {
                                                val m = VoiceCommandManager.getInstance(viewModel.getApplication())
                                                val phraseWithDefaultVal = cmd.triggerPhrase.replace("{number}", "50").replace("{text}", "boss")
                                                m.matchAndExecuteCommand(phraseWithDefaultVal, viewModel)
                                            }
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(action.name.uppercase(), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                                    Text("Type: ${action.type} • Params: ${action.paramsJson}", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.4f))
                                    if (associatedCommand != null) {
                                        Text(
                                            text = "Voice Phrase: '${associatedCommand.triggerPhrase.uppercase()}'",
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (associatedCommand != null) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Run",
                                            tint = Color.Green.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    // Delete action (if not core preloaded)
                                    IconButton(
                                        onClick = { viewModel.deleteAction(action) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCommandScreen(
    viewModel: AiraViewModel,
    onBack: () -> Unit
) {
    val allActions by viewModel.allActions.collectAsState()
    val allCommands by viewModel.allCommands.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var commandSearchQuery by remember { mutableStateOf("") }
    var conflictWarningMessage by remember { mutableStateOf<String?>(null) }
    
    // Add Command Screen overlay/card expanded panel states
    var showAddCommandPanel by remember { mutableStateOf(false) }
    var newTriggerPhrase by remember { mutableStateOf("") }
    var newCommandPriority by remember { mutableStateOf("5") }
    val newSelectedActionIds = remember { mutableStateListOf<Long>() }
    var newBatteryCondition by remember { mutableStateOf("NONE") } // NONE, LT_20, GT_80
    var newTimeCondition by remember { mutableStateOf("NONE") } // NONE, DAY, NIGHT

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Voice Commands",
                        fontWeight = FontWeight.Light,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("voice_commands_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "► Voice Triggers",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 1.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Button(
                            onClick = { showAddCommandPanel = !showAddCommandPanel },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                            modifier = Modifier.height(26.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(if (showAddCommandPanel) Icons.Default.Close else Icons.Default.Add, "Toggle Add Command", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (showAddCommandPanel) "Close" else "NewCommand", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Add custom command sub-panel (gilded overlay structure)
                    if (showAddCommandPanel) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Create Voice Command", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)

                            OutlinedTextField(
                                value = newTriggerPhrase,
                                onValueChange = { newTriggerPhrase = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Light),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                label = { Text("TriggerPhrase", fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                            )

                            Text(
                                "Use {number} or {text}",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )

                            OutlinedTextField(
                                value = newCommandPriority,
                                onValueChange = { newCommandPriority = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Light),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                label = { Text("Priority1to10", fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                            )

                            // Actions checklist selection layout
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("SelectActions", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.6f))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    allActions.forEach { act ->
                                        val isChecked = newSelectedActionIds.contains(act.id)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f) else Color.Transparent)
                                                .clickable {
                                                    if (isChecked) newSelectedActionIds.remove(act.id)
                                                    else newSelectedActionIds.add(act.id)
                                                }
                                                .padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = {
                                                    if (isChecked) newSelectedActionIds.remove(act.id)
                                                    else newSelectedActionIds.add(act.id)
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.scale(0.8f)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(act.name.uppercase() + " [${act.type}]", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = if (isChecked) MaterialTheme.colorScheme.primary else Color.White)
                                        }
                                    }
                                }
                            }

                            // Exceptional Feature 2: Conditional Chain Constraint options
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Conditions", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.6f))
                                
                                // Battery condition row
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Battery", fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(60.dp))
                                    listOf("NONE", "LT_20", "GT_80").forEach { item ->
                                        val isSel = newBatteryCondition == item
                                        val disp = when(item) {
                                            "NONE" -> "NoLimit"
                                            "LT_20" -> "LowBat"
                                            "GT_80" -> "HighBat"
                                            else -> item
                                        }
                                        Text(
                                            text = disp,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.02f))
                                                .border(0.5.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                                .clickable { newBatteryCondition = item }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                // Time condition row
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("TimeRange", fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(60.dp))
                                    listOf("NONE", "DAY", "NIGHT").forEach { item ->
                                        val isSel = newTimeCondition == item
                                        val disp = when(item) {
                                            "NONE" -> "NoLimit"
                                            "DAY" -> "Day"
                                            "NIGHT" -> "Night"
                                            else -> item
                                        }
                                        Text(
                                            text = disp,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.02f))
                                                .border(0.5.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                                .clickable { newTimeCondition = item }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            conflictWarningMessage?.let { msg ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(text = "ConflictWarning", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                        Text(text = msg, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    // Force insert override path
                                                    if (newTriggerPhrase.isNotEmpty() && newSelectedActionIds.isNotEmpty()) {
                                                        val conditionsObj = org.json.JSONObject()
                                                        if (newBatteryCondition == "LT_20") conditionsObj.put("batteryLt", 20)
                                                        if (newBatteryCondition == "GT_80") conditionsObj.put("batteryGt", 80)
                                                        if (newTimeCondition != "NONE") conditionsObj.put("timeRange", newTimeCondition)

                                                        val actionIdsJson = org.json.JSONArray(newSelectedActionIds.toList()).toString()

                                                        viewModel.insertCommand(Command(
                                                            triggerPhrase = newTriggerPhrase.lowercase().trim(),
                                                            priority = newCommandPriority.toIntOrNull() ?: 5,
                                                            actionIdsJson = actionIdsJson,
                                                            conditionsJson = if (conditionsObj.length() > 0) conditionsObj.toString() else ""
                                                        ))

                                                        newTriggerPhrase = ""
                                                        newSelectedActionIds.clear()
                                                        newBatteryCondition = "NONE"
                                                        newTimeCondition = "NONE"
                                                        showAddCommandPanel = false
                                                        conflictWarningMessage = null
                                                        viewModel.speakText("Speech path triggers updated")
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(24.dp)
                                            ) {
                                                Text("FORCE ADD TYPE", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onError)
                                            }
                                            Button(
                                                onClick = {
                                                    conflictWarningMessage = null
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(24.dp)
                                            ) {
                                                Text("CANCEL", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (newTriggerPhrase.isNotEmpty() && newSelectedActionIds.isNotEmpty()) {
                                        val lowerNewPhrase = newTriggerPhrase.lowercase().trim()
                                        
                                        // Scan and find matching high-sim conflict >= 95%
                                        val conflictCmd = allCommands.find {
                                            calculateLevenshteinSimilarity(lowerNewPhrase, it.triggerPhrase.lowercase().trim()) > 0.95f
                                        }

                                        if (conflictCmd != null) {
                                            conflictWarningMessage = "This is too similar to '${conflictCmd.triggerPhrase.uppercase()}'. Merge them?"
                                        } else {
                                            val conditionsObj = org.json.JSONObject()
                                            if (newBatteryCondition == "LT_20") conditionsObj.put("batteryLt", 20)
                                            if (newBatteryCondition == "GT_80") conditionsObj.put("batteryGt", 80)
                                            if (newTimeCondition != "NONE") conditionsObj.put("timeRange", newTimeCondition)

                                            val actionIdsJson = org.json.JSONArray(newSelectedActionIds.toList()).toString()

                                            viewModel.insertCommand(Command(
                                                triggerPhrase = newTriggerPhrase.lowercase().trim(),
                                                priority = newCommandPriority.toIntOrNull() ?: 5,
                                                actionIdsJson = actionIdsJson,
                                                conditionsJson = if (conditionsObj.length() > 0) conditionsObj.toString() else ""
                                            ))

                                            // Reset fields
                                            newTriggerPhrase = ""
                                            newSelectedActionIds.clear()
                                            newBatteryCondition = "NONE"
                                            newTimeCondition = "NONE"
                                            showAddCommandPanel = false
                                            viewModel.speakText("Speech path triggers updated")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("SaveTrigger", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                    // Floating Search input to filter trigger phrases quickly
                    OutlinedTextField(
                        value = commandSearchQuery,
                        onValueChange = { commandSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Light),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.10f)
                        ),
                        placeholder = { Text("SearchTriggers", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        leadingIcon = { Icon(Icons.Outlined.Search, "Search Icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                    )

                    // Render matching voice commands
                    val filteredCommands = allCommands.filter {
                        it.triggerPhrase.lowercase().contains(commandSearchQuery.lowercase())
                    }

                    if (filteredCommands.isEmpty()) {
                        Text(
                            text = "NoResultsFound",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    } else {
                        filteredCommands.forEach { cmd ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.02f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = cmd.triggerPhrase.uppercase(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Light,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 1.2.sp
                                        )

                                        // Badge priority
                                        Text(
                                            text = "P${cmd.priority}",
                                            fontSize = 7.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                                .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )

                                        // Conditions constraint badge
                                        if (cmd.conditionsJson.isNotEmpty()) {
                                            Icon(Icons.Default.Lock, "Condition Locked", tint = Color(0xFFD4AF37), modifier = Modifier.size(10.dp))
                                        }
                                    }

                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Action Sequence: " + cmd.actionIdsJson,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White.copy(alpha = 0.4f)
                                    )

                                    if (cmd.conditionsJson.isNotEmpty()) {
                                        Text(
                                            text = "Conditions: " + cmd.conditionsJson,
                                            fontSize = 7.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFD4AF37).copy(alpha = 0.6f)
                                        )
                                    }

                                    Spacer(Modifier.height(1.dp))
                                    Text(
                                        text = "Used ${cmd.useCount} times",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // TEST COMMAND BUTTON
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                // Run the action manager matching engine directly bypassing vocal STT!
                                                val m = VoiceCommandManager.getInstance(viewModel.getApplication())
                                                val phraseWithDefaultVal = cmd.triggerPhrase.replace("{number}", "50").replace("{text}", "boss")
                                                m.matchAndExecuteCommand(phraseWithDefaultVal, viewModel)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp).testTag("test_cmd_${cmd.id}")
                                    ) {
                                        Icon(Icons.Outlined.PlayArrow, "Test Execute", tint = Color.Green.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                    }

                                    // DELETE COMMAND
                                    IconButton(
                                        onClick = { viewModel.deleteCommand(cmd) },
                                        modifier = Modifier.size(24.dp).testTag("delete_cmd_${cmd.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, "Delete Trigger", tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.size(36.dp) // standardized size scaling box
)

private fun calculateLevenshteinSimilarity(x: String, y: String): Float {
    val m = x.length
    val n = y.length
    if (m == 0 && n == 0) return 1.0f
    if (m == 0 || n == 0) return 0.0f
    
    val dp = IntArray(n + 1) { it }
    for (i in 1..m) {
        var prev = dp[0]
        dp[0] = i
        for (j in 1..n) {
            val temp = dp[j]
            if (x[i - 1] == y[j - 1]) {
                dp[j] = prev
            } else {
                dp[j] = minOf(dp[j - 1], dp[j], prev) + 1
            }
            prev = temp
        }
    }
    val distance = dp[n]
    val maxLen = maxOf(m, n)
    return 1.0f - (distance.toFloat() / maxLen)
}
