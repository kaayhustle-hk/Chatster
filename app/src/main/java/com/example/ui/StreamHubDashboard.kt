package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.StreamingPlatformEntity
import com.example.viewmodel.StreamHubViewModel
import com.example.viewmodel.LiveStreamInfo
import kotlin.math.sin

// --- Custom Palette (Obsidian/Neon Streamer Control Vibe) ---
val CosmicBlack = Color(0xFF0F0F12)
val SlateGray = Color(0xFF1E1E26)
val ActiveNeon = Color(0xFF00FFBB)
val TwitchPurple = Color(0xFF9146FF)
val YoutubeRed = Color(0xFFFF0000)
val KickGreen = Color(0xFF53FC18)
val FacebookBlue = Color(0xFF1877F2)
val CustomBlue = Color(0xFF00D2FF)
val CardBackground = Color(0xFF16161E)
val MutedSlate = Color(0xFF8B8B9B)

@Composable
fun StreamHubDashboard(
    viewModel: StreamHubViewModel,
    modifier: Modifier = Modifier
) {
    val platforms by viewModel.platforms.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val editingPlatform by viewModel.editingPlatform.collectAsStateWithLifecycle()
    val isLive by viewModel.isLiveBroadcasting.collectAsStateWithLifecycle()
    val liveDuration by viewModel.liveBroadcastingDuration.collectAsStateWithLifecycle()

    val activePlatformsCount = platforms.count { it.isActive }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBlack)
    ) {
        val isWide = maxWidth >= 850.dp

        Scaffold(
            topBar = {
                DashboardHeader(
                    isLive = isLive,
                    liveDuration = liveDuration,
                    activeDestinationsCount = activePlatformsCount,
                    onToggleLive = { viewModel.toggleLiveBroadcast(activePlatformsCount) }
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            if (isWide) {
                // Wide Pane Desktop Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Left Column Workspace Views
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1.3f)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        TabSelector(activeTab = activeTab, onTabSelected = { viewModel.setActiveTab(it) })
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            WorkspaceContent(
                                activeTab = activeTab,
                                platforms = platforms,
                                viewModel = viewModel,
                                isWideLayout = true
                            )
                        }
                    }

                    // Divider boundary
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(SlateGray)
                    )

                    // Right column live stream telemetry or active chats
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.8f)
                            .padding(16.dp)
                    ) {
                        if (activeTab == "Stream Hub" || activeTab == "Live Chat") {
                            LiveDashboardChatPanel(viewModel = viewModel)
                        } else {
                            LiveMonitorCardContent(viewModel = viewModel, platforms = platforms)
                        }
                    }
                }
            } else {
                // Lean Portable Mobile Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    TabSelector(activeTab = activeTab, onTabSelected = { viewModel.setActiveTab(it) })
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        WorkspaceContent(
                            activeTab = activeTab,
                            platforms = platforms,
                            viewModel = viewModel,
                            isWideLayout = false
                        )
                    }
                }
            }
        }

        // Configuration Ingest Editor
        if (editingPlatform != null) {
            AddEditPlatformDialog(
                platform = editingPlatform!!,
                onDismiss = { viewModel.setEditingPlatform(null) },
                onSave = { updatedPlatform ->
                    viewModel.savePlatform(updatedPlatform)
                    viewModel.setEditingPlatform(null)
                }
            )
        }
    }
}

