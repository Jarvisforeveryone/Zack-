package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.ChatMessage
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: AiraViewModel,
    modifier: Modifier = Modifier
) {
    val vmIsListening by viewModel.isListening.collectAsState()
    var isListening by remember { mutableStateOf(false) }
    LaunchedEffect(vmIsListening) {
        isListening = vmIsListening
    }

    val isOfflineBrain by viewModel.isOfflineBrain.collectAsState()
    var onlineMode by remember { mutableStateOf(!isOfflineBrain) }
    LaunchedEffect(isOfflineBrain) {
        onlineMode = !isOfflineBrain
    }

    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val statusText by viewModel.currentStatus.collectAsState()
    val audioAmp by viewModel.audioAmplitude.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    
    val themeIndex by viewModel.themeIndex.collectAsState()
    val activeTheme = com.example.data.ThemeRepository.themes.getOrNull(themeIndex) ?: com.example.data.ThemeRepository.themes[0]
    val startColor = activeTheme.color
    val endColor = activeTheme.color

    val piperActiveVoice by viewModel.piperActiveVoice.collectAsState()
    val piperAvailableVoices = viewModel.piperAvailableVoices

    val listState = rememberLazyListState()

    // Dynamic greeting text based on current hour
    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greetingText = when (currentHour) {
        in 5..11 -> stringResource(R.string.good_morning)
        in 12..16 -> stringResource(R.string.good_afternoon)
        else -> stringResource(R.string.good_evening)
    }

    // Dynamic selected voice simple name
    val selectedVoiceName = when (piperActiveVoice) {
        "en_US-amy-low" -> "Amy"
        "en_US-ryan-medium" -> "Ryan"
        "en_US-lessac-high" -> "Lessac"
        "en_US-joanna-neural" -> "Joanna"
        else -> "Amy"
    }
    val voiceLabel = "Voice"

    var showVoiceDropdown by remember { mutableStateOf(false) }

    // Auto scroll chat thread on new messages
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    // UI CHANGE: Determine dynamic state-based status message
    val derivedStatus = when {
        isListening -> stringResource(R.string.listening_status)
        isSpeaking -> stringResource(R.string.speaking_status)
        statusText.contains("Processing", ignoreCase = true) || 
        statusText.contains("Analyzing", ignoreCase = true) || 
        statusText.contains("Transitioning", ignoreCase = true) ||
        statusText.contains("Thinking", ignoreCase = true) -> stringResource(R.string.thinking_status)
        else -> stringResource(R.string.ready_status)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "OrbRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationAngle"
    )

    val sweepGradient = Brush.sweepGradient(
        colors = listOf(
            Color(0xFF00D9FF),
            Color(0xFF7A00FF),
            Color(0xFF00D9FF)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // 1. ADD TOP BAR: Row at the top of HomeScreen content. Left = Text "AIRA" with linear gradient #00D9FF to #7A00FF, headlineSmall, Bold. Right = OutlinedButton "Voice" with dropdown icon. Padding 16dp.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.aira_logo_text),
                style = MaterialTheme.typography.headlineSmall.copy(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF00D9FF), Color(0xFF7A00FF))
                    )
                ),
                fontWeight = FontWeight.Bold
            )

            Box {
                OutlinedButton(
                    onClick = { showVoiceDropdown = true },
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF00E5FF)
                    ),
                    modifier = Modifier.testTag("quick_voice_switcher")
                ) {
                    Text(
                        text = stringResource(R.string.voice_label),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.voice_label),
                        tint = Color(0xFF00E5FF)
                    )
                }

                DropdownMenu(
                    expanded = showVoiceDropdown,
                    onDismissRequest = { showVoiceDropdown = false },
                    modifier = Modifier.background(Color(0xFF1E1E1E))
                ) {
                    piperAvailableVoices.forEach { voice ->
                        val isCurrent = piperActiveVoice == voice.id
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = voice.displayName.uppercase(),
                                    color = if (isCurrent) Color(0xFF00E5FF) else Color.White,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            onClick = {
                                viewModel.updatePiperVoice(voice.id)
                                showVoiceDropdown = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. ADD GREETING: Below TopBar, add Column centerAligned. Text "Good Morning" with same gradient. Text "Ready to help" bodyMedium, White 0.8f alpha.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = greetingText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF00D9FF), Color(0xFF7A00FF))
                    )
                ),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ready to help",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 3. REPLACE HUD ORB: Replace existing orb with 180dp size. 2 rings. Outer 6dp sweep gradient #00D9FF->#7A00FF. Inner 1.5dp #00FFFF 60% alpha. 4s rotation. Keep onClick to toggle isListening.
        Box(
            modifier = Modifier
                .size(180.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (isListening) {
                        viewModel.stopListening()
                    } else {
                        viewModel.startListening()
                    }
                    isListening = !isListening
                },
            contentAlignment = Alignment.Center
        ) {
            // Layer 1 Outer Ring: 6dp thickness sweep gradient #00D9FF->#7A00FF
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = rotationAngle }
            ) {
                drawCircle(
                    brush = sweepGradient,
                    radius = (size.minDimension - 6.dp.toPx()) / 2f,
                    style = Stroke(width = 6.dp.toPx())
                )
            }

            // Layer 2 Inner Ring: 1.5dp solid #00FFFF at 60% alpha
            Canvas(
                modifier = Modifier.size(150.dp)
            ) {
                drawCircle(
                    color = Color(0xFF00FFFF).copy(alpha = 0.6f),
                    radius = (size.minDimension - 1.5.dp.toPx()) / 2f,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // 4. ADD ORB LABEL: 8dp below orb. Text "Tap the orb to start talking", bodySmall, White 0.7f.
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the orb to start talking",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        // 5. REPLACE STATUS PILL: Replace old pill with new Row. Bg Color(0x0FFFFFFF), rounded 20dp, padding 10dpH 6dpV. Inside: 8dp Blue #00D9FF dot + "Online" text. 1dp divider. 8dp Cyan dot + "Voice Detection Active" text. Make "Online" part clickable to toggle onlineMode boolean.
        Row(
            modifier = Modifier
                .background(Color(0x0FFFFFFF), RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // PART A - CONNECTION
            Row(
                modifier = Modifier
                    .clickable {
                        onlineMode = !onlineMode
                        viewModel.toggleOfflineBrain(!onlineMode)
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (onlineMode) Color(0xFF00D9FF) else Color(0xFF888888), CircleShape)
                )
                Text(
                    text = "Online",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (onlineMode) Color.White else Color.White.copy(alpha = 0.5f)
                )
            }

            // Divider: 1dp divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(Color.White.copy(alpha = 0.3f))
            )

            // PART B - VOICE
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isListening) Color(0xFF00FFFF) else Color(0xFF888888), CircleShape)
                )
                Text(
                    text = "Voice Detection Active",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 6. ADD RECENT CONVERSATIONS CARD: 16dp below pill. Card with bg Color(0x0FFFFFFF), rounded 16dp, padding 12dp. Inside: Title "Recent Conversations". Then 3 rows with avatar + name + message + time. Last row = "View All" + > icon.
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0x0FFFFFFF)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "Recent Conversations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF00E5FF),
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val sdf = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
                val mockItems = listOf(
                    RecentConvItem("A", Color(0xFF7A00FF), "Aira", "Neural engine active and initialized.", "10:42"),
                    RecentConvItem("U", Color(0xFF00D9FF), "User", "Check system status and latency.", "10:41"),
                    RecentConvItem("A", Color(0xFF7A00FF), "Aira", "All parameters are standard.", "10:40")
                )

                val displayItems = remember(chatHistory) {
                    if (chatHistory.isEmpty()) {
                        mockItems
                    } else {
                        chatHistory.takeLast(3).reversed().map { msg ->
                            val isUser = msg.sender == "user"
                            val avatar = if (isUser) "U" else "A"
                            val color = if (isUser) Color(0xFF00D9FF) else Color(0xFF7A00FF)
                            val name = if (isUser) "User" else "Aira"
                            val timeStr = sdf.format(java.util.Date(msg.timestamp))
                            RecentConvItem(avatar, color, name, msg.message, timeStr)
                        }
                    }
                }

                displayItems.forEach { item ->
                    RecentConversationRow(
                        avatarText = item.avatarText,
                        avatarBg = item.avatarBg,
                        name = item.name,
                        message = item.message,
                        time = item.time
                    )
                }

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.1f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Handle View All */ }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View All",
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View All",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
}

@Composable
fun ChatLogBubble(msg: ChatMessage) {
    val isUser = msg.sender == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Holographic sender header label
            Text(
                text = if (isUser) "You" else "Aira" + if (msg.isOffline) " [Offline]" else "",
                fontSize = 9.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isUser) Color(0xFF8E8E93) else Color(0xFF00E5FF).copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 2.dp)
            )

            // Chat body Bubble
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 12.dp
                        )
                    )
                    .background(
                        if (isUser) Color.White.copy(alpha = 0.04f)
                        else Color(0xFF00E5FF).copy(alpha = 0.04f)
                    )
                    .border(
                        width = 0.5.dp,
                        color = if (isUser) Color.White.copy(alpha = 0.10f) else Color(0xFF00E5FF).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 12.dp
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = msg.message,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

data class RecentConvItem(
    val avatarText: String,
    val avatarBg: Color,
    val name: String,
    val message: String,
    val time: String
)

@Composable
fun RecentConversationRow(
    avatarText: String,
    avatarBg: Color,
    name: String,
    message: String,
    time: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(avatarBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = avatarText,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Name & Message
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                maxLines = 1,
                fontFamily = FontFamily.Monospace
            )
        }

        // Time
        Text(
            text = time,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
