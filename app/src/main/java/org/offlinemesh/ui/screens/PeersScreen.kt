package org.offlinemesh.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.offlinemesh.identity.DeviceIdentity
import org.offlinemesh.mesh.MeshNeighbor
import org.offlinemesh.ui.theme.LocalAirLinkColors

@Composable
fun PeersScreen(
    identity: DeviceIdentity,
    isMeshActive: Boolean,
    peers: List<MeshNeighbor>,
    onToggleMesh: () -> Unit,
    onCallPeer: (MeshNeighbor) -> Unit,
    onChatPeer: (MeshNeighbor) -> Unit,
    onSimulateTestCall: () -> Unit,
    onGroupCall: ((List<MeshNeighbor>) -> Unit)? = null
) {
    val colors = LocalAirLinkColors.current
    var isGroupMode by remember { mutableStateOf(false) }
    val selectedGroupPeerIds = remember { mutableStateListOf<String>() }

    val pulseAnim = rememberInfiniteTransition(label = "beaconPulse")
    val beaconScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconScale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(2.dp)) }

        // 1. Sleek Radar Status Hero Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                colors.surfaceElevated,
                                if (isMeshActive) colors.primary.copy(alpha = if (colors.isDark) 0.15f else 0.08f)
                                else colors.surface
                            )
                        )
                    )
                    .border(
                        1.dp,
                        if (isMeshActive) colors.primary.copy(alpha = 0.4f) else colors.cardBorder,
                        RoundedCornerShape(22.dp)
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Animated Radar Icon Container
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isMeshActive) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .scale(beaconScale)
                                        .clip(CircleShape)
                                        .background(colors.primary.copy(alpha = 0.15f))
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isMeshActive) colors.primary.copy(alpha = 0.2f)
                                        else colors.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isMeshActive) Icons.Default.Radar else Icons.Default.SensorsOff,
                                    contentDescription = null,
                                    tint = if (isMeshActive) colors.primary else colors.textMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isMeshActive) "AirLink Active" else "AirLink Standby",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isMeshActive) colors.primary else colors.textMuted)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isMeshActive)
                                    "${peers.size} peer(s) in calling range"
                                else
                                    "Tap to broadcast & discover peers",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    // Compact Power Capsule Switch
                    Surface(
                        onClick = onToggleMesh,
                        shape = RoundedCornerShape(14.dp),
                        color = if (isMeshActive) colors.red.copy(alpha = 0.15f) else colors.primary,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isMeshActive) colors.red.copy(alpha = 0.4f) else colors.primary
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isMeshActive) "Disconnect" else "Connect",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMeshActive) colors.red else Color.White
                            )
                        }
                    }
                }
            }
        }

        // 2. Section Header & Group Calling Control
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Nearby Devices",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${peers.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.cyan
                        )
                    }
                }

                if (peers.size >= 2 && onGroupCall != null) {
                    Surface(
                        onClick = {
                            if (isGroupMode) selectedGroupPeerIds.clear()
                            isGroupMode = !isGroupMode
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isGroupMode) colors.blue.copy(alpha = 0.2f) else colors.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isGroupMode) colors.blue else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = if (isGroupMode) colors.blue else colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isGroupMode) "Cancel" else "Group Call",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isGroupMode) colors.blue else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // Group Call Action Pill Bar
        if (isGroupMode) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.blue.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Select Call Participants",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "${selectedGroupPeerIds.size} of ${peers.size} selected",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }

                            Button(
                                onClick = {
                                    val chosen = peers.filter { it.deviceId in selectedGroupPeerIds }
                                    if (chosen.size >= 2) {
                                        onGroupCall?.invoke(chosen)
                                        selectedGroupPeerIds.clear()
                                        isGroupMode = false
                                    }
                                },
                                enabled = selectedGroupPeerIds.size >= 2,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.blue),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Call", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Empty State
        if (peers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(24.dp))
                        .padding(vertical = 36.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMeshActive) Icons.Default.Radar else Icons.Default.Wifi,
                                contentDescription = null,
                                tint = if (isMeshActive) colors.primary else colors.textMuted,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Text(
                            text = if (isMeshActive) "Scanning for Nearby Peers…" else "AirLink Radios are Idle",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "To test real decentralized calling and photo sharing, install AirLink on a second Android device and tap Connect. Phones connect and relay automatically.",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedButton(
                            onClick = onSimulateTestCall,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.cyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.cyan.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Launch Call UI Demo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            // Peers list
            items(peers, key = { it.deviceId }) { peer ->
                PeerRowCard(
                    peer = peer,
                    onCall = { onCallPeer(peer) },
                    onChat = { onChatPeer(peer) },
                    isGroupMode = isGroupMode,
                    isSelected = peer.deviceId in selectedGroupPeerIds,
                    onToggleSelect = {
                        if (peer.deviceId in selectedGroupPeerIds) {
                            selectedGroupPeerIds.remove(peer.deviceId)
                        } else {
                            selectedGroupPeerIds.add(peer.deviceId)
                        }
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(28.dp)) }
    }
}

@Composable
fun PeerRowCard(
    peer: MeshNeighbor,
    onCall: () -> Unit,
    onChat: () -> Unit,
    isGroupMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {}
) {
    val colors = LocalAirLinkColors.current
    val isDirect = peer.hopCount <= 1

    val avatarGradients = listOf(
        listOf(Color(0xFF059669), Color(0xFF10B981)),
        listOf(Color(0xFF2563EB), Color(0xFF38BDF8)),
        listOf(Color(0xFF7C3AED), Color(0xFFA78BFA)),
        listOf(Color(0xFFD97706), Color(0xFFFBBF24))
    )
    val colorPair = avatarGradients[Math.abs(peer.deviceId.hashCode()) % avatarGradients.size]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) colors.surfaceVariant else colors.surface)
            .border(
                1.dp,
                if (isSelected) colors.blue else colors.cardBorder,
                RoundedCornerShape(18.dp)
            )
            .clickable(enabled = isGroupMode) { onToggleSelect() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isGroupMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = colors.blue,
                        uncheckedColor = colors.textMuted
                    ),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Avatar circle with vibrant gradient
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(colorPair)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = peer.displayName.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Peer Information
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isDirect) colors.primary.copy(alpha = 0.15f)
                                else colors.cyan.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDirect) Icons.Default.Sensors else Icons.Default.Hub,
                                contentDescription = null,
                                tint = if (isDirect) colors.primary else colors.cyan,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isDirect) "1-Hop Direct" else "${peer.hopCount} Hops (${peer.viaDisplayName})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDirect) colors.primary else colors.cyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "ID: ${peer.deviceId}",
                        fontSize = 11.sp,
                        color = colors.textMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (!isGroupMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = onChat,
                        shape = CircleShape,
                        color = colors.surfaceVariant,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Chat",
                                tint = colors.cyan,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onCall,
                        shape = CircleShape,
                        color = colors.primary,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
