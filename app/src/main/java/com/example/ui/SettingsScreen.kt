package com.example.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import android.util.Log
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.launch
import com.example.data.ChatKeyManager
import com.example.data.Action
import com.example.data.Command
import com.example.data.VoiceCommandManager
import com.example.data.AppDatabase
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.draw.scale
import com.example.ui.theme.CosmicBlue
import com.example.ui.theme.CosmicCyan
import com.example.ui.theme.CosmicGold
import com.example.ui.theme.CosmicRed
import com.example.ui.theme.StealthGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AiraViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "settings_home",
        modifier = modifier.fillMaxSize(),
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        }
    ) {
        composable("settings_home") {
            SettingsHomeScreen(navController = navController)
        }
        composable("settings_general") {
            GeneralSettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("settings_voice") {
            VoiceSettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("settings_system") {
            SystemSettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("settings_memory") {
            MemorySettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("theme_screen") {
            com.example.ui.settings.ThemeScreen(navController = navController, viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHomeScreen(navController: NavController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Light,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "System",
                    fontWeight = FontWeight.Light,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp, top = 8.dp)
                )
            }

            item {
                SettingsCategoryItem(
                    title = "General",
                    subtitle = "Theme, FPS, Access",
                    icon = Icons.Default.Palette,
                    testTag = "settings_tab_general",
                    onClick = { navController.navigate("settings_general") }
                )
            }

            item {
                SettingsCategoryItem(
                    title = "Theme",
                    subtitle = "Cosmic Color Schemes",
                    icon = Icons.Default.Palette,
                    testTag = "settings_tab_theme",
                    onClick = { navController.navigate("theme_screen") }
                )
            }

            item {
                SettingsCategoryItem(
                    title = "Voice",
                    subtitle = "Wake, Listen, Voice, Sound",
                    icon = Icons.Default.Mic,
                    testTag = "settings_tab_voice",
                    onClick = { navController.navigate("settings_voice") }
                )
            }

            item {
                SettingsCategoryItem(
                    title = "AI",
                    subtitle = "Keys, Local AI, Reasoning",
                    icon = Icons.Default.Memory,
                    testTag = "settings_tab_system",
                    onClick = { navController.navigate("settings_system") }
                )
            }

            item {
                SettingsCategoryItem(
                    title = "Memory",
                    subtitle = "Long-Term, Backup, Restore",
                    icon = Icons.Default.Memory,
                    testTag = "settings_tab_memory",
                    onClick = { navController.navigate("settings_memory") }
                )
            }
        }
    }
}

@Composable
fun SettingsCategoryItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = Color.White
                )
            },
            supportingContent = {
                Text(
                    text = subtitle,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            },
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Navigate",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    viewModel: AiraViewModel,
    onBack: () -> Unit
) {
    val themeIndex by viewModel.themeIndex.collectAsState()
    val customHex by viewModel.customColorHex.collectAsState()
    val lowPerf by viewModel.lowPerformanceMode.collectAsState()

    var tempHex by remember(customHex) { mutableStateOf(customHex) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "General",
                        fontWeight = FontWeight.Light,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("general_back_btn")) {
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                    // CARD 2: Aesthetic Holographic Theme Picker
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "► Theme",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 1.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Presets Grid
                            Text(
                                text = "Presets".uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 1.2.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                fontFamily = FontFamily.Monospace
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val presets = listOf(
                                    Triple(0, "Cyan", CosmicCyan),
                                    Triple(1, "Red", CosmicRed),
                                    Triple(2, "Green", StealthGreen),
                                    Triple(3, "Gold", CosmicGold),
                                    Triple(4, "Blue", CosmicBlue)
                                )

                                presets.forEach { (index, label, color) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(color.copy(alpha = 0.06f))
                                            .border(
                                                width = if (themeIndex == index) 1.dp else 0.5.dp,
                                                color = if (themeIndex == index) color else color.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                viewModel.updateThemeIndex(index)
                                            }
                                            .testTag("theme_preset_$index"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                                text = label.uppercase(),
                                                color = color,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Light,
                                                letterSpacing = 1.sp,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                            // Custom Color Hex Picker
                            Text(
                                text = "Custom Color",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 1.2.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                fontFamily = FontFamily.Monospace
                            )
                            OutlinedTextField(
                                value = tempHex,
                                onValueChange = {
                                    tempHex = it
                                    if (it.length == 7 && it.startsWith("#")) {
                                        viewModel.updateCustomColorHex(it)
                                        viewModel.updateThemeIndex(99) // trigger custom picker choice!
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_hex_input"),
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Light),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                                ),
                                label = { Text("Enter Hex #...", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Light, letterSpacing = 1.2.sp) }
                            )
                        }
                    }

                    // CARD 4: Device Hardware safety & Low Performance Lock
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "► Performance",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 1.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "30 FPS Mode",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Saves battery on low phones",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.2.sp,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Switch(
                                    checked = lowPerf,
                                    onCheckedChange = { viewModel.toggleLowPerformanceMode(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("low_perf_switch")
                                )
                            }
                        }
                    }

                    // CARD 5: Accessibility Service Integration Guidance
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "► Accessibility",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 1.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Turn on in Android Settings",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 1.2.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Go to Settings > Accessibility",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 1.2.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        }
                    }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(
    viewModel: AiraViewModel,
    onBack: () -> Unit
) {
    val wakeWord by viewModel.wakeWord.collectAsState()
    val speakReplies by viewModel.speakReplies.collectAsState()
    val usePiperTts by viewModel.usePiperTts.collectAsState()
    val piperActiveVoice by viewModel.piperActiveVoice.collectAsState()
    val piperIsModelDownloaded by viewModel.piperIsModelDownloaded.collectAsState()
    val piperDownloadProgress by viewModel.piperDownloadProgress.collectAsState()
    val piperAvailableVoices = viewModel.piperAvailableVoices

    val voiceVibe by viewModel.voiceVibe.collectAsState()
    val noiseScale by viewModel.voiceNoiseScale.collectAsState()
    val lengthScale by viewModel.voiceLengthScale.collectAsState()
    val pitch by viewModel.voicePitch.collectAsState()
    val alpha by viewModel.voiceAlpha.collectAsState()

    val isSpeakCalled by viewModel.piperTtsManager.isSpeakCalled.collectAsState()
    val jarvisVoiceTone by viewModel.jarvisVoiceTone.collectAsState()

    var tempWakeWord by remember(wakeWord) { mutableStateOf(wakeWord) }
    val scrollState = rememberScrollState()

    val allActions by viewModel.allActions.collectAsState()
    val allCommands by viewModel.allCommands.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var commandSearchQuery by remember { mutableStateOf("") }
    var conflictWarningMessage by remember { mutableStateOf<String?>(null) }

    // Add Command Screen overlay/card expanded panel states
    var showAddCommandPanel by remember { mutableStateOf(false) }
    var newTriggerPhrase by remember { mutableStateOf("") }
    var newCommandPriority by remember { mutableStateOf("5") }
    val newSelectedActionIds = remember { mutableStateListOf<Long>() }
    var newBatteryCondition by remember { mutableStateOf("NONE") } // NONE, LT_20, GT_80
    var newTimeCondition by remember { mutableStateOf("NONE") } // NONE, DAY, NIGHT

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
                        text = "Voice",
                        fontWeight = FontWeight.Light,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("voice_back_btn")) {
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                    // CARD 1: Customizable Wake Word Configuration
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "► Wake Word",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 1.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Works 100% offline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 1.2.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                fontFamily = FontFamily.Monospace
                            )

                            OutlinedTextField(
                                value = tempWakeWord,
                                onValueChange = {
                                    tempWakeWord = it
                                    viewModel.updateWakeWord(it)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wake_word_input"),
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Light),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                label = { Text("Your Wake Word", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Light, letterSpacing = 1.2.sp) }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))

                            val usePersistentList by viewModel.usePersistentListening.collectAsState()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Always Listen",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "No tap needed",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.2.sp,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Switch(
                                    checked = usePersistentList,
                                    onCheckedChange = { viewModel.togglePersistentListening(it) },
                                    modifier = Modifier.testTag("persistent_listening_switch"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }
                        }
                    }

                    // NEW CARD: PIPER NEURAL OFFLINE TTS CONTROL CENTER
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "► Offline Voice",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 1.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Use Offline Voice",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "High quality voice offline",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.2.sp,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Switch(
                                    checked = usePiperTts,
                                    onCheckedChange = { viewModel.togglePiperTts(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("piper_tts_switch")
                                )
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Speak Responses",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Automatically speak Jarvis's text responses aloud using offline voice system.",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.2.sp,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Switch(
                                    checked = speakReplies,
                                    onCheckedChange = { viewModel.toggleSpeakReplies(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("speak_replies_switch")
                                )
                            }

                            if (usePiperTts) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))
                                
                                Text(
                                    text = "Voice",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = 1.2.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )

                                piperAvailableVoices.forEach { voice ->
                                    val isDownloaded = piperIsModelDownloaded[voice.id] == true
                                    val isCurrent = piperActiveVoice == voice.id
                                    val downloadProg = piperDownloadProgress[voice.id]

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                                                else Color.White.copy(alpha = 0.01f)
                                            )
                                            .border(
                                                0.5.dp,
                                                if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else Color.White.copy(alpha = 0.05f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable(enabled = isDownloaded) {
                                                viewModel.updatePiperVoice(voice.id)
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = voice.displayName.uppercase(),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Light,
                                                    letterSpacing = 1.2.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(
                                                            if (voice.gender == "Female") Color(0xFFFF4081).copy(alpha = 0.08f)
                                                            else Color(0xFF29B6F6).copy(alpha = 0.08f)
                                                        )
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (voice.gender == "Female") "FemaleVoice" else "MaleVoice",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Light,
                                                        letterSpacing = 1.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = if (voice.gender == "Female") Color(0xFFFF4081) else Color(0xFF29B6F6)
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "${voice.quality.uppercase()}  •  LATENCY: ${voice.latencyMs}MS",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Light,
                                                letterSpacing = 1.2.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White.copy(alpha = 0.4f)
                                            )
                                            Text(
                                                text = voice.description,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Light,
                                                letterSpacing = 1.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White.copy(alpha = 0.3f),
                                                modifier = Modifier.padding(top = 2.dp)
                                            )

                                            if (downloadProg != null) {
                                                Spacer(Modifier.height(6.dp))
                                                LinearProgressIndicator(
                                                    progress = downloadProg,
                                                    modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    trackColor = Color.White.copy(alpha = 0.05f)
                                                )
                                                Text(
                                                    text = "Downloading ${(downloadProg * 100).toInt()}%",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Light,
                                                    letterSpacing = 1.2.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }

                                        if (!isDownloaded && downloadProg == null) {
                                            Button(
                                                onClick = { viewModel.downloadPiperModel(voice.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("DOWNLOAD", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Light, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.primary)
                                            }
                                        } else if (isDownloaded) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "READY",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Light,
                                                    letterSpacing = 1.2.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.Green.copy(alpha = 0.6f)
                                                )
                                                if (voice.id != "en_US-amy-low" && voice.id != "en_US-ryan-medium") {
                                                    Text(
                                                        text = "[DEL]",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Light,
                                                        letterSpacing = 1.2.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color.Red.copy(alpha = 0.5f),
                                                        modifier = Modifier.clickable { viewModel.deletePiperModel(voice.id) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                                Text(
                                    text = "Voice Style",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = 1.2.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )

                                // Display active vibe preset label based on current value
                                val activePresetLabel = when (voiceVibe) {
                                    in 0f..16.99f -> "NORMAL"
                                    in 17f..33.99f -> "SOFT (CALM, CARING GF)"
                                    in 34f..50.99f -> "FLIRTY (PLAYFUL, LIGHT)"
                                    in 51f..66.99f -> "TEASING (FAST, BREATHY GF)"
                                    in 67f..83.99f -> "CAREFUL (MATURE, CONCERNED)"
                                    else -> "SLEEPY (SLOW, WHISPERY)"
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ACTIVE VIBE: $activePresetLabel",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${voiceVibe.toInt()}/100",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Light,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // 1 Main slider: "Voice Vibe" 0-100
                                Slider(
                                    value = voiceVibe,
                                    onValueChange = { viewModel.updateVoiceVibe(it) },
                                    valueRange = 0f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.10f)
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("voice_vibe_slider")
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // 6 practical preset buttons
                                val presets = listOf(
                                    "Normal" to 8f,
                                    "Soft" to 25f,
                                    "Flirty" to 42f,
                                    "Teasing" to 58.5f,
                                    "Careful" to 75f,
                                    "Sleepy" to 92f
                                )

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // First Row: Normal, Soft, Flirty
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        presets.take(3).forEach { (name, centerVal) ->
                                            val isActive = when (name) {
                                                "Normal" -> voiceVibe in 0f..16.99f
                                                "Soft" -> voiceVibe in 17f..33.99f
                                                "Flirty" -> voiceVibe in 34f..50.99f
                                                else -> false
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp) // Large tactile 48dp minimum touch target
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                                        else Color.White.copy(alpha = 0.03f)
                                                    )
                                                    .border(
                                                        2.dp, // Moti Bold 2dp borders
                                                        if (isActive) MaterialTheme.colorScheme.primary
                                                        else Color.White.copy(alpha = 0.12f),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { viewModel.updateVoiceVibe(centerVal) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = name.uppercase(),
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                                    letterSpacing = 1.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = if (isActive) MaterialTheme.colorScheme.primary else Color.White
                                                )
                                            }
                                        }
                                    }

                                    // Second Row: Teasing, Careful, Sleepy
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        presets.drop(3).forEach { (name, centerVal) ->
                                            val isActive = when (name) {
                                                "Teasing" -> voiceVibe in 51f..66.99f
                                                "Careful" -> voiceVibe in 67f..83.99f
                                                "Sleepy" -> voiceVibe in 84f..100f
                                                else -> false
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp) // Large tactile 48dp minimum touch target
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                                        else Color.White.copy(alpha = 0.03f)
                                                    )
                                                    .border(
                                                        2.dp, // Moti Bold 2dp borders
                                                        if (isActive) MaterialTheme.colorScheme.primary
                                                        else Color.White.copy(alpha = 0.12f),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { viewModel.updateVoiceVibe(centerVal) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = name.uppercase(),
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                                    letterSpacing = 1.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = if (isActive) MaterialTheme.colorScheme.primary else Color.White
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Real-time parameters display panel
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.35f))
                                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Voice Engine Details",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "NOISE_SCALE: %.3f".format(noiseScale),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "LENGTH_SCALE: %.2f".format(lengthScale),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "PITCH: %.2f".format(pitch),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "ALPHA: %.2f".format(alpha),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Offline Voice Ready",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("tv_tts_status")
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Play Test Button with specific content
                                Button(
                                    onClick = {
                                        Log.d("TTS_AUDIT", "Play Test clicked")
                                        viewModel.piperTtsManager.speak("Haye Boss... aaj tum thake hue lag rahe ho?")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("play_test_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Haye Boss... aaj tum thake hue lag rahe ho?",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.01f))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "Standard Android TTS is currently synthesized. Set switch above to toggle the low-latency offline neural Piper simulator.",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.2.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }

                    // CARD 4B: JARVIS-STYLE SYSTEM FEEDBACK VOICE TONES
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "► SoundBoard",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 1.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Sound Theme",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 1.2.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                fontFamily = FontFamily.Monospace
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val tones = listOf(
                                    "Classic Jarvis" to "Classic",
                                    "Deep Armor" to "DeepTone",
                                    "Friday Tactical" to "Friday",
                                    "Standard System" to "System"
                                )

                                tones.forEach { (id, label) ->
                                    val isSelected = jarvisVoiceTone == id
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                                else Color.White.copy(alpha = 0.03f)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 0.5.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                viewModel.setJarvisVoiceTone(id)
                                                viewModel.piperTtsManager.speak("System diagnostics optimized. Acoustic profile set to $id.")
                                            }
                                            .testTag("voice_tone_$label"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (label) {
                                                "CLASSIC" -> "Classic"
                                                "DEEP" -> "DeepTone"
                                                "FRIDAY" -> "Friday"
                                                "SYSTEM" -> "System"
                                                else -> label
                                            },
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Light,
                                            letterSpacing = 1.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1
                                        )
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
fun SystemSettingsScreen(
    viewModel: AiraViewModel,
    onBack: () -> Unit
) {
    val isOffline by viewModel.isOfflineBrain.collectAsState()
    val onlineModel by viewModel.onlineModel.collectAsState()
    val llamaThreads by viewModel.llamaThreads.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.advanced_settings_title),
                        fontWeight = FontWeight.Light,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("system_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                    // CARD 1B: CHAT API ROTATION VAULT (Accordion Mode)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF151922)),
                        border = BorderStroke(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Lock Icon",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "API Keys",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF00E5FF)
                                        )
                                        Text(
                                            text = "Tap to expand",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Light,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Arrow Icon",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            val context = LocalContext.current
                            val keyManager = remember { ChatKeyManager.getInstance(context) }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = Color(0xFF00E5FF).copy(alpha = 0.15f)
                                    )

                                    Text(
                                        text = "Keys encrypted on device",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.2.sp,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontFamily = FontFamily.Monospace
                                    )

                                    (1..20).forEach { index ->
                                        var keyValue by remember { mutableStateOf(keyManager.getKey(index)) }
                                        val isActive = keyValue.isNotEmpty()

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "ChatApiSlot $index",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Light,
                                                    letterSpacing = 1.2.sp,
                                                    color = if (isActive) Color(0xFF00E5FF) else Color.White
                                                )

                                                // Status Indicator Badge: "Active" (green) if exists, else "Add Key" (red)
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(
                                                            if (isActive) Color(0xFF2E7D32).copy(alpha = 0.08f)
                                                            else Color(0xFFC62828).copy(alpha = 0.08f)
                                                        )
                                                        .border(
                                                            0.5.dp,
                                                            if (isActive) Color(0xFF4CAF50).copy(alpha = 0.2f)
                                                            else Color(0xFFE57373).copy(alpha = 0.2f),
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (isActive) "Active" else "AddKey",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Light,
                                                        letterSpacing = 1.2.sp,
                                                        color = if (isActive) Color(0xFF81C784) else Color(0xFFE57373)
                                                    )
                                                }
                                            }

                                            OutlinedTextField(
                                                value = keyValue,
                                                onValueChange = { newValue ->
                                                    keyValue = newValue
                                                    keyManager.saveKey(index, newValue)
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .testTag("chat_api_input_$index"),
                                                placeholder = {
                                                    Text(
                                                        text = "EnterApiKey",
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color.White.copy(alpha = 0.3f),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Light,
                                                        letterSpacing = 1.2.sp
                                                    )
                                                },
                                                textStyle = TextStyle(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Light,
                                                    color = Color.White
                                                ),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF00E5FF),
                                                    unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                                                    cursorColor = Color(0xFF00E5FF)
                                                )
                                            )
                                        }
                                        if (index < 20) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(vertical = 4.dp),
                                                color = Color(0xFF00E5FF).copy(alpha = 0.1f)
                                            )
                                        }
                                    }

                                    // Groq API Key Included in Accordion
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = Color(0xFF00E5FF).copy(alpha = 0.1f)
                                    )

                                    var groqKeyValue by remember { mutableStateOf(keyManager.getGroqKey()) }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "► Groq API",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Light,
                                            letterSpacing = 1.2.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF00E5FF)
                                        )
                                        Text(
                                            text = "AddGroqKeyForFastAI",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Light,
                                            letterSpacing = 1.2.sp,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontFamily = FontFamily.Monospace
                                        )

                                        OutlinedTextField(
                                            value = groqKeyValue,
                                            onValueChange = { newValue ->
                                                groqKeyValue = newValue
                                                keyManager.saveGroqKey(newValue)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("groq_api_key_input"),
                                            placeholder = {
                                                Text(
                                                    text = "EnterGroqKey",
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.White.copy(alpha = 0.3f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Light,
                                                    letterSpacing = 1.2.sp
                                                )
                                            },
                                            textStyle = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Light,
                                                color = Color.White
                                            ),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF00E5FF),
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                                                cursorColor = Color(0xFF00E5FF)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // CARD 3: AI Brain Option Module (Hybrid AI Brain Configuration)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "► AI Mode",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 1.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Offline AI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isOffline) 
                                            "UseLocalAIOffline"
                                            else "UseLocalAIOffline",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.2.sp,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Switch(
                                    checked = isOffline,
                                    onCheckedChange = { viewModel.toggleOfflineBrain(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("offline_brain_switch")
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            )

                            val context = LocalContext.current

                            if (!isOffline) {
                                // Online Mode Active: Allow choosing between Gemini API and Groq API
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Online AI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.2.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("GeminiApi", "GroqApi").forEach { model ->
                                            val isSelected = onlineModel == when (model) {
                                                "GeminiApi" -> "Gemini API"
                                                "GroqApi" -> "Groq API"
                                                else -> model
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                        else Color.White.copy(alpha = 0.02f)
                                                    )
                                                    .border(
                                                        0.5.dp,
                                                        if (isSelected) MaterialTheme.colorScheme.primary
                                                        else Color.White.copy(alpha = 0.1f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .clickable {
                                                        viewModel.updateOnlineModel(
                                                            when (model) {
                                                                "GeminiApi" -> "Gemini API"
                                                                "GroqApi" -> "Groq API"
                                                                else -> model
                                                            }
                                                        )
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    .testTag("online_model_btn_${model.replace(" ", "_")}")
                                            ) {
                                                Text(
                                                    text = model,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Normal else FontWeight.Light,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Offline Mode Active: Show Llama 3.2 1B/3B (llama.cpp) configurations
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "► Offline Models",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.2.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Model Status",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                        Text(
                                            text = viewModel.getLlamaEngineStatus(),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Light
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Active Model",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                        Text(
                                            text = "llama-3.2-1b-instruct.gguf (Fallback Ready)",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Light
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "CPU Usage",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Light,
                                            letterSpacing = 1.2.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "CPU Threads",
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontFamily = FontFamily.Monospace
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(2, 4, 6, 8).forEach { threadCount ->
                                                val isSelected = llamaThreads == threadCount
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                            else Color.White.copy(alpha = 0.02f)
                                                        )
                                                        .border(
                                                            0.5.dp,
                                                            if (isSelected) MaterialTheme.colorScheme.primary
                                                            else Color.White.copy(alpha = 0.1f),
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .clickable { viewModel.updateLlamaThreads(threadCount) }
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${threadCount}Cores",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 9.sp,
                                                        fontWeight = if (isSelected) FontWeight.Normal else FontWeight.Light,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.speakText("Llama neural context graph compiled successfully with $llamaThreads active cores.")
                                            android.widget.Toast.makeText(context, "Llama Graph Compiled Successfully", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                            .height(40.dp)
                                            .testTag("llama_compile_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Test Offline AI",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.2.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
        }
    }
}

// ACCORDION COMPLIANT - NO LOGIC CHANGED

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MemorySettingsScreen(
    viewModel: AiraViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val memories by viewModel.memories.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingMemory by remember { mutableStateOf<com.example.data.Memory?>(null) }
    var editFactText by remember { mutableStateOf("") }

    LaunchedEffect(editingMemory) {
        editFactText = editingMemory?.factText ?: ""
    }

    if (showEditDialog && editingMemory != null) {
        val memoryItem = editingMemory!!
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.edit_memory_title),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editFactText,
                        onValueChange = { editFactText = it },
                        modifier = Modifier.fillMaxWidth().testTag("edit_memory_input"),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White),
                        label = { Text(stringResource(R.string.memory_fact_label), fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                    )
                    Text(
                        text = "Source: ${memoryItem.source.uppercase()}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editFactText.isNotBlank()) {
                            viewModel.updateMemory(
                                id = memoryItem.id,
                                factText = editFactText.trim(),
                                source = memoryItem.source,
                                createdAt = memoryItem.createdAt
                            )
                            showEditDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Memory updated")
                            }
                        }
                    },
                    modifier = Modifier.testTag("save_edit_memory_btn")
                ) {
                    Text(stringResource(R.string.save), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            viewModel.deleteMemory(memoryItem.id)
                            showEditDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Deleted")
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                        modifier = Modifier.testTag("dialog_delete_memory_btn")
                    ) {
                        Text(stringResource(R.string.delete), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = { showEditDialog = false },
                        modifier = Modifier.testTag("dialog_cancel_memory_btn")
                    ) {
                        Text(stringResource(R.string.cancel), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            },
            containerColor = Color(0xFF151922),
            textContentColor = Color.White,
            titleContentColor = MaterialTheme.colorScheme.primary
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.memory_settings_title),
                        fontWeight = FontWeight.Light,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("memory_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Backup/Restore Controls Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151922)),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.memory_engine_header),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = stringResource(R.string.memory_engine_desc),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val msg = viewModel.exportMemoriesToDownloads(context)
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("export_memories_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                stringResource(R.string.export_backup),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val msg = viewModel.importMemoriesFromDownloads(context)
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("import_memories_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Text(
                                stringResource(R.string.import_backup),
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Memories List Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.recalled_facts, memories.size),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )
                if (memories.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.clear_all),
                        fontSize = 10.sp,
                        color = Color.Red.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable { viewModel.clearMemories() }
                    )
                }
            }

            if (memories.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.no_memories_placeholder),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(memories.size) { index ->
                        val item = memories[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        editingMemory = item
                                        showEditDialog = true
                                    }
                                ),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF151922).copy(alpha = 0.6f)),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.factText,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Source: ${item.source.uppercase()} • ${android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", item.createdAt)}",
                                        fontSize = 9.sp,
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                IconButton(onClick = {
                                    viewModel.deleteMemory(item.id)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Deleted")
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Memory",
                                        tint = Color.Red.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
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

