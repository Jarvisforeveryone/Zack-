package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExtrasScreen(
    viewModel: AiraViewModel,
    modifier: Modifier = Modifier
) {
    val weatherData by viewModel.weatherText.collectAsState()
    val newsArticles by viewModel.newsFeed.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
         // Title
        Text(
            text = "Timeline",
            fontWeight = FontWeight.Light,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // CARD 1: ENVIRONMENTAL METRIC CORES (WEATHER)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "► Weather",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Weather Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Updating weather...",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = weatherData,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("weather_display_txt")
                    )
                }
            }
        }

        // CARD 2: DIGITAL NEWS TRANSMISSION CORE
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "► News",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.Newspaper,
                        contentDescription = "News Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Latest Updates",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace
                )

                if (newsArticles.isEmpty()) {
                    Text(
                        text = "No updates available",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    newsArticles.forEachIndexed { i, article ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.02f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "News",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 1.5.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = article,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.testTag("news_article_$i")
                            )
                        }
                    }
                }
            }
        }
    }
}
