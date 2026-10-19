package org.offlinemesh

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.offlinemesh.calls.CallState
import org.offlinemesh.ui.MainViewModel
import org.offlinemesh.ui.permissions.PermissionHelper
import org.offlinemesh.ui.screens.ActiveCallScreen
import org.offlinemesh.ui.screens.ChatScreen
import org.offlinemesh.ui.screens.DiagnosticsScreen
import org.offlinemesh.ui.screens.IncomingCallDialog
import org.offlinemesh.ui.screens.MeshVisualizerScreen
import org.offlinemesh.ui.screens.OnboardingScreen
import org.offlinemesh.ui.screens.PeersScreen
import org.offlinemesh.ui.theme.LocalAirLinkColors
import org.offlinemesh.ui.theme.OfflineMeshTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            OfflineMeshTheme(darkTheme = isDarkMode) {
                MainAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppRoot(viewModel: MainViewModel) {
    val colors = LocalAirLinkColors.current
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(PermissionHelper.hasAllPermissions(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
        if (hasPermissions) {
            viewModel.toggleMesh()
        } else {
            Toast.makeText(context, "Microphone & Nearby permissions are required for AirLink calling", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
        }
    }

    val onboardingDone by viewModel.onboardingDone.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val callState by viewModel.callState.collectAsState()
    val isMeshActive by viewModel.isMeshActive.collectAsState()
    val knownPeers by viewModel.knownPeers.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val relayEnabled by viewModel.relayEnabled.collectAsState()
    val identity = viewModel.identityManager.currentIdentity

    // 1. First-launch Username Onboarding Screen
    if (!onboardingDone) {
        OnboardingScreen(
            onConfirm = { chosenName ->
                viewModel.completeOnboarding(chosenName)
            }
        )
        return
    }

    // 2. Full-Screen Active Call Overlay
    if (callState is CallState.Connected || callState is CallState.OutgoingRinging) {
        ActiveCallScreen(
            callState = callState,
            onEndCall = { viewModel.endCall() },
            onToggleMute = { viewModel.toggleMute() },
            onToggleSpeaker = { viewModel.toggleSpeaker() }
        )
        return
    }

    // 3. Incoming Call Dialog Alert (1:1 or Group Call)
    if (callState is CallState.IncomingRinging || callState is CallState.GroupCallRinging) {
        IncomingCallDialog(
            callState = callState,
            onAccept = { viewModel.acceptCall() },
            onReject = { viewModel.rejectCall() }
        )
    }

    val pulseAnim = rememberInfiniteTransition(label = "topBeacon")
    val beaconPulse by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon"
    )

    // 4. Main App Root with Sleek Top Bar & Bottom Navigation
    Scaffold(
        containerColor = colors.background,
        topBar = {
            Surface(
                color = colors.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Minimalist AirLink Logo & App Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minimalist AirLink Vector Mark
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(24.dp)) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                // Left Emerald Arc
                                drawArc(
                                    color = colors.primary,
                                    startAngle = 135f,
                                    sweepAngle = 220f,
                                    useCenter = false,
                                    topLeft = Offset(size.width * 0.1f, size.height * 0.15f),
                                    size = androidx.compose.ui.geometry.Size(size.width * 0.55f, size.height * 0.7f),
                                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                                )
                                // Right Cyan Arc
                                drawArc(
                                    color = colors.cyan,
                                    startAngle = 315f,
                                    sweepAngle = 220f,
                                    useCenter = false,
                                    topLeft = Offset(size.width * 0.35f, size.height * 0.15f),
                                    size = androidx.compose.ui.geometry.Size(size.width * 0.55f, size.height * 0.7f),
                                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                                )
                                // Center Core Dot
                                drawCircle(
                                    color = if (colors.isDark) Color.White else colors.textPrimary,
                                    radius = 2.5f,
                                    center = center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "AirLink",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "${identity.displayName} · ${identity.deviceId}",
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Right: Sleek Interactive Mesh Status Pill
                    Surface(
                        onClick = { viewModel.toggleMesh() },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isMeshActive) colors.primary.copy(alpha = if (colors.isDark) 0.15f else 0.12f) else colors.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isMeshActive) colors.primary.copy(alpha = 0.4f) else colors.cardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .then(
                                        if (isMeshActive) Modifier.scale(beaconPulse) else Modifier
                                    )
                                    .clip(CircleShape)
                                    .background(if (isMeshActive) colors.primary else colors.textMuted)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMeshActive) "AirLink Live" else "Standby",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isMeshActive) colors.primary else colors.textSecondary
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = colors.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.Phone, contentDescription = "Peers") },
                    label = { Text("Peers", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.primary,
                        selectedTextColor = colors.primary,
                        unselectedIconColor = colors.textMuted,
                        unselectedTextColor = colors.textMuted,
                        indicatorColor = colors.surfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat") },
                    label = { Text("Chat", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.primary,
                        selectedTextColor = colors.primary,
                        unselectedIconColor = colors.textMuted,
                        unselectedTextColor = colors.textMuted,
                        indicatorColor = colors.surfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.Hub, contentDescription = "Topology") },
                    label = { Text("Radar Map", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.primary,
                        selectedTextColor = colors.primary,
                        unselectedIconColor = colors.textMuted,
                        unselectedTextColor = colors.textMuted,
                        indicatorColor = colors.surfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.primary,
                        selectedTextColor = colors.primary,
                        unselectedIconColor = colors.textMuted,
                        unselectedTextColor = colors.textMuted,
                        indicatorColor = colors.surfaceVariant
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!hasPermissions) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Permission Notice",
                            tint = colors.cyan
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permissions Required",
                                style = MaterialTheme.typography.titleSmall.copy(color = colors.textPrimary, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Microphone & Nearby Wi-Fi/Bluetooth are required for peer discovery and calling.",
                                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                            )
                        }
                    }
                    Button(
                        onClick = { permissionLauncher.launch(PermissionHelper.getRequiredPermissions()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.cyan)
                    ) {
                        Text(text = "Grant Permissions", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> PeersScreen(
                            identity = identity,
                            isMeshActive = isMeshActive,
                            peers = knownPeers,
                            onToggleMesh = { viewModel.toggleMesh() },
                            onCallPeer = { peer -> viewModel.startCall(peer) },
                            onChatPeer = { peer -> viewModel.selectTab(1) },
                            onSimulateTestCall = { viewModel.simulateTestCall() },
                            onGroupCall = { peers -> viewModel.startGroupCall(peers) }
                        )
                        1 -> ChatScreen(
                            messages = messages,
                            peers = knownPeers,
                            onSendMessage = { recipient, text -> viewModel.sendMessage(recipient, text) },
                            onSendImage = { recipient, base64 -> viewModel.sendImage(recipient, base64) }
                        )
                        2 -> MeshVisualizerScreen(
                            identity = identity,
                            peers = knownPeers,
                            diagnostics = diagnostics
                        )
                        3 -> DiagnosticsScreen(
                            identity = identity,
                            diagnostics = diagnostics,
                            isRelayEnabled = relayEnabled,
                            isDarkMode = isDarkMode,
                            onToggleRelay = { viewModel.setRelayEnabled(it) },
                            onToggleDarkMode = { viewModel.toggleDarkMode() },
                            onUpdateDisplayName = { viewModel.updateDisplayName(it) }
                        )
                    }
                }
            }
        }
    }
}
