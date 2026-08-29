package com.himanshu13ps.horton.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.himanshu13ps.horton.data.ConversationEntity
import com.himanshu13ps.horton.data.ConversationWithDetails
import kotlinx.coroutines.launch
import com.google.accompanist.permissions.isGranted
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowBack

@Composable
fun GeminiBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val color1 by infiniteTransition.animateColor(
        initialValue = Color(0xFF0F0C29),
        targetValue = Color(0xFF302B63),
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "color1"
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = Color(0xFF302B63),
        targetValue = Color(0xFF24243E),
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse),
        label = "color2"
    )

    Box(
        modifier = modifier
            .background(Brush.linearGradient(listOf(color1, color2)))
            .fillMaxSize()
    ) {
        content()
    }
}

@Composable
fun AppScreen(viewModel: MainViewModel, onStartRecording: () -> Unit, onStopRecording: () -> Unit) {
    val appState by viewModel.appState.collectAsState()

    GeminiBackground {
        when (val state = appState) {
            is AppState.Dashboard -> DashboardScreen(viewModel, onStartRecording)
            is AppState.ActiveRecording -> ActiveRecordingScreen(viewModel, state.conversationId, onStopRecording)
            is AppState.Review -> ReviewScreen(viewModel, state.conversationId)
            is AppState.Settings -> SettingsScreen(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel, onStartRecording: () -> Unit) {
    val conversations by viewModel.allConversations.collectAsState()
    val missingModels by viewModel.missingModels.collectAsState()
    val micPermissionState = com.google.accompanist.permissions.rememberPermissionState(
        android.Manifest.permission.RECORD_AUDIO
    )

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (missingModels.isNotEmpty()) {
                    viewModel.navigateToSettings()
                    return@FloatingActionButton
                }
                if (micPermissionState.status.isGranted) {
                    viewModel.startNewSession()
                    onStartRecording()
                } else {
                    micPermissionState.launchPermissionRequest()
                }
            }) {
                Icon(Icons.Filled.Add, "Start Recording")
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Conversations", style = MaterialTheme.typography.headlineMedium, color = Color.White, modifier = Modifier.weight(1f))
                    androidx.compose.material3.IconButton(onClick = { viewModel.navigateToSettings() }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Settings, "Settings", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(16.dp))
                
                if (missingModels.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        onClick = { viewModel.navigateToSettings() }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Missing ML Models", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("You must download ${missingModels.size} required ML models before recording.", color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.height(8.dp))
                            androidx.compose.material3.Button(onClick = { viewModel.navigateToSettings() }) {
                                Text("Go to Settings")
                            }
                        }
                    }
                }

                if (!micPermissionState.status.isGranted) {
                    Text("Microphone permission is required to record notes.", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                }
            }
            items(conversations) { conv ->
                ConversationCard(conv) { viewModel.viewConversation(conv.conversationId) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationCard(conv: ConversationEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(conv.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text("Status: ${conv.processingStatus}", color = Color.LightGray)
        }
    }
}

@Composable
fun ActiveRecordingScreen(viewModel: MainViewModel, conversationId: Long, onStopRecording: () -> Unit) {
    val details by viewModel.currentConversationDetails.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    viewModel.endSession(context)
                    onStopRecording()
                },
                containerColor = MaterialTheme.colorScheme.error
            ) {
                Icon(Icons.Filled.Stop, "Stop Recording", tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Recording Active", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(16.dp))
            // Pulse Visualizer
            Box(
                modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.White.copy(alpha=0.05f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                PulseVisualizer()
            }
            Spacer(Modifier.height(16.dp))
            
            LazyColumn {
                details?.transcripts?.let { transcripts ->
                    items(transcripts) { t ->
                        Text(t.rawText, color = Color.White, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewScreen(viewModel: MainViewModel, conversationId: Long) {
    val details by viewModel.currentConversationDetails.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Black.copy(alpha = 0.2f),
                contentColor = Color.White
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("Extracted Notes") }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("Raw Discussion") }
                )
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
                    // Notes View
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        if (details?.notes != null) {
                            dev.jeziellago.compose.markdowntext.MarkdownText(
                                markdown = details?.notes!!.markdownContent,
                                color = Color.White
                            )
                        } else {
                            Text("Generating notes (Simulated Inference...)", color = Color.LightGray)
                        }
                    }
                }
                1 -> {
                    // Raw Discussion View
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        details?.transcripts?.let { transcripts ->
                            items(transcripts) { t ->
                                Text(t.rawText, color = Color.White, modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val missingModels by viewModel.missingModels.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.IconButton(onClick = { viewModel.navigateToDashboard() }) {
                    Icon(androidx.compose.material.icons.Icons.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Text("Model Downloads", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            items(viewModel.requiredModels) { model ->
                val isMissing = missingModels.any { it.id == model.id }
                val progressState = downloadProgress[model.id]

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(model.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        
                        if (!isMissing) {
                            Text("Status: Downloaded ✅", color = Color.Green)
                        } else {
                            when (progressState) {
                                is com.himanshu13ps.horton.utils.FileDownloader.DownloadState.Downloading -> {
                                    Text("Status: Downloading... ${(progressState.progress * 100).toInt()}%", color = Color.Yellow)
                                    Spacer(Modifier.height(8.dp))
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = { progressState.progress },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                is com.himanshu13ps.horton.utils.FileDownloader.DownloadState.Error -> {
                                    Text("Error: ${progressState.message}", color = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.height(8.dp))
                                    androidx.compose.material3.Button(onClick = { viewModel.downloadModel(context, model) }) {
                                        Text("Retry Download")
                                    }
                                }
                                else -> {
                                    Text("Status: Missing", color = Color.LightGray)
                                    Spacer(Modifier.height(8.dp))
                                    androidx.compose.material3.Button(onClick = { viewModel.downloadModel(context, model) }) {
                                        Text("Download")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
@Composable
fun PulseVisualizer() {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFF6C63FF),
                radius = 100f * scale,
                alpha = alpha
            )
            drawCircle(
                color = Color(0xFF6C63FF),
                radius = 50f
            )
        }
    }
}