@Composable
fun DashboardHeader(
    isLive: Boolean,
    liveDuration: Int,
    activeDestinationsCount: Int,
    onToggleLive: () -> Unit
) {
    val durationFormatted = remember(liveDuration) {
        val hrs = liveDuration / 3600
        val mins = (liveDuration % 3600) / 60
        val secs = liveDuration % 60
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    }

    Surface(
        color = SlateGray,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_header")
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pulsing broadcast beacon
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (isLive) YoutubeRed else MutedSlate)
                    )
                    if (isLive) {
                        val pulseScale by rememberInfiniteTransition().animateFloat(
                            initialValue = 1f,
                            targetValue = 2.4f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200),
                                repeatMode = RepeatMode.Restart
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp * pulseScale)
                                .clip(CircleShape)
                                .border(1.dp, YoutubeRed.copy(alpha = 0.6f / pulseScale), CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "MULTISTREAM AGGREGATOR",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = if (isLive) "ACTIVE CO-CAST: $activeDestinationsCount RELAYS" else "MULTIPLEX PIPELINE IDLE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (isLive) ActiveNeon else MutedSlate
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLive) {
                    Surface(
                        color = CosmicBlack,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = durationFormatted,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Button(
                    onClick = onToggleLive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLive) YoutubeRed else ActiveNeon,
                        contentColor = if (isLive) Color.White else CosmicBlack
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("broadcast_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isLive) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = "Toggle Live broadcast",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isLive) "STOP" else "GO LIVE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun TabSelector(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf(
        Pair("Stream Hub", Icons.Default.PlayArrow),
        Pair("Live Chat", Icons.Default.Send),
        Pair("Destinations", Icons.Default.List),
        Pair("Metadata Sync", Icons.Default.Refresh),
        Pair("Link Accounts", Icons.Default.AccountBox)
    )

    ScrollableTabRow(
        selectedTabIndex = tabs.indexOfFirst { it.first == activeTab },
        containerColor = CosmicBlack,
        contentColor = ActiveNeon,
        edgePadding = 0.dp,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[tabs.indexOfFirst { it.first == activeTab }.coerceAtLeast(0)]),
                color = ActiveNeon,
                height = 2.dp
            )
        },
        divider = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SlateGray)
            )
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        tabs.forEach { (tabName, icon) ->
            Tab(
                selected = activeTab == tabName,
                onClick = { onTabSelected(tabName) },
                text = {
                    Text(
                        text = tabName,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = "$tabName Icon",
                        modifier = Modifier.size(16.dp)
                    )
                },
                selectedContentColor = ActiveNeon,
                unselectedContentColor = MutedSlate,
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .testTag("tab_${tabName.replace(" ", "_").lowercase()}")
            )
        }
    }
}

@Composable
fun WorkspaceContent(
    activeTab: String,
    platforms: List<StreamingPlatformEntity>,
    viewModel: StreamHubViewModel,
    isWideLayout: Boolean
) {
    when (activeTab) {
        "Stream Hub" -> LiveStreamDashboardTabContent(viewModel = viewModel, isWideLayout = isWideLayout)
        "Live Chat" -> {
            if (isWideLayout) {
                // On wide layouts, the console monitor or controls fits nicely
                LiveMonitorCardContent(viewModel = viewModel, platforms = platforms)
            } else {
                // On mobile, show chat in full body area
                LiveDashboardChatPanel(viewModel = viewModel)
            }
        }
        "Destinations" -> DestinationsTabContent(platforms = platforms, viewModel = viewModel)
        "Metadata Sync" -> MetadataSyncTabContent(platforms = platforms, viewModel = viewModel)
        "Link Accounts" -> LinkedProfilesTabContent(viewModel = viewModel)
        else -> Text("Component Unavailable", color = Color.White)
    }
}

