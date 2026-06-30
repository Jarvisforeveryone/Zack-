package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.theme.AiraTheme

class MainActivity : ComponentActivity() {

    // Main launcher to handle startup permissions for voice, camera, and calling
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Handle runtime results gracefully inside assistant logic
        results.forEach { (permission, isGranted) ->
            android.util.Log.d("AiraMainActivity", "Permission: $permission, Granted: $isGranted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!android.speech.SpeechRecognizer.isRecognitionAvailable(this)) {
            android.app.AlertDialog.Builder(this)
                .setTitle("OnlineAccuracy")
                .setMessage("Download STT for better accuracy")
                .setPositiveButton("Allow") { dialog, _ ->
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=com.google.android.googlequicksearchbox"))
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox"))
                            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        } catch (_: Exception) {}
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("DenyAccess") { dialog, _ ->
                    android.widget.Toast.makeText(this, "Offline Mode Active", android.widget.Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                }
                .show()
        }

        // Request core permissions on startup
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.CAMERA
            )
        )

        setContent {
            val viewModel: AiraViewModel = viewModel()
            
            val themeIndex by viewModel.themeIndex.collectAsState()
            val customColorHex by viewModel.customColorHex.collectAsState()

            AiraTheme(themeIndex = themeIndex, customColorHex = customColorHex) {
                var selectedTab by remember { mutableIntStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("bottom_nav_bar"),
                            tonalElevation = 0.dp
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Outlined.Home, contentDescription = "Assistant Hub", modifier = Modifier.size(24.dp)) },
                                label = {
                                    Text(
                                        text = "Home",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.5.sp,
                                        fontSize = 10.sp
                                    )
                                },
                                modifier = Modifier.testTag("nav_assistant_tab")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = "Command Deck", modifier = Modifier.size(24.dp)) },
                                label = {
                                    Text(
                                        text = "Automation",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.5.sp,
                                        fontSize = 10.sp
                                    )
                                },
                                modifier = Modifier.testTag("nav_commands_tab")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Outlined.Article, contentDescription = "Climate News Feed", modifier = Modifier.size(24.dp)) },
                                label = {
                                    Text(
                                        text = "Feeds",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.5.sp,
                                        fontSize = 10.sp
                                    )
                                },
                                modifier = Modifier.testTag("nav_feeds_tab")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Outlined.Settings, contentDescription = "Module Configurations", modifier = Modifier.size(24.dp)) },
                                label = {
                                    Text(
                                        text = "Settings",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = 1.5.sp,
                                        fontSize = 10.sp
                                    )
                                },
                                modifier = Modifier.testTag("nav_config_tab")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> HomeScreen(viewModel = viewModel)
                            1 -> SystemControlScreen(viewModel = viewModel)
                            2 -> ExtrasScreen(viewModel = viewModel)
                            3 -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

// TAB LABELS UPDATED - PLAY STORE READY
