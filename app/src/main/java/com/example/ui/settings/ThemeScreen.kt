package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.ThemeRepository
import com.example.ui.AiraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(navController: NavController, viewModel: AiraViewModel) {
    val themeIndex by viewModel.themeIndex.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "THEME SELECTION",
                        fontWeight = FontWeight.Light,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.testTag("theme_back_btn")) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(ThemeRepository.themes) { index, theme ->
                val solidColor = theme.color

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable { viewModel.selectTheme(index) }
                        .testTag("theme_row_$index")
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Circle Color Preview
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = solidColor,
                                shape = CircleShape
                            )
                            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f), CircleShape)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Center Text
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "- ${theme.name}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (themeIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                        Text(
                            text = theme.description,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 10.dp, top = 2.dp)
                        )
                    }

                    // Right Radio Button
                    RadioButton(
                        selected = (themeIndex == index),
                        onClick = { viewModel.selectTheme(index) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("theme_radio_$index")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
            }
        }
    }
}