// ==========================================
// --- TAB A: STREAM AGGREGATOR DASHBOARD ---
// ==========================================
@Composable
fun LiveStreamDashboardTabContent(
    viewModel: StreamHubViewModel,
    isWideLayout: Boolean
) {
    val streams by viewModel.liveStreams.collectAsStateWithLifecycle()
    val selectedStreamId by viewModel.selectedStreamId.collectAsStateWithLifecycle()
    val isMosaic by viewModel.isMosaicMode.collectAsStateWithLifecycle()

    val focusedStream = remember(streams, selectedStreamId) {
        streams.find { it.id == selectedStreamId } ?: streams.firstOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Multi-Stream Header Layout controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Consolidated Live Desk",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = if (isMosaic) "Multiplexing grid simulation active." else "Click any stream card below to swap streams and load chats.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate
                )
            }

            // Grid Mode Toggles (View Multiple Streams Simultaneously)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SlateGray)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (!isMosaic) ActiveNeon else Color.Transparent)
                        .clickable { viewModel.toggleMosaicMode() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Focus mode player",
                        tint = if (!isMosaic) CosmicBlack else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isMosaic) ActiveNeon else Color.Transparent)
                        .clickable { viewModel.toggleMosaicMode() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Mosaic Grid view",
                        tint = if (isMosaic) CosmicBlack else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Simulated Live Video Player Section ---
        if (isMosaic) {
            // Mosaic Split View Panel (4 Grid Multi-Caster)
            MosaicSplitScreenPlayer(streams = streams.take(4), viewModel = viewModel)
        } else {
            // Focus Screen Player
            focusedStream?.let { stream ->
                FocusVideoScreenPlayer(stream = stream)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Stream Index Title
        Text(
            text = "Active Broadcaster Catalogs",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = Color.White,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // Stream Target Cards Array
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            streams.forEach { streamInfo ->
                LiveStreamSelectorCard(
                    stream = streamInfo,
                    isSelected = selectedStreamId == streamInfo.id && !isMosaic,
                    onClick = {
                        viewModel.setSelectedStream(streamInfo.id)
                        // Auto-filter target chats
                        viewModel.setChatFilterPlatform(streamInfo.platform)
                        if (isMosaic) viewModel.toggleMosaicMode() // Switch back to focus player
                    }
                )
            }
        }
    }
}

@Composable
fun FocusVideoScreenPlayer(stream: LiveStreamInfo) {
    val dynamicPulse = rememberInfiniteTransition()
    val animatedWavelength by dynamicPulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val specColor = Color(stream.thumbnailHex)

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, specColor.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .testTag("focused_video_player")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Canvas for simulated moving digital vector signal
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridY = size.height / 2f
                val count = 28
                val spacing = size.width / count
                
                for (i in 0..count) {
                    val factor = kotlin.math.sin((i.toFloat() / count * 4f * Math.PI) + (animatedWavelength * 2f * Math.PI)).toFloat()
                    val barHeight = 120f * factor * (1f - (kotlin.math.abs(i - count / 2f).toFloat() / (count / 2f)))
                    
                    drawLine(
                        color = specColor.copy(alpha = 0.35f),
                        start = Offset(i * spacing, gridY - barHeight),
                        end = Offset(i * spacing, gridY + barHeight),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }

                // Crosshairs
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(0f, gridY),
                    end = Offset(size.width, gridY),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Custom UI elements overlaid
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top controls line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = YoutubeRed,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "LIVE SIMULATION",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = CosmicBlack.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "1080p @ 60 FPS",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Surface(
                        color = CosmicBlack.copy(alpha = 0.8f),
                        shape = CircleShape,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Active signal",
                                tint = ActiveNeon,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Bottom channel metadata badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CosmicBlack.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stream.title,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(specColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${stream.channelName} • ${stream.platform}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSlate
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        color = specColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, specColor),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Views icon",
                                tint = specColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${stream.viewersCount / 1000}k watching",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = specColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// Display multiple streaming slots side by side simulating real multi-player mosaicking
@Composable
fun MosaicSplitScreenPlayer(
    streams: List<LiveStreamInfo>,
    viewModel: StreamHubViewModel
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CosmicBlack),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SlateGray),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mosaic_player_grid")
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Split-Grid top row
            Row(modifier = Modifier.fillMaxWidth()) {
                val fallbackItem = LiveStreamInfo(0, "Pending Signal", "Waiting for broadcast transmission", "System", 0, "N/A", 0xFF333333)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                ) {
                    MosaicPlayerTile(stream = streams.getOrElse(0) { fallbackItem }, viewModel = viewModel)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                ) {
                    MosaicPlayerTile(stream = streams.getOrElse(1) { fallbackItem }, viewModel = viewModel)
                }
            }

            // Split-Grid bottom row
            Row(modifier = Modifier.fillMaxWidth()) {
                val fallbackItem = LiveStreamInfo(0, "Pending Signal", "Waiting for broadcast transmission", "System", 0, "N/A", 0xFF333333)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                ) {
                    MosaicPlayerTile(stream = streams.getOrElse(2) { fallbackItem }, viewModel = viewModel)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                ) {
                    MosaicPlayerTile(stream = streams.getOrElse(3) { fallbackItem }, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MosaicPlayerTile(
    stream: LiveStreamInfo,
    viewModel: StreamHubViewModel
) {
    val platformColor = Color(stream.thumbnailHex)
    val dynamicPulse = rememberInfiniteTransition()
    val randomWaveFactor by dynamicPulse.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CardBackground)
            .border(1.dp, platformColor.copy(alpha = 0.4f))
            .clickable {
                viewModel.setSelectedStream(stream.id)
                viewModel.setChatFilterPlatform(stream.platform)
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val w = size.width
            val pathBrush = Brush.linearGradient(listOf(platformColor.copy(alpha = 0.4f), Color.Transparent))
            
            // Draw visual grid and small canvas wave
            drawRect(color = platformColor.copy(alpha = 0.05f))
            drawCircle(color = platformColor.copy(alpha = 0.1f * randomWaveFactor), radius = w / 3, center = Offset(w/2, h/2))
            
            drawLine(
                color = platformColor.copy(alpha = 0.4f),
                start = Offset(10f, h - 30f),
                end = Offset(w - 10f, h - 30f + (15f * randomWaveFactor)),
                strokeWidth = 2.dp.toPx()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = platformColor,
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        text = stream.platform.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp),
                        color = CosmicBlack,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                Surface(
                    color = CosmicBlack.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        text = "${stream.viewersCount / 1000}k",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "@${stream.channelName}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stream.category,
                    style = MaterialTheme.typography.labelSmall.copy(color = MutedSlate, fontSize = 9.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun LiveStreamSelectorCard(
    stream: LiveStreamInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val platformColor = Color(stream.thumbnailHex)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SlateGray else CardBackground
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) platformColor else SlateGray
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("stream_selector_${stream.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simulated Color Thumbnail
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(platformColor.copy(alpha = 0.8f), CosmicBlack)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = platformColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Metadata
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = platformColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stream.platform,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                color = platformColor
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stream.channelName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stream.title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stream.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlate
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Action focus button
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format("%,d", stream.viewersCount)} viewers",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = platformColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    imageVector = if (isSelected) Icons.Default.Info else Icons.Default.Search,
                    contentDescription = null,
                    tint = if (isSelected) platformColor else MutedSlate,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ==========================================
// --- TAB B: INTERACTIVE LIVING CHATS PANEL ---
// ==========================================
@Composable
fun LiveDashboardChatPanel(
    viewModel: StreamHubViewModel
) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val activeChatPlatform by viewModel.selectedChatPlatform.collectAsStateWithLifecycle()

    var chatTextInput by remember { mutableStateOf("") }

    val platformChoices = listOf("All", "Twitch", "YouTube", "Kick", "Facebook Live")

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SlateGray),
        modifier = Modifier
            .fillMaxSize()
            .testTag("chat_terminal_panel")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row filter
            Text(
                text = "Multiplex Streaming Chat",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Platform Filter Row Tabs
            ScrollableTabRow(
                selectedTabIndex = platformChoices.indexOf(activeChatPlatform),
                containerColor = CosmicBlack,
                contentColor = ActiveNeon,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[platformChoices.indexOf(activeChatPlatform).coerceAtLeast(0)]),
                        color = ActiveNeon,
                        height = 1.dp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
            ) {
                platformChoices.forEach { platform ->
                    Tab(
                        selected = activeChatPlatform == platform,
                        onClick = { viewModel.setChatFilterPlatform(platform) },
                        text = {
                            Text(
                                text = platform.substringBefore(" "),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            )
                        },
                        selectedContentColor = ActiveNeon,
                        unselectedContentColor = MutedSlate
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live list stream
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(CosmicBlack, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                if (messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Awaiting live chat logs...",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MutedSlate
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        reverseLayout = true
                    ) {
                        items(messages) { message ->
                            val brandColor = when (message.platform.lowercase()) {
                                "twitch" -> TwitchPurple
                                "youtube" -> YoutubeRed
                                "kick" -> KickGreen
                                "facebook live" -> FacebookBlue
                                else -> CustomBlue
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Custom visual identifier tag for each chat platform
                                    Surface(
                                        color = brandColor,
                                        shape = RoundedCornerShape(3.dp)
                                    ) {
                                        Text(
                                            text = message.platform.substringBefore(" ").uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 8.sp,
                                                color = CosmicBlack
                                            ),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = message.senderName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (message.isFromMe) ActiveNeon else Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (message.isFromMe) "(You)" else "",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                                        color = ActiveNeon
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = message.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Message text entry box
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sendToPlatform = if (activeChatPlatform == "All") "Twitch" else activeChatPlatform

                OutlinedTextField(
                    value = chatTextInput,
                    onValueChange = { chatTextInput = it },
                    placeholder = {
                        Text(
                            text = "Send alert as $sendToPlatform...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate
                        )
                    },
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ActiveNeon,
                        unfocusedBorderColor = SlateGray
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("chat_input_text_field")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        viewModel.sendChatMessage(sendToPlatform, chatTextInput)
                        chatTextInput = ""
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(ActiveNeon, RoundedCornerShape(8.dp))
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Post message",
                        tint = CosmicBlack,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// --- TAB C: DESTINATIONS CONTENT MANAGER ---
// ==========================================
@Composable
fun DestinationsTabContent(
    platforms: List<StreamingPlatformEntity>,
    viewModel: StreamHubViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Streaming RTMP Controls",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Add custom streams keys. Enable or disable multi-sync pipelines.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate
                )
            }

            IconButton(
                onClick = {
                    viewModel.setEditingPlatform(
                        StreamingPlatformEntity(
                            name = "Custom RTMP",
                            streamUrl = "rtmp://",
                            streamKey = "",
                            username = "CustomCast",
                            isActive = true
                        )
                    )
                },
                modifier = Modifier
                    .background(SlateGray, RoundedCornerShape(8.dp))
                    .testTag("add_target_btn")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Destination", tint = ActiveNeon)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (platforms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Empty Platform Icon",
                        tint = MutedSlate,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No streaming destinations configured.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSlate
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(platforms) { platform ->
                    PlatformDestinationCard(
                        platform = platform,
                        onToggle = { isActive -> viewModel.togglePlatformActive(platform.id, isActive) },
                        onEdit = { viewModel.setEditingPlatform(platform) },
                        onDelete = { viewModel.deletePlatform(platform.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlatformDestinationCard(
    platform: StreamingPlatformEntity,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var isKeyVisible by remember { mutableStateOf(false) }

    val platformColor = when (platform.name.lowercase()) {
        "twitch" -> TwitchPurple
        "youtube" -> YoutubeRed
        "kick" -> KickGreen
        "facebook live" -> FacebookBlue
        else -> CustomBlue
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (platform.isActive) platformColor.copy(alpha = 0.4f) else SlateGray),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("platform_card_${platform.name.replace(" ", "_").lowercase()}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row Name and Active switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(platformColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = platform.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "@${platform.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = platform.isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ActiveNeon,
                        checkedTrackColor = ActiveNeon.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.scale(0.75f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ingest UI
            Text(text = "Ingest Server URL:", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = platform.streamUrl,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(platform.streamUrl))
                        Toast.makeText(context, "Server URL copied!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Copy Server URL",
                        tint = ActiveNeon,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Stream secrets
            Text(text = "Stream Secret Key:", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isKeyVisible) {
                        if (platform.streamKey.isEmpty()) "[No key configured]" else platform.streamKey
                    } else "••••••••••••••••••••••••",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = if (isKeyVisible && platform.streamKey.isEmpty()) YoutubeRed else Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(
                        onClick = { isKeyVisible = !isKeyVisible },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = if (isKeyVisible) Icons.Default.Info else Icons.Default.Search,
                            contentDescription = "Toggle Key View",
                            tint = MutedSlate,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(platform.streamKey))
                            Toast.makeText(context, "Stream key copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Copy Secret Key",
                            tint = ActiveNeon,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onEdit,
                    colors = ButtonDefaults.textButtonColors(contentColor = ActiveNeon)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit platform", modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modify", style = MaterialTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.width(6.dp))

                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = YoutubeRed)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete platform", modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ==========================================
// --- TAB D: CASCADING METADATA SYNCHRONIZER ---
// ==========================================
@Composable
fun MetadataSyncTabContent(
    platforms: List<StreamingPlatformEntity>,
    viewModel: StreamHubViewModel
) {
    val title by viewModel.syncTitle.collectAsStateWithLifecycle()
    val desc by viewModel.syncDescription.collectAsStateWithLifecycle()
    val category by viewModel.syncCategory.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()

    val activeTargets = platforms.filter { it.isActive }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Cascading Metadata Sync",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = "Broadcast changes across all active ingest relays simultaneously on SQLite push.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedSlate
        )

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { viewModel.updateSyncFields(it, desc, category) },
                    label = { Text("Stream Campaign Title", color = MutedSlate) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ActiveNeon, unfocusedBorderColor = SlateGray),
                    modifier = Modifier.fillMaxWidth().testTag("metadata_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { viewModel.updateSyncFields(title, desc, it) },
                    label = { Text("Topic Game Category", color = MutedSlate) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ActiveNeon, unfocusedBorderColor = SlateGray),
                    modifier = Modifier.fillMaxWidth().testTag("metadata_category_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = desc,
                    onValueChange = { viewModel.updateSyncFields(title, it, category) },
                    label = { Text("Global Sync Description", color = MutedSlate) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ActiveNeon, unfocusedBorderColor = SlateGray),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("metadata_desc_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { viewModel.performPlatformMetadataSync(activeTargets) },
                    enabled = !isSyncing && activeTargets.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ActiveNeon,
                        contentColor = CosmicBlack,
                        disabledContainerColor = SlateGray,
                        disabledContentColor = MutedSlate
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("execute_sync_btn")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SlateGray, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync update", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CASCADE METADATA SYNC", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black))
                    }
                }
            }
        }

        if (syncLogs.isNotEmpty() || isSyncing) {
            Spacer(modifier = Modifier.height(14.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicBlack),
                border = BorderStroke(1.dp, SlateGray),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (isSyncing) {
                        LinearProgressIndicator(
                            progress = { syncProgress },
                            color = ActiveNeon,
                            trackColor = SlateGray,
                            modifier = Modifier.fillMaxWidth().height(4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .background(CardBackground)
                            .border(1.dp, SlateGray, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(syncLogs) { log ->
                                Text(
                                    text = log,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = if (log.startsWith("✓")) KickGreen else if (log.contains("Error")) YoutubeRed else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// --- TAB E: LINK ACCOUNTS & PROFILE ---
// ==========================================
@Composable
fun LinkedProfilesTabContent(
    viewModel: StreamHubViewModel
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()

    var twitchHandle by remember { mutableStateOf("") }
    var youtubeHandle by remember { mutableStateOf("") }
    var kickHandle by remember { mutableStateOf("") }
    var facebookHandle by remember { mutableStateOf("") }

    // Password Update Fields
    var passwordUpdateRaw by remember { mutableStateOf("") }
    var displaySuccessAlert by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Linked Social channels",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = "Link your accounts to display real metadata profiles and synchronize active broad casts securely.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedSlate
        )

        // Session Account ID Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGray),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ActiveNeon,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Identity Icon",
                            tint = CosmicBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Creator Handle: @${user?.username ?: "Admin"}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Access node email: ${user?.email ?: "support@station.com"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )
                }

                IconButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.testTag("logout_button")
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Log Out Securely", tint = YoutubeRed)
                }
            }
        }

        // Channels Connector Controls
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SlateGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "ASSOCIATE INTEGRATIONS",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                    color = ActiveNeon
                )

                // Twitch Link row
                ConnectorIntegrationRow(
                    platform = "Twitch",
                    platformColor = TwitchPurple,
                    isLinked = user?.isTwitchLinked ?: false,
                    linkedUsername = user?.twitchUsername ?: "",
                    draftValue = twitchHandle,
                    onDraftChange = { twitchHandle = it },
                    onLink = { viewModel.linkPlatformProfile("Twitch", twitchHandle); twitchHandle = "" },
                    onUnlink = { viewModel.unlinkPlatformProfile("Twitch") }
                )

                HorizontalDivider(color = SlateGray, thickness = 0.5.dp)

                // YouTube Link row
                ConnectorIntegrationRow(
                    platform = "YouTube",
                    platformColor = YoutubeRed,
                    isLinked = user?.isYoutubeLinked ?: false,
                    linkedUsername = user?.youtubeUsername ?: "",
                    draftValue = youtubeHandle,
                    onDraftChange = { youtubeHandle = it },
                    onLink = { viewModel.linkPlatformProfile("YouTube", youtubeHandle); youtubeHandle = "" },
                    onUnlink = { viewModel.unlinkPlatformProfile("YouTube") }
                )

                HorizontalDivider(color = SlateGray, thickness = 0.5.dp)

                // Kick Link row
                ConnectorIntegrationRow(
                    platform = "Kick",
                    platformColor = KickGreen,
                    isLinked = user?.isKickLinked ?: false,
                    linkedUsername = user?.kickUsername ?: "",
                    draftValue = kickHandle,
                    onDraftChange = { kickHandle = it },
                    onLink = { viewModel.linkPlatformProfile("Kick", kickHandle); kickHandle = "" },
                    onUnlink = { viewModel.unlinkPlatformProfile("Kick") }
                )

                HorizontalDivider(color = SlateGray, thickness = 0.5.dp)

                // Facebook Link row
                ConnectorIntegrationRow(
                    platform = "Facebook",
                    platformColor = FacebookBlue,
                    isLinked = user?.isFacebookLinked ?: false,
                    linkedUsername = user?.facebookUsername ?: "",
                    draftValue = facebookHandle,
                    onDraftChange = { facebookHandle = it },
                    onLink = { viewModel.linkPlatformProfile("Facebook", facebookHandle); facebookHandle = "" },
                    onUnlink = { viewModel.unlinkPlatformProfile("Facebook") }
                )
            }
        }

        // Credentials Reset Section
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SlateGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "IDENTITY SECURITY",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                    color = ActiveNeon,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                if (displaySuccessAlert) {
                    Surface(
                        color = KickGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, KickGreen.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "✓ Key passphrase has been updated in database.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = passwordUpdateRaw,
                        onValueChange = { passwordUpdateRaw = it },
                        label = { Text("Update Passphrase", color = MutedSlate) },
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ActiveNeon, unfocusedBorderColor = SlateGray),
                        modifier = Modifier.weight(1f).testTag("password_update_field")
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            user?.let { u ->
                                viewModel.resetPasswordUser(u.email, passwordUpdateRaw) {
                                    passwordUpdateRaw = ""
                                    displaySuccessAlert = true
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateGray, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("password_update_btn")
                    ) {
                        Text("SAVE")
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectorIntegrationRow(
    platform: String,
    platformColor: Color,
    isLinked: Boolean,
    linkedUsername: String,
    draftValue: String,
    onDraftChange: (String) -> Unit,
    onLink: () -> Unit,
    onUnlink: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Platform Badge Color Indicator
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(platformColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = platform,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.width(80.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        if (isLinked) {
            // Display linked status with unlink button
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Account: @$linkedUsername",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = ActiveNeon,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Unlink",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = YoutubeRed,
                    modifier = Modifier
                        .clickable { onUnlink() }
                        .testTag("unlink_${platform.lowercase()}")
                )
            }
        } else {
            // Display unlinked text input connection
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draftValue,
                    onValueChange = onDraftChange,
                    placeholder = { Text("Channel user", color = MutedSlate, fontSize = 11.sp) },
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = platformColor,
                        unfocusedBorderColor = SlateGray
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("link_input_${platform.lowercase()}")
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Connect",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = platformColor,
                    modifier = Modifier
                        .clickable { onLink() }
                        .testTag("link_btn_${platform.lowercase()}")
                )
            }
        }
    }
}

// ==========================================
// --- MONITORS & TELEMETRY LIVE CARD ---
// ==========================================
@Composable
fun LiveMonitorCardContent(viewModel: StreamHubViewModel, platforms: List<StreamingPlatformEntity>) {
    val isLive by viewModel.isLiveBroadcasting.collectAsStateWithLifecycle()
    val liveDuration by viewModel.liveBroadcastingDuration.collectAsStateWithLifecycle()
    val liveBitrate by viewModel.liveBitrateKbps.collectAsStateWithLifecycle()
    val liveDb by viewModel.liveAudioLevelDb.collectAsStateWithLifecycle()
    val liveLogs by viewModel.liveLogs.collectAsStateWithLifecycle()
    val resPreset by viewModel.selectedResolutionPreset.collectAsStateWithLifecycle()
    val fpsPreset by viewModel.selectedFpsPreset.collectAsStateWithLifecycle()

    val durationFormatted = remember(liveDuration) {
        val hrs = liveDuration / 3600
        val mins = (liveDuration % 3600) / 60
        val secs = liveDuration % 60
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    }

    val activePlatformsCount = platforms.count { it.isActive }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Relay Pipeline Terminal",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = "Active multi-stream ingest configurations, diagnostics feed and system dB levels.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedSlate
        )

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TERMINAL MONITORING STATUS",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                        color = MutedSlate
                    )

                    Surface(
                        color = if (isLive) YoutubeRed.copy(alpha = 0.2f) else SlateGray,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isLive) "LIVE CO-CASTING" else "RELAY OFFLINE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace),
                            color = if (isLive) YoutubeRed else MutedSlate,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Online Duration", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                        Text(
                            text = if (isLive) durationFormatted else "00:00:00",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace),
                            color = if (isLive) ActiveNeon else Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active Pipes", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                        Text(
                            text = "$activePlatformsCount targets",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Combined Uplink Speed", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                        Text(
                            text = if (isLive) "$liveBitrate Kbps" else "0 Kbps",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace),
                            color = if (isLive) KickGreen else Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("OBS Configuration", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                        Text(
                            text = "$resPreset @ ${fpsPreset}FPS",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "ACTIVE AUDIO VOLUME PEAK",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MutedSlate
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(SlateGray)
                    ) {
                        val progressFraction = ((liveDb + 60f) / 60f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(KickGreen, Color.Yellow, YoutubeRed)
                                    )
                                )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format("%.1f dB", liveDb),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                        color = if (liveDb > -10f) YoutubeRed else if (liveDb > -25f) Color.Yellow else KickGreen,
                        modifier = Modifier.width(50.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Log streams
        Text(
            text = "Broadcast Logs terminals",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicBlack),
            border = BorderStroke(1.dp, SlateGray),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CardBackground)
                    .padding(10.dp)
            ) {
                if (liveLogs.isEmpty() && !isLive) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Awaiting Multi-Stream Activation...",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MutedSlate
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true
                    ) {
                        items(liveLogs.reversed()) { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = if (log.contains("Twitch")) TwitchPurple 
                                        else if (log.contains("Kick")) KickGreen 
                                        else if (log.contains("YouTube")) YoutubeRed 
                                        else if (log.contains("FaceBook")) FacebookBlue 
                                        else ActiveNeon
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// --- DIALOGS PLATFORMS TARGETING EDIT ---
// ==========================================
@Composable
fun AddEditPlatformDialog(
    platform: StreamingPlatformEntity,
    onDismiss: () -> Unit,
    onSave: (StreamingPlatformEntity) -> Unit
) {
    var platformName by remember { mutableStateOf(platform.name) }
    var rtmpUrl by remember { mutableStateOf(platform.streamUrl) }
    var streamKey by remember { mutableStateOf(platform.streamKey) }
    var username by remember { mutableStateOf(platform.username) }
    var latency by remember { mutableStateOf(platform.latencyMode) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SlateGray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (platform.id == 0) "Configure Stream Target" else "Modify Stream Ingest settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                    color = Color.White
                )

                if (platform.id == 0) {
                    Text(text = "SELECT TARGET BRAND", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Twitch", "YouTube", "Kick", "Facebook Live", "Custom RTMP").forEach { name ->
                            val isSelected = platformName == name
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) ActiveNeon else CosmicBlack, RoundedCornerShape(6.dp))
                                    .clickable {
                                        platformName = name
                                        rtmpUrl = when (name.lowercase()) {
                                            "twitch" -> "rtmp://live.twitch.tv/app/"
                                            "youtube" -> "rtmp://a.rtmp.youtube.com/live2"
                                            "kick" -> "rtmp://live.kick.com/app/"
                                            "facebook live" -> "rtmps://live-api-s.facebook.com:443/rtmp/"
                                            else -> "rtmp://"
                                        }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name.substringBefore(" "),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp),
                                    color = if (isSelected) CosmicBlack else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = platformName,
                        onValueChange = { platformName = it },
                        label = { Text("Brand Provider Name") },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ActiveNeon, unfocusedBorderColor = CosmicBlack),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Stream Owner Username", color = MutedSlate) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ActiveNeon, unfocusedBorderColor = CosmicBlack),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rtmpUrl,
                    onValueChange = { rtmpUrl = it },
                    label = { Text("RTMP Server URL Ingestion", color = MutedSlate) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ActiveNeon, unfocusedBorderColor = CosmicBlack),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = streamKey,
                    onValueChange = { streamKey = it },
                    label = { Text("Stream Secret Key", color = MutedSlate) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ActiveNeon, unfocusedBorderColor = CosmicBlack),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "LATENCY BUDGET PRESET", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Normal", "Low", "Ultra-Low").forEach { lat ->
                        val isSelected = latency == lat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSelected) ActiveNeon else CosmicBlack, RoundedCornerShape(6.dp))
                                .clickable { latency = lat }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lat,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) CosmicBlack else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                        Text("CANCEL")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            onSave(
                                platform.copy(
                                    name = platformName,
                                    streamUrl = rtmpUrl,
                                    streamKey = streamKey,
                                    username = username,
                                    latencyMode = latency
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ActiveNeon, contentColor = CosmicBlack)
                    ) {
                        Text("APPLY PROFILE", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}
